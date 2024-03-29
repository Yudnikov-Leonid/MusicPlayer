package com.maxim.musicplayer.core

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import com.maxim.musicplayer.core.sl.ClearViewModel
import com.maxim.musicplayer.core.sl.Core
import com.maxim.musicplayer.core.sl.ProvideInstances
import com.maxim.musicplayer.core.sl.ProvideViewModel
import com.maxim.musicplayer.core.sl.ViewModelFactory
import com.maxim.musicplayer.downBar.DownBarTrackCommunication
import com.maxim.musicplayer.media.ManageOrder
import com.maxim.musicplayer.media.MediaService
import com.maxim.musicplayer.player.presentation.PlayerCommunication

class App : Application(), ProvideViewModel, ProvideMediaService, ProvideManageOrder,
    ProvideDownBarTrackCommunication, ProvidePlayerCommunication {
    private lateinit var factory: ViewModelFactory
    private lateinit var manageOrder: ManageOrder

    val isMock = false

    override fun onCreate() {
        super.onCreate()
        val provideInstances =
            if (isMock) ProvideInstances.Mock(this) else ProvideInstances.Release(this)

        val core = Core(this, provideInstances)
        core.init()
        factory = ViewModelFactory.Empty
        val provideViewModel = ProvideViewModel.Base(core, object : ClearViewModel {
            override fun clear(clasz: Class<out ViewModel>) {
                factory.clear(clasz)
            }
        })
        factory = ViewModelFactory.Base(provideViewModel)
        manageOrder =
            ManageOrder.Base(
                provideInstances.simpleStorage(),
                provideInstances.shuffleOrder()
            )
    }

    private var isBound = false
    private var mediaService: MediaService.Base? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaService.Base.MusicBinder
            mediaService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    fun bind() {
        bindService(
            Intent(this, MediaService.Base::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun mediaService() = mediaService!!
    override fun manageOrder(): ManageOrder = manageOrder
    override fun <T : ViewModel> viewModel(clasz: Class<T>) = factory.viewModel(clasz)

    private val downBarTrackCommunication = DownBarTrackCommunication.Base()
    override fun downBarTrackCommunication() = downBarTrackCommunication
    private val playerCommunication = PlayerCommunication.Base()
    override fun playerCommunication() = playerCommunication
}

interface ProvideMediaService {
    fun mediaService(): MediaService
}

interface ProvideManageOrder {
    fun manageOrder(): ManageOrder
}

interface ProvideDownBarTrackCommunication {
    fun downBarTrackCommunication(): DownBarTrackCommunication
}

interface ProvidePlayerCommunication {
    fun playerCommunication(): PlayerCommunication
}