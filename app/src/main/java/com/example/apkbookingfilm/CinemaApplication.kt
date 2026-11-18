package com.example.apkbookingfilm

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.example.apkbookingfilm.util.AppLanguage
import com.example.apkbookingfilm.util.AppToast

class CinemaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inisialisasi bahasa aplikasi (default: ENG)
        AppLanguage.init(this)
        // Selalu gunakan tema terang cream-marun (agar status bar & popup konsisten)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                applyLightStatusBar(activity)
                if (activity is FragmentActivity) {
                    activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                        object : FragmentManager.FragmentLifecycleCallbacks() {
                            override fun onFragmentViewCreated(
                                fm: FragmentManager,
                                f: Fragment,
                                v: View,
                                savedInstanceState: Bundle?
                            ) {
                                AppLanguage.applyToView(v)
                            }

                            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                                AppLanguage.applyToView(f.view)
                                if (f is DialogFragment) {
                                    AppLanguage.applyToView(f.dialog?.window?.decorView)
                                }
                            }
                        },
                        true
                    )
                }
            }

            override fun onActivityStarted(activity: Activity) {
                applyLightStatusBar(activity)
                AppLanguage.applyToView(activity.window?.decorView)
            }

            override fun onActivityResumed(activity: Activity) {
                AppToast.setCurrentActivity(activity)
                applyLightStatusBar(activity)
                AppLanguage.applyToView(activity.window?.decorView)
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
