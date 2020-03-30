package com.itglobal.vpn_client_android.network

import com.itglobal.vpn_client_android.BuildConfig
import com.itglobal.vpn_client_android.models.UserAuthIn
import com.itglobal.vpn_client_android.repositories.PreferencesRepository
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitFactory {

    companion object {
        private const val CONNECT_TIMEOUT = 120L
        private const val WRITE_TIMEOUT = 60L
        private const val TIMEOUT = 60L
        private const val ACCEPT_HEADER = "Accept"
        private const val AUTH_HEADER = "Authorization"
        private const val UNAUTHORIZED_CODE = 401
        private const val SUCCESS_CODE = 200

        fun createRetrofit(preferences: PreferencesRepository): Retrofit {
            val retrofitBuilder = Retrofit.Builder()
                .baseUrl(BuildConfig.BACK_HOST)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))

            val okHttpBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
                .apply {
                    connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    readTimeout(TIMEOUT, TimeUnit.SECONDS)
                }
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpBuilder.authenticator(TokenAuthenticator(preferences))
            okHttpBuilder.addInterceptor(loggingInterceptor)
            okHttpBuilder.addInterceptor { chain ->
                var request: Request = chain.request()
                val builder: Request.Builder = request.newBuilder()
                builder.addHeader(ACCEPT_HEADER, "application/json")
                val token = preferences.token?.let{ "Bearer ${preferences.token}" } ?: ""
                builder.addHeader(AUTH_HEADER, token)
                request = builder.build()
                chain.proceed(request)
            }
            retrofitBuilder.client(okHttpBuilder.readTimeout(TIMEOUT, TimeUnit.SECONDS).build())
            return retrofitBuilder.build()
        }

        class TokenAuthenticator(private val preferences: PreferencesRepository) : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                return if (response.code() == UNAUTHORIZED_CODE) {
                    response.request().newBuilder()
                    val call = Retrofit.Builder()
                        .baseUrl(BuildConfig.BACK_HOST)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(Api::class.java)
                        .login(UserAuthIn(preferences.login, BuildConfig.SECRET_KEY))

                    val refreshResponse = call.execute()
                    if (refreshResponse.code() == SUCCESS_CODE) {
                        val userAuthData = refreshResponse.body()
                        preferences.token = userAuthData?.key
                        return response.request().newBuilder()
                            .removeHeader(AUTH_HEADER)
                            .addHeader(AUTH_HEADER, "Bearer ${userAuthData?.key}")
                            .build()
                    } else {
                        null
                    }
                } else null
            }
        }
    }
}