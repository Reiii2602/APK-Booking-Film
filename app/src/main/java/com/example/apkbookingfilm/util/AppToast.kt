package com.example.apkbookingfilm.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.example.apkbookingfilm.R
import java.lang.ref.WeakReference

/**
 * Floating banner notification rendered inside the active Activity window so it
 * always displays the real Cinema Ticket launcher icon (@mipmap/ic_launcher_round).
 */
object AppToast {
    private var activeActivityRef: WeakReference<Activity>? = null
    private var activeViewRef: WeakReference<View>? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    fun setCurrentActivity(activity: Activity?) {
        activeActivityRef = if (activity != null) WeakReference(activity) else null
    }

    fun show(context: Context, message: String, longDuration: Boolean = false) {
        mainHandler.post {
            val activity = unwrapActivity(context) ?: activeActivityRef?.get()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                showInActivity(activity, message, if (longDuration) 3200L else 2100L)
            } else {
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showInActivity(activity: Activity, message: String, durationMs: Long) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        removeCurrentBanner()

        val banner = LayoutInflater.from(activity).inflate(R.layout.view_app_toast, root, false)
        banner.findViewById<TextView>(R.id.tvAppToastMessage).text = message

        val dp = activity.resources.displayMetrics.density
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            bottomMargin = (86 * dp).toInt()
            marginStart = (20 * dp).toInt()
            marginEnd = (20 * dp).toInt()
        }

        banner.alpha = 0f
        banner.translationY = 18 * dp
        banner.elevation = 24f
        root.addView(banner, lp)
        banner.animate().alpha(1f).translationY(0f).setDuration(180).start()

        activeViewRef = WeakReference(banner)
        val hideTask = Runnable {
            banner.animate().alpha(0f).translationY(12 * dp).setDuration(180).withEndAction {
                (banner.parent as? ViewGroup)?.removeView(banner)
            }.start()
        }
        dismissRunnable = hideTask
        mainHandler.postDelayed(hideTask, durationMs)
    }

    private fun removeCurrentBanner() {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        dismissRunnable = null
        activeViewRef?.get()?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        activeViewRef = null
    }

    private fun unwrapActivity(ctx: Context): Activity? {
        var cur = ctx
        while (cur is ContextWrapper) {
            if (cur is Activity) return cur
            cur = cur.baseContext
        }
        return null
    }
}
