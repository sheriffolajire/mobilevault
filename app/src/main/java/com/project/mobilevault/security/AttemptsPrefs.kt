package com.project.mobilevault.security

import android.content.Context

class AttemptsPrefs(context: Context) {
    private val sp = context.getSharedPreferences("attempts_prefs", Context.MODE_PRIVATE)

    var count: Int
        get() = sp.getInt(KEY_COUNT, 0)
        set(v) { sp.edit().putInt(KEY_COUNT, v).apply() }

    fun reset() { count = 0 }

    companion object {
        private const val KEY_COUNT = "count"
    }
}