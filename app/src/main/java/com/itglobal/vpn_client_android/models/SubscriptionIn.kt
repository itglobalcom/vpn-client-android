package com.itglobal.vpn_client_android.models

import com.google.gson.annotations.SerializedName

data class SubscriptionIn(
    val token: String,
    @SerializedName("subscription_id")
    val subscriptionId: String
)