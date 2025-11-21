package com.project.mobilevault

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import com.project.mobilevault.di.ServiceLocator

class MobileVaultApp : Application() {
    private var wentBgAt: Long? = null
    private var started = 0
    private var graceMs: Long = 15_000L // default; will read from settings

    override fun onCreate() {
        super.onCreate()
        // Read background grace from settings
        try {
            graceMs = com.project.mobilevault.settings.SettingsPrefs(this).bgGraceMs
        } catch (_: Throwable) {}
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                started++
                if (started == 1) {
                    // app moved to foreground
                    val t = wentBgAt
                    if (t != null && SystemClock.elapsedRealtime() - t > graceMs) {
                        ServiceLocator.session().clear()
                    }
                    wentBgAt = null
                }
            }
            override fun onActivityStopped(activity: Activity) {
                started--
                if (started == 0) {
                    // app moved to background
                    wentBgAt = SystemClock.elapsedRealtime()
                }
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
}