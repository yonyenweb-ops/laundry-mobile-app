package com.laundryhub.mobile.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Remembers who is logged in (saved on the phone, so you stay logged in).
 * `role` is Compose state: when it changes, the app switches screens automatically.
 */
object Session {

    /** Emulator: 10.0.2.2 means "the PC". Real phone: use your PC's IP, e.g. http://192.168.1.15:8080/ */
    const val DEFAULT_SERVER = "http://10.0.2.2:8080/"

    private lateinit var prefs: SharedPreferences

    var role by mutableStateOf<String?>(null)
        private set
    var displayName by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var verified by mutableStateOf(true)
        private set
    var token: String? = null
        private set
    var serverUrl: String = DEFAULT_SERVER
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("laundryhub", Context.MODE_PRIVATE)
        token = prefs.getString("token", null)
        serverUrl = prefs.getString("server", DEFAULT_SERVER) ?: DEFAULT_SERVER
        displayName = prefs.getString("name", "") ?: ""
        email = prefs.getString("email", "") ?: ""
        verified = prefs.getBoolean("verified", true)
        role = if (token != null) prefs.getString("role", null) else null
    }

    fun saveServer(url: String) {
        var clean = url.trim()
        if (!clean.startsWith("http")) clean = "http://$clean"
        if (!clean.endsWith("/")) clean += "/"
        serverUrl = clean
        prefs.edit().putString("server", clean).apply()
    }

    fun login(auth: AuthResponse) {
        token = auth.token
        displayName = auth.displayName
        email = auth.email
        verified = auth.verified
        prefs.edit()
            .putString("token", auth.token)
            .putString("role", auth.role)
            .putString("name", auth.displayName)
            .putString("email", auth.email)
            .putBoolean("verified", auth.verified)
            .apply()
        role = auth.role          // switches the UI to this role's home
    }

    fun updateVerified(value: Boolean) {
        verified = value
        prefs.edit().putBoolean("verified", value).apply()
    }

    /**
     * A driver's hand-picked location name (null = use GPS). Kept per account on this phone, so
     * logging in on a shared emulator doesn't replace the chosen place with the emulator's GPS.
     */
    var chosenPlace: String?
        get() = prefs.getString("chosen_place_$email", null)
        set(value) {
            prefs.edit().apply { if (value == null) remove("chosen_place_$email") else putString("chosen_place_$email", value) }.apply()
        }

    fun logout() {
        token = null
        prefs.edit().remove("token").remove("role").apply()
        role = null
    }
}
