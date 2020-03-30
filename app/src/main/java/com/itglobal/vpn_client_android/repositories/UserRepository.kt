package com.itglobal.vpn_client_android.repositories

import com.google.gson.JsonObject
import com.itglobal.vpn_client_android.BuildConfig
import com.itglobal.vpn_client_android.models.*
import com.itglobal.vpn_client_android.network.Api
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val api: Api) {

    fun login(login: String): Observable<UserAuthOut> {
        return api.checkLogin(UserAuthIn(login, BuildConfig.SECRET_KEY))
    }

    fun getUser(subscriptionToken: String): Observable<UserLogin?> {
        val jsonObject = JsonObject()
        jsonObject.addProperty(SECRET_KEY, BuildConfig.SECRET_KEY)
        jsonObject.addProperty(KEY_TOKEN, subscriptionToken)
        val body = RequestBody.create(MediaType.parse(JSON_TYPE), jsonObject.toString())
        return api.getUserByToken(body)
    }

    fun createUser(login: String): Observable<UserLogin> {
        val jsonObject = JsonObject()
        jsonObject.addProperty(SECRET_KEY, BuildConfig.SECRET_KEY)
        jsonObject.addProperty(KEY_LOGIN, login)
        val body = RequestBody.create(MediaType.parse(JSON_TYPE), jsonObject.toString())
        return api.createUser(body)
    }

    fun subscribe(subscriptionToken: String, subscriptionId: String): Observable<ResponseBody> {
        val body = SubscriptionIn(subscriptionToken, subscriptionId)
        return api.subscribe(body)
    }

    fun getCountries(): Observable<List<Country>> {
        return api.getCountries()
    }

    fun connect(country: String): Single<VPNConfig> = api.connect(OPEN_VPN_PROTOCOL, country)

    companion object {
        private const val OPEN_VPN_PROTOCOL = "openvpn"
        private const val SECRET_KEY = "secret_key"
        private const val KEY_TOKEN = "token"
        private const val KEY_LOGIN = "login"
        private const val JSON_TYPE = "application/json; charset=utf-8"
    }
}