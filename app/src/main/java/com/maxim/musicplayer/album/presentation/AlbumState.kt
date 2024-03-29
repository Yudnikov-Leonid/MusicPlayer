package com.maxim.musicplayer.album.presentation

import com.maxim.musicplayer.albumList.presentation.AlbumUi

interface AlbumState {
    fun show(
        adapter: AlbumAdapter
    )

    data class Base(private val albumUi: AlbumUi, private val position: Int) : AlbumState {
        override fun show(adapter: AlbumAdapter) {
            albumUi.updateAdapter(adapter, position)
        }
    }
}