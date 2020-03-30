package com.itglobal.vpn_client_android.models

import com.google.gson.annotations.SerializedName

data class VPNConfig(
    val type: String,
    val config: Config?,
    val country: Country?
) {
    inner class Config(
        val login: String?,
        val password: String?,
        @SerializedName("config_file_data")
        val configFileData: String?,
        val address: String?
    )
}