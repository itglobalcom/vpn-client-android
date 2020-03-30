package com.itglobal.vpn_client_android.dagger

import com.itglobal.vpn_client_android.network.Api
import com.itglobal.vpn_client_android.network.RetrofitFactory
import com.itglobal.vpn_client_android.repositories.PreferencesRepository
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class RetrofitModule {

    @Provides
    @Singleton
    fun provideRetrofit(preferences: PreferencesRepository): Retrofit = RetrofitFactory.createRetrofit(preferences)

    @Provides
    @Singleton
    fun provideApiRetrofit(retrofit: Retrofit): Api = retrofit.create(Api::class.java)
}