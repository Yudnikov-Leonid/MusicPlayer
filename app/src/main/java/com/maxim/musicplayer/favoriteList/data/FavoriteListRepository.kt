package com.maxim.musicplayer.favoriteList.data

import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.maxim.musicplayer.audioList.domain.AudioDomain
import com.maxim.musicplayer.core.presentation.Reload

interface FavoriteListRepository : FavoritesActions {
    fun init(reload: Reload, owner: LifecycleOwner)
    fun data(): List<AudioDomain>
    suspend fun singleDataIds(): List<Long>

    class Base(private val dao: FavoriteDao) : FavoriteListRepository {
        private lateinit var livedata: LiveData<List<AudioRoom>>
        private val data = mutableListOf<AudioDomain>()
        private val reloads = mutableListOf<Reload>()

        override fun init(reload: Reload, owner: LifecycleOwner) {
            livedata = dao.favoriteTracksLiveData()
            if (!reloads.contains(reload))
                livedata.observe(owner) { list ->
                    synchronized(lock) {
                        data.clear()
                        data.addAll(list.sortedBy { it.title }.map {
                            AudioDomain.Favorite(
                                it.id, it.title, it.artist, it.duration, it.album, it.artUri, it.uri
                            )
                        })
                        reload.reload()
                    }
                }
        }

        override fun data(): List<AudioDomain> = synchronized(lock) {
            val newList = ArrayList(data)
            newList.add(0, AudioDomain.Count(data.size))
            return newList
        }

        override suspend fun singleDataIds() =
            dao.favoriteTracks().map { it.id }

        override suspend fun addToFavorite(
            id: Long,
            title: String,
            artist: String,
            duration: Long,
            album: String,
            artUri: Uri,
            uri: Uri
        ) {
            dao.insert(AudioRoom(id, title, artist, duration, album, artUri, uri))
        }

        override suspend fun removeFromFavorites(id: Long) {
            dao.removeTrack(id)
        }

        companion object {
            private val lock = Object()
        }
    }
}

interface FavoritesActions {
    suspend fun addToFavorite(
        id: Long,
        title: String,
        artist: String,
        duration: Long,
        album: String,
        artUri: Uri,
        uri: Uri
    )

    suspend fun removeFromFavorites(id: Long)
}