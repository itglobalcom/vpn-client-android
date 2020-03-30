package com.itglobal.vpn_client_android.interactor

import com.itglobal.vpn_client_android.models.Country
import com.itglobal.vpn_client_android.repositories.PreferencesRepository
import com.itglobal.vpn_client_android.repositories.UserRepository
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserInteractor @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val userRepository: UserRepository
) {

    private var disposable: Disposable? = null

    fun getCountries(forceLoad: Boolean): Observable<List<Country>> {
        return if (forceLoad || preferencesRepository.locations == null) userRepository.getCountries().map {
            preferencesRepository.locations = it
            it
        } else
            Observable.create<List<Country>> { emitter ->
                emitter.onNext(preferencesRepository.locations ?: emptyList())
            }
    }

    fun loadCountries() {
        removeObserver()
        disposable = Observable.interval(0, PERIOD_IN_MINUTES, TimeUnit.MINUTES)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { userRepository.getCountries() }
            .subscribe({ preferencesRepository.locations = it }, { })
    }

    fun removeObserver() {
        disposable?.dispose()
        disposable = null
    }

    fun checkUser(token: String, subscriptionId: String, updateToken: Boolean): Observable<Boolean> {
        return Observable.create<Boolean> { emitter ->
            if (preferencesRepository.login == null || updateToken) {
                userRepository.getUser(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { user ->
                        preferencesRepository.login = user.login
                        userRepository.subscribe(token, subscriptionId)
                    }
                    .subscribe(
                        {
                            emitter.onNext(true)
                        },
                        {
                            if (it is HttpException && it.code() == NOT_FOUND_CODE) {
                                val userLogin = preferencesRepository.login ?: UUID.randomUUID().toString()
                                userRepository.login(userLogin)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .flatMap { response ->
                                        preferencesRepository.token = response.key
                                        userRepository.subscribe(token, subscriptionId)
                                    }
                                    .subscribe(
                                        {
                                            emitter.onNext(true)
                                        },
                                        {
                                            userRepository.createUser(userLogin)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .flatMap { user ->
                                                    preferencesRepository.login = user.login
                                                    userRepository.subscribe(token, subscriptionId)
                                                }
                                                .subscribe(
                                                    {
                                                        emitter.onNext(true)
                                                    },
                                                    { error ->
                                                        emitter.onError(error)
                                                    }
                                                )
                                        }
                                    )
                            } else emitter.onError(it)
                        }
                    )
            } else emitter.onNext(true)
        }
    }

    companion object {
        private const val PERIOD_IN_MINUTES = 10L
        private const val NOT_FOUND_CODE = 404
    }
}