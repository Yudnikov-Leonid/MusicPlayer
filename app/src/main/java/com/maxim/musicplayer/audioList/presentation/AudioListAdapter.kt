package com.maxim.musicplayer.audioList.presentation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import com.maxim.musicplayer.R
import com.maxim.musicplayer.core.ProvideMediaService
import com.maxim.musicplayer.databinding.AudioLayoutBinding
import com.maxim.musicplayer.databinding.CountLayoutBinding
import com.maxim.musicplayer.media.MediaService

class AudioListAdapter(
    private val listener: Listener
) : RecyclerView.Adapter<AudioListAdapter.ItemViewHolder>() {
    private val list = mutableListOf<AudioUi>()
    private var actualPosition = -1

    abstract class ItemViewHolder(binding: ViewBinding) : ViewHolder(binding.root) {
        open fun bind(item: AudioUi, listener: Listener, position: Int, actualPosition: Int) = Unit
    }

    class BaseViewHolder(private val binding: AudioLayoutBinding) :
        ItemViewHolder(binding) {
        override fun bind(item: AudioUi, listener: Listener, position: Int, actualPosition: Int) {
            item.showTitle(binding.titleTextView)
            item.showDescription(binding.descriptionTextView)
            binding.artImageView.notifyArtChanged()
            item.showArt(binding.artImageView, false)
            itemView.setOnClickListener {
                listener.open(
                    item,
                    position - 1,
                    (binding.titleTextView.context.applicationContext as ProvideMediaService).mediaService()
                )
            }
            binding.moreButton.setOnClickListener {
                listener.more(item)
            }
            val color = ContextCompat.getColor(
                binding.artImageView.context,
                if (position == actualPosition) R.color.green else R.color.title_text
            )
            binding.titleTextView.setTextColor(color)
        }
    }

    class CountViewHolder(private val binding: CountLayoutBinding) : ItemViewHolder(binding) {
        override fun bind(item: AudioUi, listener: Listener, position: Int, actualPosition: Int) {
            item.showTitle(binding.countTextView)
        }
    }

    override fun getItemViewType(position: Int) =
        if (list[position] is AudioUi.Base || list[position] is AudioUi.Favorite) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return when (viewType) {
            0 -> BaseViewHolder(
                AudioLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> CountViewHolder(
                CountLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(list[position], listener, position, actualPosition)
    }

    fun update(newList: List<AudioUi>, actualPosition: Int) {
        notifyItemChanged(this.actualPosition)
        this.actualPosition = actualPosition + 1
        val diff = AudioDiffUtil(list, newList)
        val result = DiffUtil.calculateDiff(diff)
        list.clear()
        list.addAll(newList)
        result.dispatchUpdatesTo(this)
        notifyItemChanged(actualPosition + 1)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateAll(newList: List<AudioUi>, actualPosition: Int) {
        this.actualPosition = actualPosition + 1
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
        return
    }

    interface Listener {
        fun open(audioUi: AudioUi, position: Int, mediaService: MediaService)
        fun more(audioUi: AudioUi)
    }
}

class AudioDiffUtil(
    private val oldList: List<AudioUi>,
    private val newList: List<AudioUi>,
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].same(newList[newItemPosition])

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition] == newList[newItemPosition]
}