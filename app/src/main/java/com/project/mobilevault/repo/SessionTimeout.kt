package com.project.mobilevault.repo

import android.os.SystemClock

/**
 * Simple idle timeout tracker. Call poke() on user interaction.
 * shouldTimeout() returns true if the inactivity exceeds timeoutMs.
 */
class SessionTimeout(private val timeoutMs: Long = 2 * 60_000L) {
    @Volatile private var lastInteraction: Long = SystemClock.elapsedRealtime()

    fun poke() {
        lastInteraction = SystemClock.elapsedRealtime()
    }

    fun shouldTimeout(): Boolean = SystemClock.elapsedRealtime() - lastInteraction > timeoutMs
}