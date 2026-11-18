package com.example.apkbookingfilm.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.example.apkbookingfilm.BuildConfig
import com.example.apkbookingfilm.databinding.DialogUpdateProgressBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Mengatur alur update di dalam aplikasi:
 * cek GitHub Release -> dialog "Update tersedia" -> unduh APK -> installer Android
 * (APK baru menimpa versi lama, data tetap aman, tidak perlu uninstall).
 */
object UpdateManager {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var latestInfo: UpdateInfo? = null
    private var dismissedThisSession = false
    private var isDownloading = false
    private var activeDialog: AlertDialog? = null

    /** Info update yang menunggu izin "Install unknown apps". */
    private var pendingInstallInfo: UpdateInfo? = null

    /**
     * Cek update. [forceDialog] = true jika user membuka dari notifikasi / menu,
     * dialog tetap ditampilkan walau sebelumnya sudah ditutup.
     */
    fun checkForUpdate(activity: Activity, forceDialog: Boolean = false, showUpToDateToast: Boolean = false) {
        executor.execute {
            val info = UpdateChecker.fetchLatest()
            mainHandler.post {
                if (activity.isFinishing || activity.isDestroyed) return@post
                if (info == null) {
                    if (showUpToDateToast) Toast.makeText(activity, "Gagal cek update. Periksa koneksi internet.", Toast.LENGTH_SHORT).show()
                    return@post
                }
                latestInfo = info
                if (UpdateChecker.isNewer(info)) {
                    UpdateNotifier.showUpdateNotification(activity, info, onlyOnce = true)
                    if (forceDialog || !dismissedThisSession) showUpdateDialog(activity, info)
                } else if (showUpToDateToast) {
                    Toast.makeText(activity, "Aplikasi sudah versi terbaru (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun showUpdateDialog(activity: Activity, info: UpdateInfo) {
        if (isDownloading || activeDialog?.isShowing == true) return
        if (activity is FragmentActivity) {
            val fm = activity.supportFragmentManager
            if (fm.findFragmentByTag(UpdateBottomSheet.TAG) != null || fm.isStateSaved) return
            UpdateBottomSheet(
                info = info,
                onUpdateClick = { startUpdate(activity, info) },
                onDismissClick = { dismissedThisSession = true }
            ).show(fm, UpdateBottomSheet.TAG)
            return
        }

        val sizeText = if (info.apkSize > 0) String.format(Locale.US, " (%.1f MB)", info.apkSize / 1024.0 / 1024.0) else ""
        val changelog = info.changelog.ifEmpty { "Perbaikan bug & peningkatan performa." }
        val message = "Versi baru v${info.versionName}$sizeText sudah tersedia.\n" +
                "Versi kamu sekarang: v${BuildConfig.VERSION_NAME}\n\n" +
                "Yang baru:\n$changelog\n\n" +
                "Update langsung menimpa aplikasi lama — data & login kamu tetap aman, tidak perlu uninstall."

        activeDialog = MaterialAlertDialogBuilder(activity)
            .setTitle("\uD83C\uDF9F\uFE0F Update tersedia")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Update sekarang") { _, _ ->
                activeDialog = null
                startUpdate(activity, info)
            }
            .setNegativeButton("Nanti") { _, _ ->
                activeDialog = null
                dismissedThisSession = true
            }
            .show()
    }

    private fun startUpdate(activity: Activity, info: UpdateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallInfo = info
            MaterialAlertDialogBuilder(activity)
                .setTitle("Izinkan instal update")
                .setMessage("Supaya bisa update langsung dari aplikasi, aktifkan \"Izinkan dari sumber ini\" untuk Cinema Booking, lalu kembali ke aplikasi.")
                .setPositiveButton("Buka pengaturan") { _, _ ->
                    try {
                        activity.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${activity.packageName}")
                            )
                        )
                    } catch (_: ActivityNotFoundException) {
                        openInBrowser(activity, info)
                    }
                }
                .setNegativeButton("Batal") { _, _ -> pendingInstallInfo = null }
                .show()
            return
        }
        downloadAndInstall(activity, info)
    }

    /** Dipanggil dari onResume: lanjutkan update setelah user memberi izin. */
    fun onResume(activity: Activity) {
        val info = pendingInstallInfo ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallInfo = null
            downloadAndInstall(activity, info)
        }
    }

    private fun downloadAndInstall(activity: Activity, info: UpdateInfo) {
        if (isDownloading) return
        isDownloading = true

        val dialogBinding = DialogUpdateProgressBinding.inflate(activity.layoutInflater)
        val progressDialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Mengunduh update v${info.versionName}")
            .setView(dialogBinding.root)
            .setCancelable(false)
            .show()

        val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val apkFile = File(dir, "CinemaBooking-${info.versionName}.apk")

        executor.execute {
            var success = false
            var conn: HttpURLConnection? = null
            try {
                conn = URL(info.apkUrl).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "CinemaBooking-Android")
                conn.connect()
                if (conn.responseCode in 200..299) {
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.apkSize
                    var downloaded = 0L
                    var lastPercent = -1
                    conn.inputStream.use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (total > 0) {
                                    val percent = (downloaded * 100 / total).toInt()
                                    if (percent != lastPercent) {
                                        lastPercent = percent
                                        val mbDone = downloaded / 1024.0 / 1024.0
                                        val mbTotal = total / 1024.0 / 1024.0
                                        mainHandler.post {
                                            dialogBinding.progressUpdate.isIndeterminate = false
                                            dialogBinding.progressUpdate.setProgressCompat(percent, true)
                                            dialogBinding.tvUpdateProgress.text =
                                                String.format(Locale.US, "%d%%  •  %.1f / %.1f MB", percent, mbDone, mbTotal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    success = apkFile.length() > 0
                }
            } catch (_: Exception) {
                success = false
            } finally {
                conn?.disconnect()
            }

            mainHandler.post {
                isDownloading = false
                try { progressDialog.dismiss() } catch (_: Exception) {}
                if (activity.isFinishing || activity.isDestroyed) return@post
                if (success) {
                    UpdateNotifier.cancel(activity)
                    installApk(activity, apkFile, info)
                } else {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Gagal mengunduh")
                        .setMessage("Unduhan update gagal. Coba lagi atau unduh lewat browser.")
                        .setPositiveButton("Coba lagi") { _, _ -> downloadAndInstall(activity, info) }
                        .setNeutralButton("Buka browser") { _, _ -> openInBrowser(activity, info) }
                        .setNegativeButton("Batal", null)
                        .show()
                }
            }
        }
    }

    private fun installApk(activity: Activity, apkFile: File, info: UpdateInfo) {
        try {
            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (_: Exception) {
            openInBrowser(activity, info)
        }
    }

    private fun openInBrowser(activity: Activity, info: UpdateInfo) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, "Tidak bisa membuka link update", Toast.LENGTH_SHORT).show()
        }
    }
}
