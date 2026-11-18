package com.example.apkbookingfilm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ItemFoodCardBinding
import com.example.apkbookingfilm.databinding.ItemFoodCartRowBinding
import com.example.apkbookingfilm.databinding.ItemSeeMoreArrowBinding
import com.example.apkbookingfilm.model.FoodItem
import com.example.apkbookingfilm.util.AppLanguage

enum class FoodViewType {
    HOME_CARD,
    CART_ROW
}

class FoodAdapter(
    private val items: List<FoodItem>,
    private val viewType: FoodViewType,
    private val repository: MovieRepository,
    private val onBuyClick: ((FoodItem) -> Unit)? = null,
    private val onSeeMoreClick: (() -> Unit)? = null,
    private val onQuantityChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_SEE_MORE = 1
    }

    override fun getItemCount(): Int {
        val showMore = viewType == FoodViewType.HOME_CARD && onSeeMoreClick != null && items.isNotEmpty()
        return if (showMore) items.size + 1 else items.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (viewType == FoodViewType.HOME_CARD && onSeeMoreClick != null && position == items.size) {
            TYPE_SEE_MORE
        } else {
            TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewTypeInt: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewTypeInt == TYPE_SEE_MORE) {
            return SeeMoreVH(ItemSeeMoreArrowBinding.inflate(inflater, parent, false))
        }
        return if (viewType == FoodViewType.HOME_CARD) {
            HomeCardVH(ItemFoodCardBinding.inflate(inflater, parent, false))
        } else {
            CartRowVH(ItemFoodCartRowBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SeeMoreVH) {
            holder.bind()
            AppLanguage.applyToView(holder.itemView)
            return
        }
        val item = items[position]
        if (holder is HomeCardVH) holder.bind(item)
        else if (holder is CartRowVH) holder.bind(item)
        AppLanguage.applyToView(holder.itemView)
    }

    inner class SeeMoreVH(private val b: ItemSeeMoreArrowBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind() {
            b.root.setOnClickListener { onSeeMoreClick?.invoke() }
        }
    }

    inner class HomeCardVH(private val b: ItemFoodCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FoodItem) {
            b.tvFoodName.text = item.name
            b.tvFoodDesc.text = item.description
            b.tvFoodPrice.text = MovieRepository.formatRupiah(item.price)
            if (item.badge.isNotEmpty()) {
                b.tvFoodBadge.visibility = View.VISIBLE
                b.tvFoodBadge.text = item.badge
            } else {
                b.tvFoodBadge.visibility = View.GONE
            }
            if (item.category == "Minuman") {
                b.ivFoodPrimary.setImageResource(R.drawable.ic_drink)
                b.ivFoodSecondary.visibility = View.GONE
            } else if (item.category == "Combo") {
                b.ivFoodPrimary.setImageResource(R.drawable.ic_popcorn)
                b.ivFoodSecondary.visibility = View.VISIBLE
                b.ivFoodSecondary.setImageResource(R.drawable.ic_drink)
            } else {
                b.ivFoodPrimary.setImageResource(R.drawable.ic_popcorn)
                b.ivFoodSecondary.visibility = View.GONE
            }
            b.btnFoodBuy.setOnClickListener { onBuyClick?.invoke(item) }
            b.root.setOnClickListener { onBuyClick?.invoke(item) }
        }
    }

    inner class CartRowVH(private val b: ItemFoodCartRowBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FoodItem) {
            val qty = repository.getFoodQuantity(item.id)
            b.tvCartFoodName.text = item.name
            b.tvCartFoodDesc.text = item.description
            b.tvCartFoodPrice.text = MovieRepository.formatRupiah(item.price)
            b.tvFoodQty.text = qty.toString()
            b.ivCartFoodIcon.setImageResource(
                if (item.category == "Minuman") R.drawable.ic_drink else R.drawable.ic_popcorn
            )

            b.btnFoodMinus.setOnClickListener {
                val cur = repository.getFoodQuantity(item.id)
                if (cur > 0) {
                    repository.setFoodQuantity(item.id, cur - 1)
                    b.tvFoodQty.text = (cur - 1).toString()
                    onQuantityChanged?.invoke()
                }
            }
            b.btnFoodPlus.setOnClickListener {
                val next = repository.getFoodQuantity(item.id) + 1
                repository.setFoodQuantity(item.id, next)
                b.tvFoodQty.text = next.toString()
                onQuantityChanged?.invoke()
            }
        }
    }
}
