package com.itglobal.vpn_client_android.dagger

import android.content.Context
import com.itglobal.vpn_client_android.network.Api
import com.itglobal.vpn_client_android.repositories.PreferencesRepository
import com.itglobal.vpn_client_android.repositories.UserRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepositoryModule(private val context: Context) {

    @Provides
    @Singleton
    fun provideUserRepository(api: Api): UserRepository = UserRepository(api)

    @Provides
    @Singleton
    fun providePreferencesRepository(): PreferencesRepository = PreferencesRepository(context)
}