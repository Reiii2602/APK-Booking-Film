package com.example.apkbookingfilm.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.CinemaAdapter
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.FragmentTheatersBinding
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.util.AppLanguage
import com.example.apkbookingfilm.util.AppToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TheatersFragment : Fragment() {

    private var _binding: FragmentTheatersBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    private var activeFormatFilter: String = "ALL"

    private val onCityChangedListener = {
        if (_binding != null) {
            refreshCinemasAndMap()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scanGpsLocation()
        } else {
            com.example.apkbookingfilm.util.AppToast.show(requireContext(), "Izin lokasi ditolak. Pilih kota manual.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTheatersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MovieRepository(requireContext())
        MovieRepository.addOnCityChangedListener(onCityChangedListener)

        binding.rvTheaters.layoutManager = LinearLayoutManager(requireContext())
        setupMapWebView(binding.webTheaterMap)
        refreshCinemasAndMap()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) refreshCinemasAndMap()
    }

    private fun refreshCinemasAndMap() {
        val city = repository.getSelectedCity()
        val query = binding.etSearchTheater.text?.toString()?.trim().orEmpty()
        val cityCinemas = repository.getCinemasForCity(city)

        binding.tvCityTheaters.text = city
        binding.tvTheatersNearHeader.text = AppLanguage.tr(
            "${cityCinemas.size} Cinemas in $city",
            "${cityCinemas.size} Bioskop di $city"
        )

        val filtered = cityCinemas.filter { c ->
            val matchQuery = query.isEmpty() ||
                c.name.contains(query, ignoreCase = true) ||
                c.location.contains(query, ignoreCase = true)
            val matchFormat = when (activeFormatFilter) {
                "IMAX" -> c.formats.any { it.contains("IMAX", ignoreCase = true) }
                "DOLBY" -> c.formats.any { it.contains("DOLBY", ignoreCase = true) }
                "4DX" -> c.formats.any { it.contains("4DX", ignoreCase = true) || it.contains("PREMIERE", ignoreCase = true) }
                else -> true
            }
            matchQuery && matchFormat
        }

        binding.rvTheaters.adapter = CinemaAdapter(filtered) { _, _ ->
            val firstMovie = repository.movies.firstOrNull()
            val openDetail = {
                val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
                    putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, firstMovie?.id ?: "dune-2")
                }
                startActivity(intent)
            }
            if (!repository.isLoggedIn()) {
                AppToast.show(
                    requireContext(),
                    AppLanguage.tr("Please sign in first to book tickets & choose seats", "Silakan login terlebih dahulu untuk memesan tiket & memilih kursi")
                )
                LoginBottomSheet(
                    onLoginSuccess = { openDetail() },
                    onSwitchToRegister = {
                        CreateAccountBottomSheet(
                            onRegisterSuccess = { openDetail() },
                            onSwitchToLogin = {
                                LoginBottomSheet(onLoginSuccess = { openDetail() })
                                    .show(parentFragmentManager, LoginBottomSheet.TAG)
                            }
                        ).show(parentFragmentManager, CreateAccountBottomSheet.TAG)
                    }
                ).show(parentFragmentManager, LoginBottomSheet.TAG)
                return@CinemaAdapter
            }
            openDetail()
        }

        renderMapMarkers(binding.webTheaterMap, city, cityCinemas)
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupMapWebView(webView: WebView) {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = "CinemaBookingAndroid/1.0 (https://github.com/Reiii2602/APK-Booking-Film)"
        webView.webViewClient = WebViewClient()
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        // Izinkan interaksi drag/pan peta tanpa dicegat oleh NestedScrollView induk
        webView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_MOVE -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    private fun renderMapMarkers(webView: WebView, city: String, cinemas: List<Cinema>) {
        val markersJs = cinemas.joinToString(",") { c ->
            val safeName = c.name.replace("'", "\\'")
            val safeLoc = c.location.replace("'", "\\'")
            val safeDist = c.distance.replace("'", "\\'")
            "{lat:${c.lat},lng:${c.lng},name:'$safeName',loc:'$safeLoc',dist:'$safeDist'}"
        }
        val safeCity = city.replace("'", "\\'")

        // 100% self-contained interactive city map (Canvas + pan/zoom + clickable cinema pins)
        // Tidak bergantung tile server eksternal sehingga bebas dari blokir 418 / watermark API KEY.
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
              <style>
                html, body { margin:0; padding:0; width:100%; height:100%; overflow:hidden; background:#F5EFEA; font-family:-apple-system,Roboto,sans-serif; user-select:none; }
                canvas { width:100%; height:100%; display:block; touch-action:none; }
                #cityBadge { position:absolute; top:10px; left:10px; background:rgba(255,255,255,0.95); color:#1D1917; font-size:11px; font-weight:700; padding:5px 10px; border-radius:14px; border:1px solid #E8D4C8; box-shadow:0 2px 6px rgba(0,0,0,0.08); pointer-events:none; }
                #pinPopup { display:none; position:absolute; top:10px; right:10px; max-width:58%; background:#781D2E; color:#FFF; font-size:11px; padding:6px 10px; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.18); pointer-events:none; }
                #zoomBox { position:absolute; bottom:12px; left:12px; display:flex; flex-direction:row; gap:8px; z-index:20; }
                .zbtn { width:34px; height:34px; border-radius:50%; background:#FFFFFF; border:1.5px solid #E8D4C8; box-shadow:0 2px 6px rgba(0,0,0,0.15); display:flex; align-items:center; justify-content:center; cursor:pointer; user-select:none; -webkit-tap-highlight-color:transparent; padding:0; margin:0; box-sizing:border-box; }
                .zbtn:active { background:#F5EFEA; transform:scale(0.92); }
                .zbtn svg { display:block; }
              </style>
            </head>
            <body>
              <canvas id="c"></canvas>
              <div id="cityBadge">$safeCity • ${cinemas.size} ${AppLanguage.tr("Cinemas", "Bioskop")}</div>
              <div id="pinPopup"></div>
              <div id="zoomBox">
                <div class="zbtn" onclick="changeZoom(1.25)" title="Perbesar">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#781D2E" stroke-width="3.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                </div>
                <div class="zbtn" onclick="changeZoom(0.8)" title="Perkecil">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#781D2E" stroke-width="3.2" stroke-linecap="round"><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                </div>
              </div>
              <script>
                var canvas = document.getElementById('c');
                var ctx = canvas.getContext('2d');
                var pts = [$markersJs];
                var dpr = window.devicePixelRatio || 1;
                var W = 0, H = 0;
                var panX = 0, panY = 0, zoom = 1.0, selectedIdx = 0;

                var minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
                pts.forEach(function(p) {
                  if (p.lat < minLat) minLat = p.lat;
                  if (p.lat > maxLat) maxLat = p.lat;
                  if (p.lng < minLng) minLng = p.lng;
                  if (p.lng > maxLng) maxLng = p.lng;
                });
                var spanLat = Math.max(maxLat - minLat, 0.03);
                var spanLng = Math.max(maxLng - minLng, 0.03);
                var cLat = (minLat + maxLat) / 2;
                var cLng = (minLng + maxLng) / 2;

                function resize() {
                  W = canvas.clientWidth; H = canvas.clientHeight;
                  canvas.width = W * dpr; canvas.height = H * dpr;
                  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
                  draw();
                }

                function toXY(lat, lng) {
                  var scale = Math.min(W * 0.62 / spanLng, H * 0.56 / spanLat) * zoom;
                  return {
                    x: W / 2 + (lng - cLng) * scale + panX,
                    y: H / 2 - (lat - cLat) * scale + panY
                  };
                }

                function draw() {
                  ctx.fillStyle = '#F5EFEA';
                  ctx.fillRect(0, 0, W, H);

                  var ox = panX % 60, oy = panY % 60;
                  // Green park blocks
                  ctx.fillStyle = '#E1EDE1';
                  for (var gx = -1; gx < 6; gx++) {
                    for (var gy = -1; gy < 4; gy++) {
                      if ((gx + gy) % 3 === 0) {
                        ctx.fillRect(gx * 90 + ox + 12, gy * 70 + oy + 10, 44, 28);
                      }
                    }
                  }

                  // River curve
                  ctx.strokeStyle = '#CFE3EE';
                  ctx.lineWidth = 10;
                  ctx.beginPath();
                  ctx.moveTo(-40, H * 0.72 + panY * 0.3);
                  ctx.bezierCurveTo(W * 0.35, H * 0.52 + panY * 0.3, W * 0.65, H * 0.85 + panY * 0.3, W + 40, H * 0.45 + panY * 0.3);
                  ctx.stroke();

                  // Secondary city street grid
                  ctx.strokeStyle = '#EAE0D8';
                  ctx.lineWidth = 1.5;
                  for (var x = -60 + (panX % 36); x < W + 60; x += 36) {
                    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke();
                  }
                  for (var y = -60 + (panY % 36); y < H + 60; y += 36) {
                    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke();
                  }

                  // Main arterial roads connecting cinemas
                  ctx.strokeStyle = '#FFFFFF';
                  ctx.lineWidth = 6;
                  ctx.beginPath();
                  pts.forEach(function(p, i) {
                    var xy = toXY(p.lat, p.lng);
                    if (i === 0) ctx.moveTo(xy.x, xy.y); else ctx.lineTo(xy.x, xy.y);
                  });
                  ctx.stroke();

                  ctx.strokeStyle = '#F3CFA4';
                  ctx.lineWidth = 3;
                  ctx.beginPath();
                  ctx.moveTo(0, H * 0.48 + panY * 0.5);
                  ctx.lineTo(W, H * 0.42 + panY * 0.5);
                  ctx.moveTo(W * 0.45 + panX * 0.5, 0);
                  ctx.lineTo(W * 0.52 + panX * 0.5, H);
                  ctx.stroke();

                  // Cinema pins
                  pts.forEach(function(p, idx) {
                    var xy = toXY(p.lat, p.lng);
                    var active = idx === selectedIdx;
                    p._x = xy.x; p._y = xy.y;

                    ctx.fillStyle = active ? 'rgba(120,29,46,0.22)' : 'rgba(120,29,46,0.12)';
                    ctx.beginPath(); ctx.arc(xy.x, xy.y, active ? 17 : 12, 0, Math.PI * 2); ctx.fill();

                    ctx.fillStyle = active ? '#781D2E' : '#9A273D';
                    ctx.strokeStyle = '#FFFFFF';
                    ctx.lineWidth = 2.2;
                    ctx.beginPath(); ctx.arc(xy.x, xy.y, active ? 9 : 7, 0, Math.PI * 2); ctx.fill(); ctx.stroke();

                    var label = p.name.replace(' Jogja','').replace(' Bandung','').replace(' Surabaya','').replace(' Semarang','').replace(' Malang','').replace(' Medan','').replace(' Makassar','').replace(' Palembang','').replace(' Balikpapan','');
                    ctx.font = (active ? 'bold 10px' : '600 9px') + ' sans-serif';
                    var tw = ctx.measureText(label).width;
                    ctx.fillStyle = active ? '#781D2E' : 'rgba(255,255,255,0.92)';
                    ctx.fillRect(xy.x - tw/2 - 5, xy.y - 24, tw + 10, 14);
                    ctx.fillStyle = active ? '#FFFFFF' : '#1D1917';
                    ctx.fillText(label, xy.x - tw/2, xy.y - 14);
                  });
                }

                function selectPin(idx) {
                  selectedIdx = idx;
                  var p = pts[idx];
                  var pop = document.getElementById('pinPopup');
                  if (p) {
                    pop.style.display = 'block';
                    pop.innerHTML = '<b>' + p.name + '</b> • ' + p.dist;
                  }
                  draw();
                }

                function changeZoom(factor) {
                  zoom = Math.max(0.5, Math.min(3.5, zoom * factor));
                  draw();
                }

                var dragging = false, sx = 0, sy = 0, moved = false;
                var pinchDist = 0;

                canvas.addEventListener('touchstart', function(e) {
                  if (e.touches.length === 1) {
                    dragging = true;
                    moved = false;
                    sx = e.touches[0].clientX;
                    sy = e.touches[0].clientY;
                  } else if (e.touches.length === 2) {
                    dragging = false;
                    pinchDist = Math.hypot(
                      e.touches[0].clientX - e.touches[1].clientX,
                      e.touches[0].clientY - e.touches[1].clientY
                    );
                  }
                }, {passive: false});

                canvas.addEventListener('touchmove', function(e) {
                  if (dragging && e.touches.length === 1) {
                    var dx = e.touches[0].clientX - sx;
                    var dy = e.touches[0].clientY - sy;
                    if (Math.abs(dx) > 2 || Math.abs(dy) > 2) moved = true;
                    panX += dx;
                    panY += dy;
                    sx = e.touches[0].clientX;
                    sy = e.touches[0].clientY;
                    draw();
                    e.preventDefault();
                  } else if (e.touches.length === 2) {
                    var dist = Math.hypot(
                      e.touches[0].clientX - e.touches[1].clientX,
                      e.touches[0].clientY - e.touches[1].clientY
                    );
                    if (pinchDist > 0) {
                      var factor = dist / pinchDist;
                      zoom = Math.max(0.5, Math.min(3.5, zoom * factor));
                      draw();
                    }
                    pinchDist = dist;
                    e.preventDefault();
                  }
                }, {passive: false});

                canvas.addEventListener('touchend', function(e) {
                  if (e.touches.length < 2) pinchDist = 0;
                  if (e.touches.length === 0) {
                    dragging = false;
                    if (!moved && e.changedTouches.length === 1) {
                      var rect = canvas.getBoundingClientRect();
                      var tx = e.changedTouches[0].clientX - rect.left;
                      var ty = e.changedTouches[0].clientY - rect.top;
                      var best = -1, bestD = 32;
                      pts.forEach(function(p, i) {
                        var d = Math.hypot(p._x - tx, p._y - ty);
                        if (d < bestD) { bestD = d; best = i; }
                      });
                      if (best >= 0) selectPin(best);
                    }
                  }
                });

                // Support mouse pan for desktop & emulators
                var mousedown = false, mx = 0, my = 0;
                canvas.addEventListener('mousedown', function(e) {
                  mousedown = true; mx = e.clientX; my = e.clientY;
                });
                window.addEventListener('mousemove', function(e) {
                  if (mousedown) {
                    panX += e.clientX - mx;
                    panY += e.clientY - my;
                    mx = e.clientX;
                    my = e.clientY;
                    draw();
                  }
                });
                window.addEventListener('mouseup', function() { mousedown = false; });

                window.addEventListener('resize', resize);
                resize();
                if (pts.length > 0) selectPin(0);
              </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun openExternalMapsForCity() {
        val city = repository.getSelectedCity()
        val cinemas = repository.getCinemasForCity(city)
        val first = cinemas.firstOrNull()
        val lat = first?.lat ?: -7.7828
        val lng = first?.lng ?: 110.4012

        val options = listOf(
            "Buka Google Maps ($city)" to "Tampilkan seluruh bioskop sekitar di peta",
            "Pilih Bioskop Spesifik" to "Arahkan navigasi langsung ke salah satu bioskop"
        )
        ThemedOptionsBottomSheet(
            title = "Peta Bioskop Sekitar ($city)",
            subtitle = "Pilih cara membuka lokasi bioskop",
            options = options
        ) { which ->
            if (which == 0) {
                launchGeoUri("geo:$lat,$lng?q=" + Uri.encode("Bioskop $city"))
            } else {
                val cinemaOpts = cinemas.map { it.name to "${it.distance} • ${it.location}" }
                ThemedOptionsBottomSheet(
                    title = "Pilih Bioskop di $city",
                    subtitle = "Ketuk untuk membuka rute Google Maps",
                    options = cinemaOpts
                ) { idx ->
                    val c = cinemas[idx]
                    launchGeoUri("geo:${c.lat},${c.lng}?q=${c.lat},${c.lng}(${Uri.encode(c.name)})")
                }.show(parentFragmentManager, ThemedOptionsBottomSheet.TAG)
            }
        }.show(parentFragmentManager, ThemedOptionsBottomSheet.TAG)
    }

    private fun launchGeoUri(geoUri: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)))
        } catch (_: ActivityNotFoundException) {
            val city = repository.getSelectedCity()
            val url = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode("Bioskop $city")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanGpsLocation() {
        val ctx = context ?: return
        val hasFine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }

        com.example.apkbookingfilm.util.AppToast.show(ctx, "Mendeteksi lokasi sekitar...")
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        var best: Location? = null
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            try {
                if (!lm.isProviderEnabled(p)) continue
                val loc = lm.getLastKnownLocation(p)
                if (loc != null && (best == null || loc.accuracy < best.accuracy)) best = loc
            } catch (_: Exception) {}
        }

        if (best != null && (System.currentTimeMillis() - best.time) < 10 * 60 * 1000L) {
            onGpsFound(best)
            return
        }

        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            if (best != null) onGpsFound(best)
            else CityPickerDialog { refreshCinemasAndMap() }.show(parentFragmentManager, CityPickerDialog.TAG)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lm.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(ctx)) { loc ->
                if (_binding == null) return@getCurrentLocation
                if (loc != null) onGpsFound(loc)
                else if (best != null) onGpsFound(best)
                else com.example.apkbookingfilm.util.AppToast.show(ctx, "GPS tidak mendapat sinyal, pilih kota manual.")
            }
        } else {
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (_binding != null) onGpsFound(location)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            }, Looper.getMainLooper())
        }
    }

    private fun onGpsFound(loc: Location) {
        val city = repository.updateUserLocation(loc.latitude, loc.longitude)
        refreshCinemasAndMap()
        val nearest = repository.getCinemasForCity(city).firstOrNull()
        val msg = if (nearest != null) "Lokasi terdeteksi: $city (${nearest.name} - ${nearest.distance})" else "Lokasi: $city"
        com.example.apkbookingfilm.util.AppToast.show(requireContext(), msg, longDuration = true)
    }

    private fun setupListeners() {
        binding.btnTheatersBack.setOnClickListener {
            (activity as? MainActivity)?.selectTab(0)
        }

        binding.btnTheatersBookmark.setOnClickListener {
            (activity as? MainActivity)?.selectTab(1)
        }

        binding.layoutCityTheaters.setOnClickListener {
            CityPickerDialog { refreshCinemasAndMap() }.show(parentFragmentManager, CityPickerDialog.TAG)
        }

        binding.btnScanLocationTheaters.setOnClickListener {
            scanGpsLocation()
        }

        binding.btnOpenMapsNearby.setOnClickListener {
            openExternalMapsForCity()
        }

        binding.cardMapPreview.setOnClickListener {
            openExternalMapsForCity()
        }

        binding.etSearchTheater.addTextChangedListener {
            refreshCinemasAndMap()
        }

        binding.chipFilterNearby.setOnClickListener {
            activeFormatFilter = "ALL"
            updateFilterChipStyle(binding.chipFilterNearby)
            refreshCinemasAndMap()
        }

        binding.chipFilterImax.setOnClickListener {
            activeFormatFilter = "IMAX"
            updateFilterChipStyle(binding.chipFilterImax)
            refreshCinemasAndMap()
        }

        binding.chipFilterDolby.setOnClickListener {
            activeFormatFilter = "DOLBY"
            updateFilterChipStyle(binding.chipFilterDolby)
            refreshCinemasAndMap()
        }

        binding.chipFilterOpenNow.setOnClickListener {
            activeFormatFilter = "4DX"
            updateFilterChipStyle(binding.chipFilterOpenNow)
            refreshCinemasAndMap()
        }
    }

    private fun updateFilterChipStyle(selectedChip: View) {
        val chips = listOf(
            binding.chipFilterNearby,
            binding.chipFilterImax,
            binding.chipFilterDolby,
            binding.chipFilterOpenNow
        )
        chips.forEach {
            val sel = it == selectedChip
            it.setBackgroundResource(if (sel) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            it.setTextColor(requireContext().getColor(if (sel) R.color.white else R.color.chip_text))
            it.paint.isFakeBoldText = sel
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MovieRepository.removeOnCityChangedListener(onCityChangedListener)
        binding.webTheaterMap.destroy()
        _binding = null
    }
}
