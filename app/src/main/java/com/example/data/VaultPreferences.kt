package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

class VaultPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vaultkeep_security_prefs", Context.MODE_PRIVATE)

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var hasMasterPassword: Boolean
        get() = prefs.getString(KEY_MASTER_HASH, null) != null
        private set(_) {}

    var masterPasswordHash: String?
        get() = prefs.getString(KEY_MASTER_HASH, null)
        private set(_) {}

    fun setMasterPassword(hash: String, salt: ByteArray) {
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_MASTER_HASH, hash)
            .putString(KEY_MASTER_SALT, saltBase64)
            .apply()
    }

    fun getSalt(): ByteArray? {
        val saltBase64 = prefs.getString(KEY_MASTER_SALT, null) ?: return null
        return try {
            Base64.decode(saltBase64, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var autoLockMinutes: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_MINUTES, 5)
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_MINUTES, value).apply()

    var isAutofillConfigured: Boolean
        get() = prefs.getBoolean(KEY_AUTOFILL_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTOFILL_CONFIGURED, value).apply()

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_MASTER_HASH = "key_master_hash"
        private const val KEY_MASTER_SALT = "key_master_salt"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_AUTO_LOCK_MINUTES = "key_auto_lock_minutes"
        private const val KEY_AUTOFILL_CONFIGURED = "key_autofill_configured"
    }
}
