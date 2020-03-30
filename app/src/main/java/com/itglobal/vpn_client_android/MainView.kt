package com.itglobal.vpn_client_android

import com.android.billingclient.api.Purchase
import com.hannesdorfmann.mosby3.mvp.MvpView
import com.itglobal.vpn_client_android.models.Status

interface MainView : MvpView {
    fun showConnectionStatus(status: Status, address: String, flagUrl: String?)
    fun showProgress(value: Int)
    fun showSelectedLocation(location: String, flagUrl: String?)
    fun enableVpnConnectButton(enable: Boolean)
    fun enableCountryButton(enable: Boolean)
    fun showSubscribeButton(show: Boolean)
    fun showMessage(message: String)
    fun getPurchases(): List<Purchase>?
    fun showLocationsDialog()
    fun prepareVpn()
    fun subscribe()
}