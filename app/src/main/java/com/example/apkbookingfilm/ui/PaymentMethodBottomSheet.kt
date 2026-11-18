package com.example.apkbookingfilm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.BottomSheetPaymentMethodsBinding
import com.example.apkbookingfilm.databinding.ItemPaymentMethodBinding
import com.example.apkbookingfilm.model.PaymentMethod
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PaymentMethodBottomSheet(
    private val onSelected: (PaymentMethod) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPaymentMethodsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPaymentMethodsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MovieRepository(requireContext())

        val selectedId = repository.getSelectedPaymentMethod().id
        binding.rvPaymentMethods.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPaymentMethods.adapter = Adapter(MovieRepository.paymentMethods, selectedId) { method ->
            repository.setSelectedPaymentMethod(method.id)
            onSelected(method)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class Adapter(
        private val list: List<PaymentMethod>,
        private val selectedId: String,
        private val onClick: (PaymentMethod) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemPaymentMethodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(list[position])
        }

        inner class VH(private val b: ItemPaymentMethodBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(method: PaymentMethod) {
                val active = method.id == selectedId
                b.ivPaymentLogo.setImageResource(method.logoResId)
                b.tvPaymentName.text = method.name
                b.tvPaymentMeta.text = "${method.type} • ${method.accountNumber}"
                b.ivPaymentSelected.visibility = if (active) View.VISIBLE else View.GONE
                b.root.setBackgroundResource(if (active) R.drawable.bg_peach_card else R.drawable.bg_chip_unselected)
                b.root.setOnClickListener { onClick(method) }
            }
        }
    }

    companion object {
        const val TAG = "PaymentMethodBottomSheet"
    }
}
