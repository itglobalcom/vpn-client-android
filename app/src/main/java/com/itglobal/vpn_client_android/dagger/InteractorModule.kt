package com.itglobal.vpn_client_android.dagger

import com.itglobal.vpn_client_android.interactor.UserInteractor
import com.itglobal.vpn_client_android.repositories.PreferencesRepository
import com.itglobal.vpn_client_android.repositories.UserRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class InteractorModule {

    @Provides
    @Singleton
    fun provideUserInteractor(
        preferencesRepository: PreferencesRepository,
        userRepository: UserRepository
    ) = UserInteractor(preferencesRepository, userRepository)
}