package com.maxim.musicplayer.audioList.domain

import com.maxim.musicplayer.audioList.data.AudioListRepository
import com.maxim.musicplayer.favoriteList.data.FavoriteListRepository

interface AudioListInteractor {
    suspend fun data(): List<AudioDomain>
    suspend fun cachedData(): List<AudioDomain>

    class Base(
        private val repository: AudioListRepository,
        private val favoriteRepository: FavoriteListRepository
    ) : AudioListInteractor {
        private val cache = mutableListOf<AudioDomain>()

        override suspend fun data(): List<AudioDomain> {
            val data = repository.data().toMutableList()
            val favoritesIds = favoriteRepository.singleDataIds()
            for (i in data.indices) {
                if (data[i].containsId(favoritesIds))
                    data[i] = data[i].changeFavorite()
            }
            cache.clear()
            cache.addAll(data)
            return cache
        }

        override suspend fun cachedData(): List<AudioDomain> {
            val favoritesIds = favoriteRepository.singleDataIds()
            for (i in cache.indices) {
                cache[i] = cache[i].toBase()
                if (cache[i].containsId(favoritesIds))
                    cache[i] = cache[i].changeFavorite()
            }
            return cache
        }
    }
}