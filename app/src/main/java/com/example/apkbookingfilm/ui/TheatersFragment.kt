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
            Toast.makeText(requireContext(), "Izin lokasi ditolak. Kamu bisa pilih kota secara manual.", Toast.LENGTH_SHORT).show()
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
        binding.tvTheatersNearHeader.text = "${cityCinemas.size} Bioskop di $city"

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

        binding.rvTheaters.adapter = CinemaAdapter(filtered) { cinema, _ ->
            val firstMovie = repository.movies.firstOrNull()
            val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
                putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, firstMovie?.id ?: "dune-2")
            }
            startActivity(intent)
        }

        renderMapMarkers(binding.webTheaterMap, city, cityCinemas)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMapWebView(webView: WebView) {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
    }

    private fun renderMapMarkers(webView: WebView, city: String, cinemas: List<Cinema>) {
        val center = cinemas.firstOrNull()
        val centerLat = center?.lat ?: -7.7828
        val centerLng = center?.lng ?: 110.4012

        val markersJs = cinemas.joinToString(",") { c ->
            val safeName = c.name.replace("'", "\\'")
            val safeLoc = c.location.replace("'", "\\'")
            "{lat:${c.lat},lng:${c.lng},name:'$safeName',loc:'$safeLoc'}"
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
              <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
              <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
              <style>
                html, body, #map { margin:0; padding:0; width:100%; height:100%; background:#F7EBE4; }
                .leaflet-control-attribution { font-size: 8px !important; }
              </style>
            </head>
            <body>
              <div id="map"></div>
              <script>
                var map = L.map('map', { zoomControl: false }).setView([$centerLat, $centerLng], 12);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                  maxZoom: 18,
                  attribution: 'OSM'
                }).addTo(map);
                var pts = [$markersJs];
                var bounds = [];
                pts.forEach(function(p) {
                  var m = L.circleMarker([p.lat, p.lng], {
                    radius: 8, color: '#581220', fillColor: '#781D2E', fillOpacity: 0.95, weight: 2
                  }).addTo(map);
                  m.bindPopup('<b>' + p.name + '</b><br/><small>' + p.loc + '</small>');
                  bounds.push([p.lat, p.lng]);
                });
                if (bounds.length > 1) { map.fitBounds(bounds, { padding: [24, 24] }); }
              </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
    }

    private fun openExternalMapsForCity() {
        val city = repository.getSelectedCity()
        val cinemas = repository.getCinemasForCity(city)
        val first = cinemas.firstOrNull()
        val lat = first?.lat ?: -7.7828
        val lng = first?.lng ?: 110.4012

        val options = arrayOf(
            "🗺️ Buka Google Maps (Semua Bioskop di $city)",
            "📍 Pilih Bioskop Spesifik untuk Navigasi"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Peta Bioskop Sekitar ($city)")
            .setItems(options) { _, which ->
                if (which == 0) {
                    launchGeoUri("geo:$lat,$lng?q=" + Uri.encode("Bioskop $city"))
                } else {
                    val names = cinemas.map { "${it.name} (${it.distance})" }.toTypedArray()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Pilih Bioskop")
                        .setItems(names) { _, idx ->
                            val c = cinemas[idx]
                            launchGeoUri("geo:${c.lat},${c.lng}?q=${c.lat},${c.lng}(${Uri.encode(c.name)})")
                        }
                        .show()
                }
            }
            .show()
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

        Toast.makeText(ctx, "Mendeteksi lokasi sekitar...", Toast.LENGTH_SHORT).show()
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
                else Toast.makeText(ctx, "GPS tidak mendapat sinyal, pilih kota manual.", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
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
