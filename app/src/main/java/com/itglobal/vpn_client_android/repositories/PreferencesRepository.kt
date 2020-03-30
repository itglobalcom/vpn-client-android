package com.itglobal.vpn_client_android.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itglobal.vpn_client_android.models.Country
import com.itglobal.vpn_client_android.models.VPNConfig
import javax.inject.Singleton

@Singleton
class PreferencesRepository(private val context: Context) {

    private val prefs: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(
            context
        )
    private val editor: SharedPreferences.Editor get() = prefs.edit()

    var token: String?
        get() = prefs.getString(TOKEN, null)
        set(value) {
            editor.putString(TOKEN, value).apply()
        }

    var login: String?
        get() = prefs.getString(LOGIN, null)
        set(value) {
            editor.putString(LOGIN, value).apply()
        }

    var country: Country?
        get() = if (prefs.getString(COUNTRY, null) != null) {
            Gson().fromJson(prefs.getString(COUNTRY, null), Country::class.java)
        } else null
        set(value) {
            editor.putString(COUNTRY, Gson().toJson(value)).apply()
        }

    private val type = object : TypeToken<List<Country>>() {}.type
    var locations: List<Country>?
        get() = if (prefs.getString(LOCATIONS, null) != null) {
            Gson().fromJson(prefs.getString(LOCATIONS, null), type)
        } else null
        set(value) {
            editor.putString(LOCATIONS, Gson().toJson(value, type)).apply()
        }

    var config: VPNConfig?
        get() = if (prefs.getString(CONFIG, null) != null) {
            Gson().fromJson(prefs.getString(CONFIG, null), VPNConfig::class.java)
        } else null
        set(value) {
            editor.putString(CONFIG, Gson().toJson(value)).apply()
        }

    companion object {
        private const val TOKEN = "token"
        private const val LOGIN = "login"
        private const val COUNTRY = "country"
        private const val LOCATIONS = "locations"
        private const val CONFIG = "config"
    }
}