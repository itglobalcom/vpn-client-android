package com.itglobal.vpn_client_android.extensions

import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.itglobal.vpn_client_android.BuildConfig
import com.itglobal.vpn_client_android.R
import com.squareup.picasso.Picasso

fun ImageView.loadFlag(imageUrl: String?) {
    if (imageUrl == null || imageUrl.isEmpty()) {
        this.setImageResource(R.drawable.ic_global)
    } else {
        Picasso.get().load("${BuildConfig.IMAGES_BASE_URL}${imageUrl}").into(this)
    }
}

fun TextView.setHtmlText(text: String) {
    this.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
}