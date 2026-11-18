package com.example.apkbookingfilm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.databinding.ItemCastBinding
import com.example.apkbookingfilm.model.CastMember
import com.example.apkbookingfilm.util.TmdbImageLoader

class CastAdapter(private var items: List<CastMember>) :
    RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    fun updateList(newItems: List<CastMember>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val binding = ItemCastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CastViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class CastViewHolder(private val binding: ItemCastBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(cast: CastMember) {
            TmdbImageLoader.loadCast(binding.ivCastPhoto, cast)
            binding.tvCastName.text = cast.name
            binding.tvCastCharacter.text = cast.character
        }
    }
}
