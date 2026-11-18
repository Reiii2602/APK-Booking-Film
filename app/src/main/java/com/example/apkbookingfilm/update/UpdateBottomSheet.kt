package com.example.apkbookingfilm.update

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.apkbookingfilm.BuildConfig
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.databinding.BottomSheetUpdateBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class UpdateBottomSheet(
    private val info: UpdateInfo,
    private val onUpdateClick: () -> Unit,
    private val onDismissClick: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetUpdateBinding? = null
    private val binding get() = _binding!!
    private var actionTaken = false

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sizeText = if (info.apkSize > 0) {
            String.format(Locale.US, " (%.1f MB)", info.apkSize / 1024.0 / 1024.0)
        } else ""

        binding.tvCurrentVersion.text = "v${BuildConfig.VERSION_NAME}"
        binding.tvNewVersion.text = "v${info.versionName}$sizeText"
        binding.tvUpdateSubtitle.text = "Versi v${info.versionName}$sizeText sudah tersedia. Perbarui sekarang untuk fitur terbaru."

        binding.tvUpdateChangelog.text = formatFriendlyChangelog(info.changelog)

        binding.btnUpdateNow.setOnClickListener {
            actionTaken = true
            dismiss()
            onUpdateClick()
        }

        binding.btnUpdateLater.setOnClickListener {
            actionTaken = true
            dismiss()
            onDismissClick()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (!actionTaken) onDismissClick()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "UpdateBottomSheet"

        /**
         * Mengubah catatan git commit teknis menjadi bahasa Indonesia yang ramah,
         * jelas, dan mudah dipahami oleh pengguna umum bioskop.
         */
        fun formatFriendlyChangelog(raw: String): String {
            if (raw.isBlank()) {
                return "✨ Pembaruan tampilan & fitur bioskop terbaru\n\n🛠️ Peningkatan kestabilan aplikasi"
            }

            val fullTextLower = raw.lowercase(Locale.ROOT)
            val friendlyList = mutableListOf<String>()
            val addedKeys = mutableSetOf<String>()

            fun addBullet(key: String, bullet: String) {
                if (addedKeys.add(key)) {
                    friendlyList.add(bullet)
                }
            }

            if (fullTextLower.contains("map") || fullTextLower.contains("peta") || fullTextLower.contains("simetris") || fullTextLower.contains("draggable")) {
                addBullet("map", "🗺️ Peta Bioskop: Peta interaktif kini dapat digeser & dizoom dengan tombol yang presisi")
            }
            if (fullTextLower.contains("create account") || fullTextLower.contains("register") || fullTextLower.contains("daftar") || fullTextLower.contains("sign in") || fullTextLower.contains("login") || fullTextLower.contains("profile") || fullTextLower.contains("profil")) {
                addBullet("account", "👤 Akun & Profil: Menu pendaftaran akun baru lebih lengkap dengan Google, Apple, dan Email")
            }
            if (fullTextLower.contains("notif") || fullTextLower.contains("toast") || fullTextLower.contains("bulat") || fullTextLower.contains("logo")) {
                addBullet("toast", "🔔 Tampilan Notifikasi: Ikon notifikasi kini tampil bulat rapi dan lebih elegan")
            }
            if (fullTextLower.contains("changelog") || fullTextLower.contains("awam") || fullTextLower.contains("update")) {
                addBullet("changelog", "📋 Catatan Pembaruan: Rincian update kini menggunakan bahasa yang ramah dan mudah dipahami")
            }
            if (fullTextLower.contains("food") || fullTextLower.contains("drink") || fullTextLower.contains("snack") || fullTextLower.contains("camilan") || fullTextLower.contains("popcorn")) {
                addBullet("food", "🍿 Cinema Food & Drinks: Pesan Popcorn dan Minuman bioskop favorit langsung di aplikasi")
            }
            if (fullTextLower.contains("trailer") || fullTextLower.contains("cuplikan") || fullTextLower.contains("video")) {
                addBullet("trailer", "🎬 Cuplikan Film: Tonton trailer film resmi langsung tanpa harus keluar aplikasi")
            }
            if (fullTextLower.contains("seat") || fullTextLower.contains("kursi") || fullTextLower.contains("bangku")) {
                addBullet("seats", "🎟️ Pilih Kursi: Denah kursi bioskop interaktif dengan pilihan jadwal lengkap")
            }
            if (fullTextLower.contains("payment") || fullTextLower.contains("bayar") || fullTextLower.contains("proof") || fullTextLower.contains("bukti") || fullTextLower.contains("transfer") || fullTextLower.contains("bank") || fullTextLower.contains("ovo") || fullTextLower.contains("gopay") || fullTextLower.contains("dana")) {
                addBullet("payment", "💳 Pembayaran Mudah: Dukungan berbagai bank, e-wallet, dan upload bukti bayar")
            }
            if (fullTextLower.contains("gps") || fullTextLower.contains("scan") || fullTextLower.contains("city") || fullTextLower.contains("kota") || fullTextLower.contains("theater") || fullTextLower.contains("bioskop")) {
                addBullet("theaters", "📍 Bioskop Sekitar: Scan GPS & cek bioskop terdekat di berbagai kota Indonesia")
            }
            if (fullTextLower.contains("tix") || fullTextLower.contains("showing") || fullTextLower.contains("tayang") || fullTextLower.contains("highlight")) {
                addBullet("ui", "✨ Tampilan Beranda: Tampilan film Sedang Tayang & Akan Tayang lebih segar dan dinamis")
            }
            if (fullTextLower.contains("fix") || fullTextLower.contains("perbaikan") || fullTextLower.contains("bug")) {
                addBullet("fix", "🛠️ Perbaikan Sistem: Peningkatan kestabilan dan perbaikan beberapa kendala teknis")
            }

            if (friendlyList.isEmpty()) {
                val lines = raw.split("\n", ";", ",")
                    .map {
                        it.trim()
                            .replace(Regex("^(feat|fix|style|refactor|perf|chore)(\\(.*?\\))?:\\s*", RegexOption.IGNORE_CASE), "")
                            .trimStart('-', '*', '•')
                            .trim()
                    }
                    .filter {
                        it.isNotBlank() &&
                        !it.startsWith("Update otomatis", ignoreCase = true) &&
                        !it.startsWith("Perubahan terbaru", ignoreCase = true) &&
                        !it.matches(Regex("[0-9a-f]{7,40}"))
                    }

                for (l in lines) {
                    friendlyList.add("✨ $l")
                    if (friendlyList.size >= 4) break
                }
            }

            if (friendlyList.isEmpty()) {
                return "✨ Pembaruan tampilan & fitur bioskop terbaru\n\n🛠️ Peningkatan kestabilan aplikasi"
            }

            return friendlyList.joinToString("\n\n")
        }
    }
}
