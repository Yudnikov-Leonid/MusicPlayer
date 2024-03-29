package com.maxim.musicplayer.order.presentation

import com.maxim.musicplayer.audioList.presentation.AudioUi

interface OrderState {
    fun show(adapter: OrderAdapter)

    data class Base(private val list: List<AudioUi>, private val actualPosition: Int): OrderState {
        override fun show(adapter: OrderAdapter) {
            adapter.update(list, actualPosition)
        }
    }
}