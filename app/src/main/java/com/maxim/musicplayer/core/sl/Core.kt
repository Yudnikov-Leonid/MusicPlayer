package com.maxim.musicplayer.core.sl

import android.content.Context
import com.maxim.musicplayer.album.data.OpenAlbumStorage
import com.maxim.musicplayer.audioList.data.TracksCacheDataSource
import com.maxim.musicplayer.core.ProvideDownBarTrackCommunication
import com.maxim.musicplayer.core.ProvideManageOrder
import com.maxim.musicplayer.core.ProvideMediaService
import com.maxim.musicplayer.core.ProvidePlayerCommunication
import com.maxim.musicplayer.core.presentation.Navigation
import com.maxim.musicplayer.details.presentation.DetailsStorage
import com.maxim.musicplayer.favoriteList.data.FavoriteListRepository
import com.maxim.musicplayer.trackMore.presentation.MoreStorage

class Core(private val context: Context, private val provideInstances: ProvideInstances) {
    private val navigation = Navigation.Base()
    fun navigation() = navigation
    private val tracksProvider = provideInstances.tacksProvider()
    fun tracksCacheDataSource() = TracksCacheDataSource.Base(tracksProvider)
    fun tracksProvider() = tracksProvider
    fun manageOrder() = (context.applicationContext as ProvideManageOrder).manageOrder()
    fun provideMediaService() = (context.applicationContext as ProvideMediaService)
    fun downBarTrackCommunication() =
        (context.applicationContext as ProvideDownBarTrackCommunication).downBarTrackCommunication()
    fun playerCommunication() = (context.applicationContext as ProvidePlayerCommunication).playerCommunication()

    private lateinit var favoriteRepository: FavoriteListRepository
    fun favoriteRepository() = favoriteRepository

    fun database() = provideInstances.database()

    private val moreStorage = MoreStorage.Base()
    fun moreStorage() = moreStorage

    private val openAlbumStorage = OpenAlbumStorage.Base()
    fun openAlbumStorage() = openAlbumStorage

    fun init() {
        favoriteRepository = FavoriteListRepository.Base(database().dao())
    }

    private val detailsStorage = DetailsStorage.Base()
    fun detailsStorage() = detailsStorage
}