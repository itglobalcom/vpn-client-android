package com.itglobal.vpn_client_android.network

import com.itglobal.vpn_client_android.models.*
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface Api {

    @POST("login")
    fun login(@Body body: UserAuthIn): Call<UserAuthOut>

    @POST("login")
    fun checkLogin(@Body body: UserAuthIn): Observable<UserAuthOut>

    @POST("subscribe")
    fun subscribe(@Body body: SubscriptionIn): Observable<ResponseBody>

    @GET("countries")
    fun getCountries(): Observable<List<Country>>

    @POST("connect/{protocol}")
    fun connect(
        @Path("protocol") protocol: String,
        @Query("country") country: String
    ): Single<VPNConfig>

    @POST("users/get_user_by_token")
    fun getUserByToken(@Body requestBody: RequestBody): Observable<UserLogin?>

    @POST("users")
    fun createUser(@Body requestBody: RequestBody): Observable<UserLogin>
}