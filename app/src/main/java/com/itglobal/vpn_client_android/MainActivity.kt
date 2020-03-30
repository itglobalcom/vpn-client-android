package com.itglobal.vpn_client_android

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.itglobal.vpn_client_android.VpnApp.Companion.INSTANCE
import com.itglobal.vpn_client_android.interactor.UserInteractor
import com.itglobal.vpn_client_android.repositories.PreferencesRepository
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferences: PreferencesRepository
    @Inject
    lateinit var interactor: UserInteractor

    override fun onCreate(savedInstanceState: Bundle?) {
        INSTANCE.provideAppComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val isPhone = resources.getBoolean(R.bool.isPhone)
        if (isPhone) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        interactor.loadCountries()
    }

    override fun onDestroy() {
        super.onDestroy()
        interactor.removeObserver()
    }
}
