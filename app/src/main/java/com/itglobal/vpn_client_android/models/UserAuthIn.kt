package com.itglobal.vpn_client_android.models

import com.google.gson.annotations.SerializedName

data class UserAuthIn(
    @SerializedName("login")
    val userId: String?,
    @SerializedName("secret_key")
    val secretKey: String
)