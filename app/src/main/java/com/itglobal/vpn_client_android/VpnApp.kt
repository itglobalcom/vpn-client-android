package com.itglobal.vpn_client_android

import android.app.Application
import com.itglobal.vpn_client_android.dagger.AppComponent
import com.itglobal.vpn_client_android.dagger.DaggerAppComponent
import com.itglobal.vpn_client_android.dagger.RepositoryModule

class VpnApp : Application() {

    companion object {
        lateinit var INSTANCE: VpnApp
    }

    override fun onCreate() {
        super.onCreate()
        appComponent.inject(this)
        INSTANCE = this
    }

    private val appComponent: AppComponent by lazy {
        DaggerAppComponent.builder()
            .repositoryModule(RepositoryModule(this))
            .build()
    }

    fun provideAppComponent() = appComponent
}