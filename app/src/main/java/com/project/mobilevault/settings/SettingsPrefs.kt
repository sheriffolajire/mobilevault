package com.project.mobilevault.settings

import android.content.Context

class SettingsPrefs(context: Context) {
    private val sp = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    var biometricsEnabled: Boolean
        get() = sp.getBoolean(KEY_BIOMETRICS_ENABLED, true)
        set(v) { sp.edit().putBoolean(KEY_BIOMETRICS_ENABLED, v).apply() }

    var idleTimeoutMs: Long
        get() = sp.getLong(KEY_IDLE_TIMEOUT_MS, 2 * 60_000L)
        set(v) { sp.edit().putLong(KEY_IDLE_TIMEOUT_MS, v).apply() }

    var stillnessMs: Long
        get() = sp.getLong(KEY_STILLNESS_MS, 30_000L)
        set(v) { sp.edit().putLong(KEY_STILLNESS_MS, v).apply() }

    var bgGraceMs: Long
        get() = sp.getLong(KEY_BG_GRACE_MS, 15_000L)
        set(v) { sp.edit().putLong(KEY_BG_GRACE_MS, v).apply() }

    var autoWipeEnabled: Boolean
        get() = sp.getBoolean(KEY_AUTO_WIPE, false)
        set(v) { sp.edit().putBoolean(KEY_AUTO_WIPE, v).apply() }

    var wipeThreshold: Int
        get() = sp.getInt(KEY_WIPE_THRESHOLD, 10)
        set(v) { sp.edit().putInt(KEY_WIPE_THRESHOLD, v).apply() }

    // New: auto-delete originals after successful import
    var autoDeleteOnImport: Boolean
        get() = sp.getBoolean(KEY_AUTO_DELETE_ON_IMPORT, false)
        set(v) { sp.edit().putBoolean(KEY_AUTO_DELETE_ON_IMPORT, v).apply() }

    companion object {
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_IDLE_TIMEOUT_MS = "idle_timeout_ms"
        private const val KEY_STILLNESS_MS = "stillness_ms"
        private const val KEY_BG_GRACE_MS = "bg_grace_ms"
        private const val KEY_AUTO_WIPE = "auto_wipe"
        private const val KEY_WIPE_THRESHOLD = "wipe_threshold"
        private const val KEY_AUTO_DELETE_ON_IMPORT = "auto_delete_on_import"
    }
}