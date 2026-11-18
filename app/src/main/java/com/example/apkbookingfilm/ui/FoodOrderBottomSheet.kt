package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.FoodAdapter
import com.example.apkbookingfilm.adapter.FoodViewType
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.DialogFoodMenuBinding
import com.example.apkbookingfilm.model.FoodOrderItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FoodOrderBottomSheet(
    private val preselectFoodId: String? = null,
    private val onCartUpdated: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: DialogFoodMenuBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFoodMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MovieRepository(requireContext())

        if (preselectFoodId != null && repository.getFoodQuantity(preselectFoodId) == 0) {
            repository.addFoodItem(preselectFoodId)
        }

        val city = repository.getSelectedCity()
        val firstCinema = repository.getCinemasForCity(city).firstOrNull()?.name ?: "Bioskop"
        binding.tvFoodMenuSubtitle.text = "Ambil di $firstCinema ($city)"

        binding.rvFoodMenu.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFoodMenu.adapter = FoodAdapter(
            items = MovieRepository.foodMenu,
            viewType = FoodViewType.CART_ROW,
            repository = repository,
            onQuantityChanged = { updateSummary() }
        )

        updateSummary()

        binding.btnSaveFoodCart.setOnClickListener {
            onCartUpdated?.invoke()
            val count = repository.getFoodCartCount()
            if (count > 0) {
                Toast.makeText(
                    requireContext(),
                    "$count item camilan disimpan — akan ikut saat pilih kursi!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dismiss()
        }

        binding.btnCheckoutFoodOnly.setOnClickListener {
            val orders = repository.getFoodCartOrders()
            if (orders.isEmpty()) {
                Toast.makeText(requireContext(), "Pilih minimal 1 makanan atau minuman", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val cinema = repository.getCinemasForCity().firstOrNull() ?: repository.cinemas.first()
            val intent = Intent(requireContext(), PaymentActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_CINEMA, cinema)
                putExtra(PaymentActivity.EXTRA_FOOD_ONLY, true)
                putExtra(PaymentActivity.EXTRA_FOOD_ITEMS, ArrayList<FoodOrderItem>(orders))
            }
            startActivity(intent)
            dismiss()
        }
    }

    private fun updateSummary() {
        val count = repository.getFoodCartCount()
        val total = repository.getFoodCartTotal()
        binding.tvFoodCartSummary.text = "$count item dipilih"
        binding.tvFoodCartTotal.text = MovieRepository.formatRupiah(total)
        binding.btnCheckoutFoodOnly.isEnabled = count > 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FoodOrderBottomSheet"
    }
}
