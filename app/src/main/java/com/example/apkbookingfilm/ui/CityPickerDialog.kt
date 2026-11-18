package com.example.apkbookingfilm.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.DialogCityPickerBinding
import com.example.apkbookingfilm.databinding.ItemCityBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CityPickerDialog(
    private val onCitySelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogCityPickerBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scanGpsLocation()
        } else {
            if (_binding != null) {
                binding.tvLocationStatus.visibility = View.VISIBLE
                binding.tvLocationStatus.text = "Izin lokasi ditolak. Pilih kota secara manual di bawah."
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCityPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MovieRepository(requireContext())

        val allCities = repository.getAvailableCities()
        var currentSelected = repository.getSelectedCity()

        val adapter = CityAdapter(allCities, currentSelected) { chosen ->
            repository.setSelectedCity(chosen)
            onCitySelected(chosen)
            dismiss()
        }

        binding.rvCities.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCities.adapter = adapter

        binding.etSearchCity.addTextChangedListener { text ->
            val q = text?.toString()?.trim().orEmpty()
            val filtered = if (q.isEmpty()) allCities else allCities.filter {
                it.contains(q, ignoreCase = true)
            }
            adapter.update(filtered, currentSelected)
        }

        binding.btnScanLocation.setOnClickListener {
            requestOrScanLocation()
        }
    }

    private fun requestOrScanLocation() {
        val ctx = context ?: return
        val hasFine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            scanGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanGpsLocation() {
        val ctx = context ?: return
        binding.tvLocationStatus.visibility = View.VISIBLE
        binding.tvLocationStatus.text = "Mendeteksi lokasi GPS sekitar..."
        binding.btnScanLocation.isEnabled = false

        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            onLocationFailed()
            return
        }

        // Cek lastKnownLocation dulu dari GPS / Network / Passive
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var bestLocation: Location? = null
        for (p in providers) {
            try {
                if (!lm.isProviderEnabled(p)) continue
                val loc = lm.getLastKnownLocation(p)
                if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                    bestLocation = loc
                }
            } catch (_: Exception) {}
        }

        if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 10 * 60 * 1000L) {
            applyDetectedLocation(bestLocation)
            return
        }

        val activeProvider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (activeProvider == null) {
            if (bestLocation != null) applyDetectedLocation(bestLocation) else onLocationFailed()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                lm.getCurrentLocation(activeProvider, null, ContextCompat.getMainExecutor(ctx)) { loc ->
                    if (_binding == null) return@getCurrentLocation
                    if (loc != null) applyDetectedLocation(loc)
                    else if (bestLocation != null) applyDetectedLocation(bestLocation)
                    else onLocationFailed()
                }
            } catch (_: Exception) {
                if (bestLocation != null) applyDetectedLocation(bestLocation) else onLocationFailed()
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(activeProvider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (_binding != null) applyDetectedLocation(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }, Looper.getMainLooper())
            } catch (_: Exception) {
                if (bestLocation != null) applyDetectedLocation(bestLocation) else onLocationFailed()
            }
        }
    }

    private fun applyDetectedLocation(loc: Location) {
        val detectedCity = repository.updateUserLocation(loc.latitude, loc.longitude)
        val nearest = repository.getCinemasForCity(detectedCity).firstOrNull()
        val distStr = nearest?.let { " • Terdekat: ${it.name} (${it.distance})" }.orEmpty()
        binding.btnScanLocation.isEnabled = true
        Toast.makeText(
            requireContext(),
            "Lokasi terdeteksi: $detectedCity$distStr",
            Toast.LENGTH_LONG
        ).show()
        onCitySelected(detectedCity)
        dismiss()
    }

    private fun onLocationFailed() {
        if (_binding == null) return
        binding.btnScanLocation.isEnabled = true
        binding.tvLocationStatus.visibility = View.VISIBLE
        binding.tvLocationStatus.text = "GPS belum mendapat sinyal. Pastikan GPS aktif atau pilih kota di daftar."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class CityAdapter(
        private var cities: List<String>,
        private var selectedCity: String,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<CityAdapter.VH>() {

        fun update(newList: List<String>, current: String) {
            cities = newList
            selectedCity = current
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = cities.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(cities[position])
        }

        inner class VH(private val b: ItemCityBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(city: String) {
                val count = repository.getCinemasForCity(city).size
                val isSelected = city.equals(selectedCity, ignoreCase = true)
                b.tvCityName.text = city
                b.tvCityCinemaCount.text = "$count bioskop (XXI, CGV, Cinépolis)"
                b.ivCitySelected.visibility = if (isSelected) View.VISIBLE else View.GONE
                b.root.setBackgroundResource(if (isSelected) R.drawable.bg_peach_card else R.drawable.bg_chip_unselected)
                b.root.setOnClickListener { onClick(city) }
            }
        }
    }

    companion object {
        const val TAG = "CityPickerDialog"
    }
}
