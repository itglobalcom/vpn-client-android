package com.itglobal.vpn_client_android

import android.app.Activity
import android.content.*
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.*
import com.hannesdorfmann.mosby3.mvp.MvpFragment
import com.itglobal.vpn_client_android.VpnApp.Companion.INSTANCE
import com.itglobal.vpn_client_android.extensions.loadFlag
import com.itglobal.vpn_client_android.extensions.setHtmlText
import com.itglobal.vpn_client_android.interactor.UserInteractor
import com.itglobal.vpn_client_android.models.Country
import com.itglobal.vpn_client_android.models.Status
import com.itglobal.vpn_client_android.repositories.PreferencesRepository
import com.itglobal.vpn_client_android.repositories.UserRepository
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainFragment : MvpFragment<MainView, MainPresenter>(), PurchasesUpdatedListener, MainView {

    @Inject
    lateinit var userRepository: UserRepository
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    @Inject
    lateinit var userInteractor: UserInteractor

    private lateinit var billingClient: BillingClient

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(componentName: ComponentName?) = Unit

        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
            presenter.updateConnectionStatus(OpenVPNService.getStatus())
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                intent?.getStringExtra(VPN_STATE)?.let { presenter.updateConnectionStatus(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        INSTANCE.provideAppComponent().inject(this)
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        billingClient = BillingClient.newBuilder(requireContext())
            .enablePendingPurchases()
            .setListener(this)
            .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val purchases = getPurchases()
                    val hasSubscription = purchases != null && purchases.isNotEmpty()
                    enableVpnConnectButton(true)
                    presenter.checkSubscription(purchases, false)
                    showSubscribeButton(!hasSubscription)
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                    showMessage(getString(R.string.billing_unavailable_message))
                }
            }
            override fun onBillingServiceDisconnected() {
                enableVpnConnectButton(false)
                showMessage(getString(R.string.login_message))
            }
        })
        btnSubscribe.setOnClickListener { presenter.subscribeClicked() }
        btnConnect.setOnClickListener { presenter.connectVpnClicked() }
        btnCountry.setOnClickListener { presenter.locationClicked() }
        presenter.checkVpnState(requireContext())
        VpnStatus.initLogCache(activity?.cacheDir)
    }

    override fun subscribe() {
        lifecycleScope.launch {
            val result = querySkuDetails()
            onResult(result)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_VPN) {
            if (resultCode == Activity.RESULT_OK) {
                presenter.startVpn(requireContext())
            } else {
                showMessage(getString(R.string.vpn_permission_deny_message))
            }
        }
    }

    override fun createPresenter(): MainPresenter = MainPresenter(userRepository, preferencesRepository, userInteractor)

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver, IntentFilter(CONNECTION_STATE))
        val intent = Intent(requireContext(), OpenVPNService::class.java)
        intent.action = OpenVPNService.START_SERVICE
        requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver)
        requireActivity().unbindService(connection)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        billingClient.endConnection()
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult?,
        purchases: MutableList<Purchase>?
    ) {
        val subs = purchases?.filter { it.packageName == BuildConfig.APPLICATION_ID }
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED || subs?.isNotEmpty() == true) {
            showSubscribeButton(false)
            enableVpnConnectButton(true)
            presenter.checkSubscription(subs, true)
        }
    }

    override fun getPurchases(): List<Purchase>? {
        val purchaseResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
        return purchaseResult.purchasesList?.filter { it.packageName == BuildConfig.APPLICATION_ID }
    }

    override fun enableVpnConnectButton(enable: Boolean) {
        btnConnect?.isEnabled = enable
    }

    override fun enableCountryButton(enable: Boolean) {
        btnCountry?.isEnabled = enable
    }

    override fun showSubscribeButton(show: Boolean) {
        btnSubscribe?.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    override fun showLocationsDialog() {
        fragmentManager?.let {
            CountriesDialogFragment.getInstance(
                { item -> connectToSelectedCountry(item) },
                { item -> enableCountryButton(item) }
            ).show(it, TAG)
        }
    }

    private fun onResult(result: SkuDetailsResult) {
        val flowParams = BillingFlowParams.newBuilder()
        result.skuDetailsList?.forEach { flowParams.setSkuDetails(it) }
        billingClient.launchBillingFlow(activity, flowParams.build())
    }

    private suspend fun querySkuDetails(): SkuDetailsResult {
        val skuList = ArrayList<String>()
        skuList.add(SUBSCRIPTION_ID)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
        return withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }
    }

    override fun showConnectionStatus(status: Status, address: String, flagUrl: String?) {
        tvAddress?.text = address
        when(status) {
            Status.DISCONNECTED -> {
                tvStatus?.setHtmlText(getString(R.string.disconnected_label))
                tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.disconnectedColor))
                btnConnect?.setBackgroundResource(R.drawable.bg_disconnected_button)
                ivLogo?.setImageResource(R.drawable.ic_logo_disconnected)
                tvAddress?.visibility = View.INVISIBLE
                ivServerIcon?.visibility = View.INVISIBLE
            }
            Status.CONNECTION -> {
                tvStatus?.setHtmlText(getString(R.string.connection_label))
                tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.connectionColor))
                btnConnect?.setBackgroundResource(R.drawable.bg_connection_button)
                ivLogo?.setImageResource(R.drawable.ic_logo_connection)
                tvAddress?.visibility = View.INVISIBLE
                ivServerIcon?.visibility = View.INVISIBLE
            }
            else -> {
                tvStatus?.setHtmlText(getString(R.string.connected_label))
                tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.connectedColor))
                btnConnect?.setBackgroundResource(R.drawable.bg_connected_button)
                ivLogo?.setImageResource(R.drawable.ic_logo_connected)
                tvAddress?.visibility = View.VISIBLE
                ivServerIcon?.loadFlag(flagUrl)
                ivServerIcon?.visibility = View.VISIBLE
            }
        }
    }

    override fun showProgress(value: Int) {
        progress?.progress = value
    }

    override fun showSelectedLocation(location: String, flagUrl: String?) {
        btnCountry.text = location
        ivCountryIcon.loadFlag(flagUrl)
    }

    private fun connectToSelectedCountry(country: Country) = presenter.selectCountry(country)

    override fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun prepareVpn() {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            startActivityForResult(intent, RC_VPN)
        } else {
            presenter.startVpn(requireContext())
        }
    }

    companion object {
        private const val TAG = "CountriesDialogFragment"
        private const val SUBSCRIPTION_ID = "YOUR_SUBSCRIPTION_ID"
        private const val VPN_STATE = "state"
        private const val RC_VPN = 1000
        private const val CONNECTION_STATE = "connectionState"
    }
}