package com.example.apkbookingfilm.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.R

object UpdateNotifier {

    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 7001
    private const val PREFS = "app_update_prefs"
    private const val KEY_LAST_NOTIFIED = "last_notified_version"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Update Aplikasi",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Pemberitahuan saat versi baru aplikasi tersedia" }
                nm.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Tampilkan notifikasi "Update tersedia". [onlyOnce] = true -> tiap versi hanya
     * dinotifikasi sekali (dipakai oleh pengecekan background).
     */
    fun showUpdateNotification(context: Context, info: UpdateInfo, onlyOnce: Boolean) {
        if (!com.example.apkbookingfilm.data.MovieRepository(context).areNotificationsEnabled()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (onlyOnce && prefs.getInt(KEY_LAST_NOTIFIED, 0) >= info.versionCode) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_SHOW_UPDATE, true)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "Versi ${info.versionName} sudah tersedia. Ketuk untuk update sekarang."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ticket)
            .setColor(ContextCompat.getColor(context, R.color.maroon_primary))
            .setContentTitle("Update tersedia \uD83C\uDF9F\uFE0F")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (info.changelog.isNotEmpty()) "$text\n\n${info.changelog.take(300)}" else text
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .addAction(R.drawable.ic_upload, "Update sekarang", pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            prefs.edit().putInt(KEY_LAST_NOTIFIED, info.versionCode).apply()
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
