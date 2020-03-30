package com.itglobal.vpn_client_android

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Build.VERSION_CODES.Q
import com.android.billingclient.api.Purchase
import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter
import com.itglobal.vpn_client_android.VpnApp.Companion.INSTANCE
import com.itglobal.vpn_client_android.interactor.UserInteractor
import com.itglobal.vpn_client_android.models.Country
import com.itglobal.vpn_client_android.models.Status
import com.itglobal.vpn_client_android.models.VPNConfig
import com.itglobal.vpn_client_android.repositories.PreferencesRepository
import com.itglobal.vpn_client_android.repositories.UserRepository
import de.blinkt.openvpn.OpenVpnApi
import de.blinkt.openvpn.core.OpenVPNThread
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

class MainPresenter(
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository,
    private val userInteractor: UserInteractor
) : MvpBasePresenter<MainView>() {

    private var status = Status.DISCONNECTED
    private var country: Country?
        get() = preferencesRepository.country
        set(value) {
            preferencesRepository.country = value
        }
    private var disposables = CompositeDisposable()
    private var vpnConfig: VPNConfig?
        get() = preferencesRepository.config
        set(value) {
            preferencesRepository.config = value
        }
    private var timerDisposable: Disposable? = null
    private var progress = 0
    private var hasSubscription = false
    private var canStopVpn = false

    fun locationClicked() {
        if (status == Status.DISCONNECTED || canStopVpn) {
            ifViewAttached { view ->
                view.enableCountryButton(false)
                view.showLocationsDialog()
            }
        }
    }

    fun selectCountry(country: Country) {
        if (country.name != this.country?.name) {
            this.country = country
            ifViewAttached { it.showSelectedLocation(country.name ?: "", country.flag) }
            if (canStopVpn) {
                val needReconnect = status == Status.CONNECTED
                stopVpn()
                status = Status.DISCONNECTED
                showConnectionStatus()
                if (needReconnect) startVpn()
            }
        }
    }

    fun checkVpnState(context: Context) {
        val progress = when(status) {
            Status.CONNECTION -> MAX_PROGRESS.times(80).div(100)
            Status.CONNECTED -> MAX_PROGRESS
            else -> 0
        }
        ifViewAttached { view ->
            view.showConnectionStatus(status, vpnConfig?.config?.address ?: "", vpnConfig?.country?.flag)
            view.showSelectedLocation(country?.name ?: context.getString(R.string.default_location), country?.flag)
            view.showProgress(progress)
        }
    }

    fun subscribeClicked() = ifViewAttached { it.subscribe() }

    fun checkSubscription(subscriptions: List<Purchase>?, updateToken: Boolean) {
        this.hasSubscription = subscriptions != null && subscriptions.isNotEmpty()
        val subscription = subscriptions?.maxBy { it.purchaseTime }
        subscription?.let {
            disposables.add(userInteractor.checkUser(it.purchaseToken, it.sku, updateToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ if (updateToken) connectVpnClicked() }, { }))
        }
    }

    fun connectVpnClicked() {
        val connected = hasInternetConnection()
        if (!connected && status == Status.DISCONNECTED) {
            ifViewAttached { it.showMessage(INSTANCE.getString(R.string.check_connection_message)) }
        } else {
            if (hasSubscription) {
                startVpn()
            } else {
                subscribeClicked()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun hasInternetConnection(): Boolean {
        val connectivityManager = INSTANCE.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else false
        } else {
            try {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                activeNetworkInfo != null && activeNetworkInfo.isConnected
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun startVpn() {
        if (status == Status.DISCONNECTED) {
            status = Status.CONNECTION
            startProgress()
            showConnectionStatus()
            disposables.add(userRepository.connect(country?.code ?: "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { config ->
                        vpnConfig = config
                        ifViewAttached { it.prepareVpn() }
                    },
                    { handleError(it) }
                ))
        } else if (canStopVpn) {
            stopVpn()
            status = Status.DISCONNECTED
            showConnectionStatus()
        }
    }

    private fun handleError(error: Throwable) {
        error.printStackTrace()
        if (error is HttpException && error.code() == 402) {
            ifViewAttached { view ->
                view.showMessage(error.message())
                val purchases = view.getPurchases()?.filter { it.packageName == BuildConfig.APPLICATION_ID }
                if (purchases != null && purchases.isNotEmpty()) {
                    val purchase = purchases.maxBy { it.purchaseTime }
                    purchase?.let {
                        disposables.add(userRepository.subscribe(purchase.purchaseToken, purchase.sku)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .flatMap { userRepository.connect(country?.code ?: "").toObservable() }
                            .subscribe(
                                { config ->
                                    vpnConfig = config
                                    stopVpn()
                                    ifViewAttached { it.prepareVpn() }
                                },
                                {
                                    status = Status.DISCONNECTED
                                    showConnectionStatus()
                                    if (it is HttpException && it.code() == 400) {
                                        view.showSubscribeButton(true)
                                        hasSubscription = false
                                    }
                                }
                            ))
                    }
                } else {
                    status = Status.DISCONNECTED
                    showConnectionStatus()
                    view.showSubscribeButton(true)
                    hasSubscription = false
                }
            }
        } else {
            status = Status.DISCONNECTED
            showConnectionStatus()
        }
    }

    private fun showConnectionStatus() {
        ifViewAttached { view -> view.showConnectionStatus(status, vpnConfig?.config?.address ?: "", vpnConfig?.country?.flag) }
        when (status) {
            Status.CONNECTED -> {
                stopProgress()
                ifViewAttached { view -> view.showProgress(MAX_PROGRESS) }
            }
            Status.DISCONNECTED -> {
                stopProgress()
                ifViewAttached { view -> view.showProgress(0) }
            }
            else -> {
                if (timerDisposable == null) {
                    ifViewAttached { view ->
                        view.showProgress(MAX_PROGRESS.times(80).div(100))
                    }
                }
            }
        }
    }

    fun updateConnectionStatus(serviceStatus: String) {
        println("OpenVPN updateConnectionStatus serviceStatus $serviceStatus")
        canStopVpn = canVpnStopped(serviceStatus)
        status = when(serviceStatus) {
            "DISCONNECTED" -> Status.DISCONNECTED
            "CONNECTED" -> Status.CONNECTED
            "WAIT", "AUTH", "RECONNECTING", "CONNECTRETRY" -> Status.CONNECTION
            else -> status
        }
        showConnectionStatus()
    }

    private fun canVpnStopped(serviceStatus: String):Boolean {
        return when(serviceStatus) {
            "CONNECTED", "RECONNECTING", "CONNECTRETRY", "NONETWORK", "EXITING", "NOPROCESS" -> true
            else -> false
        }
    }

    private fun startProgress() {
        progress = MAX_PROGRESS.times(20).div(100)
        stopProgress()
        timerDisposable = Observable
            .interval(PERIOD, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { time ->
                if (time >= TIMEOUT) {
                    stopProgress()
                } else {
                    progress++
                    ifViewAttached { view -> view.showProgress(progress) }
                }
            }
    }

    private fun stopProgress() {
        timerDisposable?.dispose()
        timerDisposable = null
    }

    fun startVpn(context: Context) {
        vpnConfig?.config?.apply { OpenVpnApi.startVpn(context, configFileData, login, password) }
    }

    private fun stopVpn() {
        canStopVpn = false
        OpenVPNThread.stop()
    }

    override fun destroy() {
        super.destroy()
        disposables.dispose()
        stopProgress()
    }

    companion object {
        private const val MAX_PROGRESS = 37
        private const val PERIOD = 250L
        private const val TIMEOUT = 22
    }

}