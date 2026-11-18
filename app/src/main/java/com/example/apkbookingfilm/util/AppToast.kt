package com.example.apkbookingfilm.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.apkbookingfilm.R

/**
 * Toast kustom dengan logo tiket bioskop agar ikonnya selalu logo aplikasi (bukan ikon Android).
 */
object AppToast {
    private var currentToast: Toast? = null

    fun show(context: Context, message: String, longDuration: Boolean = false) {
        val appCtx = context.applicationContext
        val view = LayoutInflater.from(appCtx).inflate(R.layout.view_app_toast, null)
        view.findViewById<TextView>(R.id.tvAppToastMessage).text = message

        currentToast?.cancel()
        @Suppress("DEPRECATION")
        val toast = Toast(appCtx).apply {
            duration = if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            setView(view)
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, (84 * appCtx.resources.displayMetrics.density).toInt())
        }
        currentToast = toast
        toast.show()
    }
}
