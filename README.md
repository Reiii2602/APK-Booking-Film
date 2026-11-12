# Cinema Booking App (Android Kotlin)

Aplikasi Android Booking Tiket Bioskop modern berbasis **Kotlin** yang dirancang dengan presisi sesuai UI/UX Figma dengan integrasi live API dari **The Movie Database (TMDB)** dan bioskop lokal di Yogyakarta / Indonesia.

## 📱 Fitur Utama
1. **Live TMDB Movie Catalog:**
   - Menampilkan puluhan film terbaru secara real-time via API TMDB (Now Playing, Upcoming, dan Popular).
   - Mengambil poster (`w500`) dan backdrop banner (`w780`) dinamis menggunakan Glide dengan caching dan fallback gambar lokal.
   - Kategori umur otomatis (`SU`, `13+`, `17+`, `21+`) dan rating film.
2. **Bioskop Lokal Yogyakarta / Indonesia:**
   - **Ambarrukmo XXI** (Plaza Ambarrukmo Lt. 3, Jl. Laksda Adisucipto)
   - **Empire XXI Jogja** (Jl. Urip Sumoharjo No. 104)
   - **Jogja City XXI** (Jogja City Mall Lt. 2)
   - **CGV Pakuwon Mall Jogja** (Pakuwon Mall Lt. 2)
   - **Cinepolis Lippo Plaza Jogja** (Lippo Plaza Lt. 1)
   - Format: `PREMIERE`, `IMAX WITH LASER`, `DOLBY ATMOS`, `4DX`, `VELVET CLASS`.
3. **Pilih Kursi (Seat Selection):**
   - Indikator layar bioskop lengkung berpijar merah dengan label `SCREEN`.
   - Grid baris A s/d H dengan label baris ganda di sisi kiri dan kanan.
   - Status kursi: Available, Taken, dan Selected (warna merah marun `#781D2E`).
   - Chip pilihan kursi dinamis (`[E6 x]`, `[E7 x]`) dan total harga otomatis `Rp50.000`.
4. **Pembayaran & Upload Bukti Transfer:**
   - Countdown timer pembayaran (14:59).
   - Transfer Bank BCA (`0373 8812 4566` a.n. PT Cinema Booking Indonesia) dengan tombol salin cepat (Copy).
   - Dashed card untuk upload bukti transfer dengan indikator file terunggah (`transfer-proof-0611.jpg`).
5. **Autentikasi & Profil (Google & Apple):**
   - Modal Bottom Sheet otomatis saat membuka Profil jika belum login.
   - Dukungan tombol **Continue with Google** dan **Continue with Apple** lengkap dengan logo resmi.
   - Akun demo 1-tap: **Daffa Fadhul Rahman** (`daffasigma@gmail.com`).
   - Tampilan profil lengkap: Avatar inisial `DF`, statistik tiket & poin, jadwal tayang tiket berikutnya, dan preferensi kota `Yogyakarta`.
6. **E-Ticket:**
   - Tampilan tiket bioskop dengan barcode, info studio, tanggal, jam tayang, dan kursi.

## 🛠️ Tech Stack
- **Bahasa:** Kotlin
- **Min SDK:** 24 (Android 7.0)
- **Target SDK / Compile SDK:** 35 (Android 15)
- **UI:** ViewBinding, Material Components 3, ConstraintLayout, CoordinatorLayout
- **Image Loading:** Glide 4.16.0
- **Network / API:** The Movie Database (TMDB) API v3
- **Data Persistence:** SharedPreferences

## 🚀 Cara Menjalankan
1. Clone repositori:
   ```bash
   git clone https://github.com/Reiii2602/APK-Booking-Film.git
   ```
2. Buka folder proyek di **Android Studio**.
3. Sinkronkan Gradle (`Sync Project with Gradle Files`).
4. Jalankan pada emulator atau perangkat fisik Android:
   ```bash
   ./gradlew assembleDebug
   ```
5. File APK siap instal juga tersedia di root repositori: `APK_Booking_Film.apk`.
