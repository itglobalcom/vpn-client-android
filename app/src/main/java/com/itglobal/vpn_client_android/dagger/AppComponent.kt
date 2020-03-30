package com.itglobal.vpn_client_android.dagger

import com.itglobal.vpn_client_android.CountriesDialogFragment
import com.itglobal.vpn_client_android.MainActivity
import com.itglobal.vpn_client_android.MainFragment
import com.itglobal.vpn_client_android.VpnApp
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [RepositoryModule::class, RetrofitModule::class, InteractorModule::class])
interface AppComponent {
    fun inject(application: VpnApp)
    fun inject(activity: MainActivity)
    fun inject(fragment: MainFragment)
    fun inject(fragment: CountriesDialogFragment)
}