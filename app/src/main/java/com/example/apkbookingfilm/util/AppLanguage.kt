package com.example.apkbookingfilm.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout

/**
 * Manajemen bahasa aplikasi (Default: ENG, dapat dialihkan ke ID / ENG dari header Homepage).
 * Menerjemahkan teks statis XML maupun string dinamis dua arah antara English <-> Indonesia.
 */
object AppLanguage {

    const val LANG_ENG = "ENG"
    const val LANG_ID = "ID"

    private const val PREFS_NAME = "app_cinema_prefs"
    private const val KEY_LANGUAGE = "app_language_v1"

    @Volatile
    var current: String = LANG_ENG
        private set

    private val listeners = mutableListOf<() -> Unit>()

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        current = prefs.getString(KEY_LANGUAGE, LANG_ENG) ?: LANG_ENG
    }

    fun isId(): Boolean = current == LANG_ID

    fun isEng(): Boolean = current == LANG_ENG

    fun setLanguage(context: Context, lang: String) {
        val target = if (lang == LANG_ID) LANG_ID else LANG_ENG
        if (current == target) return
        current = target
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, target).apply()
        listeners.toList().forEach { it.invoke() }
    }

    fun addListener(listener: () -> Unit) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun tr(en: String, id: String): String = if (isId()) id else en

    /** Pasangan terjemahan exact-match (first = English, second = Bahasa Indonesia). */
    private val pairs: List<Pair<String, String>> = listOf(
        // Bottom Nav & General
        "Tickets" to "Tiket",
        "Saved" to "Tersimpan",
        "Theaters" to "Bioskop",
        "Profile" to "Profil",
        "All >" to "Semua >",
        "See More!" to "Lihat Lainnya!",
        "ALL" to "Semua",
        "All" to "Semua",

        // Home Header & Location
        "Sign in to save tickets & earn points" to "Masuk untuk simpan tiket & kumpulkan poin",
        "Book your favorite movies & snacks today" to "Pesan film & camilan favoritmu hari ini",
        "SIGN IN" to "MASUK",
        "LOGIN" to "LOGIN",

        // Home Highlight & Sections
        "FEATURED FILM" to "FILM PILIHAN",
        "BUY TICKET" to "BELI TIKET",
        "BOOK" to "PESAN",
        "🎬 Now Showing" to "🎬 Sedang Tayang",
        "Now Showing" to "Sedang Tayang",
        "Coming Soon" to "Akan Tayang",
        "All Movies" to "Semua Film",
        "♡ My Watchlist" to "♡ Watchlist Saya",
        "NOW SHOWING" to "SEDANG TAYANG",
        "PRE-SALE" to "PRE-SALE",
        "COMING SOON" to "SEGERA TAYANG",
        "🍿 Cinema Food" to "🍿 Cinema Food",
        "Enjoy the show with your favorite snacks. Order easily with Cinema Food!" to "Nonton lebih asik bareng camilan favoritmu. Pesannya lebih mudah pake Cinema Food!",
        "BUY NOW" to "BELI SEKARANG",
        "⏳ Coming Soon" to "⏳ Akan Tayang",
        "Exciting movies coming soon to theaters" to "Film-film seru yang segera tayang di bioskop",
        "Popular" to "Populer",
        "Top Rated" to "Rating Tertinggi",
        "Partner Theaters" to "Bioskop Partner",

        // All Movies / Search
        "Search movies on TMDB..." to "Cari film di TMDB...",
        "No movies found" to "Film tidak ditemukan",

        // Movie Detail
        "PLAY TRAILER" to "PUTAR CUPLIKAN",
        "TRAILER" to "CUPLIKAN",
        "Cast" to "Pemeran",
        "Loading cast..." to "Memuat data pemeran...",
        "Select a showtime" to "Pilih jadwal tayang",
        "All formats ⌄" to "Semua format ⌄",
        "CHOOSE SEATS  >" to "PILIH KURSI  >",
        "Choose Studio Format" to "Pilih Format Studio",
        "Filter showtimes by studio type" to "Filter jadwal tayang berdasarkan tipe studio",

        // Seat Selection
        "Choose your seats" to "Pilih kursi kamu",
        "SCREEN" to "LAYAR",
        "Available" to "Tersedia",
        "Taken" to "Terisi",
        "Selected" to "Dipilih",
        "Your seats" to "Kursi kamu",
        "No seats" to "Belum ada kursi",
        "No seats selected — tap an available seat above." to "Belum ada kursi dipilih — ketuk kursi tersedia di atas.",
        "🍿 Add Food & Drinks" to "🍿 Tambah Makanan & Minuman",
        "Pick up at the cinema counter before entering (optional)" to "Ambil di konter bioskop sebelum masuk studio (opsional)",
        "No seats selected" to "Belum ada kursi dipilih",
        "Tap available seats above" to "Ketuk kursi tersedia di atas",
        "Tap a seat to select" to "Ketuk kursi untuk memilih",
        "CONTINUE TO PAYMENT  >" to "LANJUT KE PEMBAYARAN  >",

        // Food Menu BottomSheet
        "Pick up at your chosen cinema counter" to "Ambil di konter bioskop pilihanmu",
        "Save" to "Simpan",
        "Pay" to "Bayar",

        // Payment Activity
        "Payment" to "Pembayaran",
        "Transfer the exact amount, then upload your proof of payment." to "Transfer sesuai nominal, lalu unggah bukti pembayaran.",
        "Complete payment within" to "Selesaikan pembayaran dalam",
        "ORDER SUMMARY" to "RINGKASAN PESANAN",
        "Movie" to "Film",
        "Showtime" to "Jadwal Tayang",
        "Cinema" to "Bioskop",
        "Seats" to "Kursi",
        "Food" to "Makanan",
        "Drinks" to "Minuman",
        "Food & Drinks" to "Makanan & Minuman",
        "Total Items" to "Jumlah Item",
        "Pickup" to "Pengambilan",
        "PAYMENT METHOD" to "METODE PEMBAYARAN",
        "Change Method >" to "Ganti Metode >",
        "Bank Transfer" to "Transfer Bank",
        "Account / E-Wallet Number" to "Nomor Rekening / E-Wallet",
        "Copy" to "Salin",
        "Account Holder" to "Atas Nama",
        "Amount to transfer" to "Nominal transfer",
        "Upload transfer proof" to "Unggah bukti transfer",
        "Required" to "Wajib",
        "Tap to upload a screenshot or receipt" to "Ketuk untuk unggah screenshot atau struk",
        "JPG, PNG or PDF • max 5 MB" to "JPG, PNG atau PDF • maks 5 MB",
        "✓ Ready • Uploaded" to "✓ Terpilih",
        "Transfer Proof Preview:" to "Preview Bukti Transfer:",
        "Verification usually takes up to 15 minutes. You will get your e-ticket right after." to "Verifikasi memakan waktu maksimal 15 menit. E-ticket kamu akan langsung terbit setelahnya.",
        "SUBMIT PAYMENT PROOF  >" to "KIRIM BUKTI BAYAR  >",

        // Ticket Success Activity
        "Payment confirmed!" to "Pembayaran berhasil!",
        "Your digital cinema ticket is ready" to "Tiket bioskop digital kamu sudah siap",
        "Your digital F&B order voucher is ready" to "Voucher pesanan makanan & minuman kamu sudah siap",
        "Date" to "Tanggal",
        "Time" to "Jam",
        "Booking ID" to "ID Pesanan",
        "Order Type" to "Jenis Pesanan",
        "SCAN AT CINEMA TURNSTILE" to "SCAN DI PINTU MASUK STUDIO",
        "SCAN AT CINEMA F&B COUNTER" to "SCAN DI KONTER F&B BIOSKOP",
        "SAVE TICKET TO DEVICE" to "SIMPAN TIKET KE PERANGKAT",
        "BACK TO HOME  >" to "KEMBALI KE BERANDA  >",

        // Saved Fragment
        "Saved films" to "Film tersimpan",
        "Your watchlist and bookmarked movies" to "Daftar tontonan dan film favorit kamu",
        "No saved films yet" to "Belum ada film tersimpan",
        "Bookmark films from Home to quickly book them later." to "Tandai film dari Beranda agar mudah dipesan nanti.",

        // Theaters Fragment
        "Find nearest cinemas & scan locations across Indonesia." to "Temukan bioskop terdekat & scan lokasi di seluruh Indonesia.",
        "Scan Location" to "Scan Lokasi",
        "Search cinema or mall name..." to "Cari nama bioskop atau mall...",
        "Open Nearby Cinema Map" to "Buka Peta Bioskop Sekitar",
        "Tap for schedule" to "Ketuk untuk jadwal",

        // Profile Fragment (Guest & Logged In)
        "Sign in to see your tickets, saved films and rewards." to "Masuk untuk lihat tiket, film tersimpan, dan poinmu.",
        "Manage your account, tickets and preferences." to "Kelola akun, tiket, dan preferensi kamu.",
        "Book faster and earn points on every ticket." to "Pesan tiket lebih cepat & kumpulkan poin di setiap transaksi.",
        "One account for your e-tickets, saved films and reminders on any device." to "Satu akun untuk e-ticket, film tersimpan, dan pengingat di semua perangkat.",
        "CREATE ACCOUNT" to "BUAT AKUN",
        "WHY SIGN IN" to "MENGAPA MASUK",
        "Your e-tickets in one place" to "E-ticket tersimpan di satu tempat",
        "Open them at the entrance, even offline." to "Buka kapan saja di pintu studio, bahkan saat offline.",
        "Save films and get reminders" to "Simpan film & dapatkan pengingat",
        "Know when a saved film starts showing." to "Ketahui saat film favoritmu mulai tayang.",
        "Earn points on every booking" to "Kumpulkan poin setiap pemesanan",
        "Redeem points for tickets and snacks." to "Tukarkan poin untuk tiket & camilan bioskop.",
        "OR CONTINUE WITH" to "ATAU LANJUTKAN DENGAN",
        "GENERAL" to "UMUM",
        "Preferred city" to "Kota pilihan",
        "Help & support" to "Bantuan & dukungan",
        "Terms & privacy" to "Syarat & privasi",
        "NEW MEMBER • SINCE 2026" to "MEMBER BARU • SINCE 2026",
        "Tickets booked" to "Tiket dipesan",
        "Points" to "Poin",
        "NEXT SCREENING" to "JADWAL BERIKUTNYA",
        "VIEW E-TICKET  >" to "LIHAT E-TIKET  >",
        "ACCOUNT" to "AKUN",
        "My tickets" to "Tiket saya",
        "Payment methods" to "Metode pembayaran",
        "Notifications" to "Notifikasi",
        "PREFERENCES" to "PREFERENSI",
        "Check App Update" to "Cek Update Aplikasi",
        "Log out" to "Keluar",

        // Login, Create Account, Edit Profile, City Picker, Update & Confirm BottomSheets
        "MEMBER ACCESS" to "AKSES MEMBER",
        "Sign In to Your Account" to "Masuk ke Akun Kamu",
        "Sign in with your Google account to book faster, earn reward points, and view your tickets." to "Masuk dengan akun Google atau Apple untuk pesan lebih cepat & lihat tiket kamu.",
        "Continue with Google" to "Lanjutkan dengan Google",
        "Continue with Apple" to "Lanjutkan dengan Apple",
        "or sign in with email" to "atau masuk dengan email",
        "Email address" to "Alamat email",
        "Password" to "Kata sandi",
        "Don't have an account? " to "Belum punya akun? ",
        "Sign up here" to "Daftar di sini",
        "By continuing, you agree to the Terms of Service & Privacy Policy" to "Dengan melanjutkan, kamu menyetujui Syarat Layanan & Kebijakan Privasi",

        "CREATE NEW MEMBER" to "DAFTAR MEMBER BARU",
        "Create New Account" to "Buat Akun Baru",
        "Sign up to book tickets easily, earn loyalty points, and get movie promos." to "Daftar untuk nikmati kemudahan booking tiket, kumpulkan poin loyalty, dan dapatkan promo film.",
        "Sign up with Google" to "Daftar dengan Google",
        "Sign up with Apple" to "Daftar dengan Apple",
        "or sign up manually" to "atau daftar manual",
        "Full Name / Nickname" to "Nama Lengkap / Nickname",
        "Email Address" to "Alamat Email",
        "Password" to "Kata Sandi (Password)",
        "CREATE ACCOUNT NOW" to "DAFTAR SEKARANG",
        "Already have an account? " to "Sudah punya akun? ",
        "Sign in here" to "Masuk di sini",

        "Edit Profile" to "Edit Profil",
        "Change profile photo, nickname & account details" to "Ubah foto profil, nickname & informasi akunmu",
        "Tap photo to change" to "Ketuk foto untuk mengganti",
        "Nickname / Display Name" to "Nickname / Nama Tampilan",
        "Member Status / Bio" to "Status / Bio Member",
        "SAVE PROFILE" to "SIMPAN PROFIL",

        "Select City in Indonesia" to "Pilih Kota di Indonesia",
        "Show cinemas & schedules for your chosen location" to "Menampilkan bioskop & jadwal sesuai lokasi pilihanmu",
        "Scan My Location (GPS)" to "Scan Lokasi Sekitar Saya (GPS)",
        "Search city (Yogyakarta, Solo, Jakarta...)" to "Cari kota (Yogyakarta, Solo, Jakarta...)",

        "Choose Payment Method" to "Pilih Metode Pembayaran",
        "E-Wallet (OVO, GoPay, DANA, ShopeePay, QRIS) & Bank Transfer" to "E-Wallet (OVO, GoPay, DANA, ShopeePay, QRIS) & Transfer Bank",

        "CONFIRMATION" to "KONFIRMASI",
        "SIGN OUT" to "KELUAR AKUN",
        "Tap pencil icon to complete email" to "Ketuk ikon pensil untuk lengkapi email",
        "Are you sure you want to log out?" to "Yakin ingin keluar dari akun kamu?",
        "YES, CONTINUE" to "YA, LANJUTKAN",
        "CANCEL" to "BATAL",
        "Choose Option" to "Pilih Opsi",

        "Update Available!" to "Update Tersedia!",
        "The latest version is ready to download and will install over the app." to "Versi terbaru sudah siap diunduh dan langsung menimpa aplikasi lama.",
        "Current Version" to "Versi Sekarang",
        "Latest Version" to "Versi Terbaru",
        "WHAT'S NEW" to "YANG BARU",
        "Login data, watchlist & tickets stay safe without uninstalling." to "Data login, watchlist & tiket kamu tetap aman tanpa uninstall.",
        "UPDATE NOW" to "UPDATE SEKARANG",
        "LATER" to "NANTI SAJA",
        "Preparing download..." to "Menyiapkan unduhan..."
    )

    private val enToIdMap: Map<String, String> = pairs.associate { it.first to it.second }
    private val idToEnMap: Map<String, String> = pairs.associate { it.second to it.first }

    // Regex patterns for dynamic texts
    private val rCinemasEn = Regex("^(\\d+) Cinemas$")
    private val rCinemasId = Regex("^(\\d+) Bioskop$")
    private val rAvailEn = Regex("^(\\d+) cinemas available$")
    private val rAvailId = Regex("^(\\d+) bioskop tersedia$")
    private val rNearEn = Regex("^(\\d+) Cinemas in (.+)$")
    private val rNearId = Regex("^(\\d+) Bioskop di (.+)$")
    private val rExploreEn = Regex("^Explore (\\d+) Cinemas in (.+)$")
    private val rExploreId = Regex("^Explore (\\d+) Bioskop di (.+)$")
    private val rExploreSimpleId = Regex("^Explore Bioskop di (.+)$")
    private val rExploreSimpleEn = Regex("^Explore Cinemas in (.+)$")
    private val rWelcomeEn = Regex("^Welcome back, (.+)$")
    private val rWelcomeId = Regex("^Selamat datang, (.+)$")
    private val rReleaseEn = Regex("^Release: (.+)$")
    private val rReleaseId = Regex("^Rilis: (.+)$")
    private val rItemsSelEn = Regex("^(\\d+) items selected$")
    private val rItemsSelId = Regex("^(\\d+) item dipilih$")
    private val rPickupEn = Regex("^Pick up at (.+)$")
    private val rPickupId = Regex("^Ambil di (.+)$")
    private val rSeatsOf8En = Regex("^(\\d+) of 8 seats • Standard$")
    private val rSeatsOf8Id = Regex("^(\\d+) dari 8 kursi • Standar$")
    private val rPriceAdultEn = Regex("^(.+)/adult$")
    private val rPriceAdultId = Regex("^(.+)/dewasa$")
    private val rSeatsCountEn = Regex("^(.+) • (\\d+) tickets$")
    private val rSeatsCountId = Regex("^(.+) • (\\d+) tiket$")
    private val rResultsEn = Regex("^Results: \"(.+)\"$")
    private val rResultsId = Regex("^Hasil: \"(.+)\"$")
    private val rSeatSelectedEn = Regex("^(\\d+) seats selected$")
    private val rSeatSelectedId = Regex("^(\\d+) kursi dipilih$")
    private val rTodayEn = Regex("^Today • (.+)$")
    private val rTodayId = Regex("^Hari Ini • (.+)$")

    fun translateText(raw: String): String {
        if (raw.isEmpty()) return raw
        if (isId()) {
            enToIdMap[raw]?.let { return it }
            rCinemasEn.matchEntire(raw)?.let { return "${it.groupValues[1]} Bioskop" }
            rAvailEn.matchEntire(raw)?.let { return "${it.groupValues[1]} bioskop tersedia" }
            rNearEn.matchEntire(raw)?.let { return "${it.groupValues[1]} Bioskop di ${it.groupValues[2]}" }
            rExploreEn.matchEntire(raw)?.let { return "Explore ${it.groupValues[1]} Bioskop di ${it.groupValues[2]}" }
            rExploreSimpleEn.matchEntire(raw)?.let { return "Explore Bioskop di ${it.groupValues[1]}" }
            rWelcomeEn.matchEntire(raw)?.let { return "Selamat datang, ${it.groupValues[1]}" }
            rReleaseEn.matchEntire(raw)?.let { return "Rilis: ${it.groupValues[1]}" }
            rItemsSelEn.matchEntire(raw)?.let { return "${it.groupValues[1]} item dipilih" }
            rPickupEn.matchEntire(raw)?.let { return "Ambil di ${it.groupValues[1]}" }
            rSeatsOf8En.matchEntire(raw)?.let { return "${it.groupValues[1]} dari 8 kursi • Standar" }
            rPriceAdultEn.matchEntire(raw)?.let { return "${it.groupValues[1]}/dewasa" }
            rSeatsCountEn.matchEntire(raw)?.let { return "${it.groupValues[1]} • ${it.groupValues[2]} tiket" }
            rResultsEn.matchEntire(raw)?.let { return "Hasil: \"${it.groupValues[1]}\"" }
            rSeatSelectedEn.matchEntire(raw)?.let { return "${it.groupValues[1]} kursi dipilih" }
            rTodayEn.matchEntire(raw)?.let { return "Hari Ini • ${it.groupValues[1]}" }
            return raw
        } else {
            idToEnMap[raw]?.let { return it }
            rCinemasId.matchEntire(raw)?.let { return "${it.groupValues[1]} Cinemas" }
            rAvailId.matchEntire(raw)?.let { return "${it.groupValues[1]} cinemas available" }
            rNearId.matchEntire(raw)?.let { return "${it.groupValues[1]} Cinemas in ${it.groupValues[2]}" }
            rExploreId.matchEntire(raw)?.let { return "Explore ${it.groupValues[1]} Cinemas in ${it.groupValues[2]}" }
            rExploreSimpleId.matchEntire(raw)?.let { return "Explore Cinemas in ${it.groupValues[1]}" }
            rWelcomeId.matchEntire(raw)?.let { return "Welcome back, ${it.groupValues[1]}" }
            rReleaseId.matchEntire(raw)?.let { return "Release: ${it.groupValues[1]}" }
            rItemsSelId.matchEntire(raw)?.let { return "${it.groupValues[1]} items selected" }
            rPickupId.matchEntire(raw)?.let { return "Pick up at ${it.groupValues[1]}" }
            rSeatsOf8Id.matchEntire(raw)?.let { return "${it.groupValues[1]} of 8 seats • Standard" }
            rPriceAdultId.matchEntire(raw)?.let { return "${it.groupValues[1]}/adult" }
            rSeatsCountId.matchEntire(raw)?.let { return "${it.groupValues[1]} • ${it.groupValues[2]} tickets" }
            rResultsId.matchEntire(raw)?.let { return "Results: \"${it.groupValues[1]}\"" }
            rSeatSelectedId.matchEntire(raw)?.let { return "${it.groupValues[1]} seats selected" }
            rTodayId.matchEntire(raw)?.let { return "Today • ${it.groupValues[1]}" }
            return raw
        }
    }

    /** Rekursif terjemahkan seluruh tampilan (TextView, Button, hint TextInputLayout). */
    fun applyToView(root: View?) {
        if (root == null) return
        if (root is TextInputLayout) {
            val h = root.hint?.toString()
            if (!h.isNullOrEmpty()) {
                val translated = translateText(h)
                if (translated != h) root.hint = translated
            }
        }
        if (root is TextView) {
            if (root !is EditText) {
                val txt = root.text?.toString()
                if (!txt.isNullOrEmpty() && txt != "ID" && txt != "ENG") {
                    val translated = translateText(txt)
                    if (translated != txt) root.text = translated
                }
            }
            val hint = root.hint?.toString()
            if (!hint.isNullOrEmpty()) {
                val translatedHint = translateText(hint)
                if (translatedHint != hint) root.hint = translatedHint
            }
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                applyToView(root.getChildAt(i))
            }
        }
    }
}
