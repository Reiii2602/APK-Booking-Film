package com.example.apkbookingfilm

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.apkbookingfilm.util.AppToast

class CinemaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Selalu gunakan tema terang cream-marun (agar status bar & popup konsisten)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                applyLightStatusBar(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                applyLightStatusBar(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                AppToast.setCurrentActivity(activity)
                applyLightStatusBar(activity)
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun applyLightStatusBar(activity: Activity) {
        val window = activity.window ?: return
        window.statusBarColor = ContextCompat.getColor(activity, R.color.bg_cream)
        window.navigationBarColor = ContextCompat.getColor(activity, R.color.bg_white)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }
}
