package com.maxim.musicplayer.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.core.content.ContextCompat
import com.maxim.musicplayer.R
import com.maxim.musicplayer.audioList.presentation.AudioUi
import com.maxim.musicplayer.core.ProvideDownBarTrackCommunication
import com.maxim.musicplayer.core.ProvideManageOrder
import com.maxim.musicplayer.core.ProvidePlayerCommunication
import com.maxim.musicplayer.downBar.DownBarTrackCommunication
import com.maxim.musicplayer.main.MainActivity
import com.maxim.musicplayer.main.MainActivity.Companion.OPEN_PLAYER_ACTION
import com.maxim.musicplayer.player.presentation.PlayerCommunication
import com.maxim.musicplayer.player.presentation.PlayerState
import java.lang.NullPointerException


interface MediaService : StartAudio, Playable {
    fun currentPosition(): Int
    fun seekTo(position: Int)
    fun setOnCompleteListener(action: () -> Unit)
    fun open(list: List<AudioUi>, audio: AudioUi, position: Int, orderType: OrderType)
    fun stop()
    fun isPlaying(): Boolean
    fun changeRandom()
    fun changeLoop()

    class Base : Service(), MediaService {
        private var mediaPlayer: MediaPlayer? = null
        private var actualUri: Uri? = null
        private lateinit var notificationManager: NotificationManager

        private val binder = MusicBinder()

        private var cachedTitle = ""
        private var cachedArtist = ""
        private var cachedIcon: Bitmap? = null

        private var isPlaying = false

        private lateinit var manageOrder: ManageOrder
        private lateinit var downBarTrackCommunication: DownBarTrackCommunication
        private lateinit var playerCommunication: PlayerCommunication

        inner class MusicBinder : Binder() {
            fun getService(): Base = this@Base
        }

        override fun onBind(intent: Intent?): IBinder {
            return binder
        }

        override fun currentPosition() = mediaPlayer?.currentPosition ?: 0

        override fun seekTo(position: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                mediaPlayer!!.seekTo(position.toLong(), MediaPlayer.SEEK_CLOSEST)
            else
                mediaPlayer!!.seekTo(position)
            notificationManager.notify(
                NOTIFICATION_ID,
                makeNotification(cachedTitle, cachedArtist, cachedIcon, !mediaPlayer!!.isPlaying)
            )
        }

        override fun setOnCompleteListener(action: () -> Unit) {
            mediaPlayer!!.setOnCompletionListener {
                action.invoke()
            }
        }

        override fun onCreate() {
            super.onCreate()
            manageOrder = (applicationContext as ProvideManageOrder).manageOrder()
            downBarTrackCommunication =
                (applicationContext as ProvideDownBarTrackCommunication).downBarTrackCommunication()
            playerCommunication =
                (applicationContext as ProvidePlayerCommunication).playerCommunication()
            notificationManager =
                this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createChannel()
            mediaSessionCompat = MediaSessionCompat(this, "tag")
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            return START_STICKY
        }

        override fun start(
            title: String,
            artist: String,
            uri: Uri,
            icon: Bitmap?,
            ignoreSame: Boolean
        ) {
            if (uri == Uri.EMPTY) return

            actualUri?.let {
                if (uri != actualUri || ignoreSame) {
                    mediaPlayer?.reset()
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer.create(this, uri)
                    mediaPlayer?.setOnCompletionListener { next() }
                }
            }
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, uri)
                mediaPlayer?.setOnCompletionListener { if (manageOrder.loopState() != LoopState.LoopTrack) next() }
            }
            try {
                mediaPlayer!!.start()
            } catch (e: NullPointerException) {
                next()
                return
            }

            actualUri = uri

            cachedTitle = title
            cachedArtist = artist
            cachedIcon = icon

            manageOrder.initLoop(mediaPlayer!!)
            startForeground(NOTIFICATION_ID, makeNotification(title, artist, icon, false))
        }

        override fun play() {
            isPlaying = !isPlaying
            val track = manageOrder.actualTrack()
            downBarTrackCommunication.setTrack(track, this, isPlaying)
            if (isPlaying) {
                track.start(this, contentResolver)
                playerCommunication.update(
                    PlayerState.Base(
                        track,
                        manageOrder.isRandom(),
                        manageOrder.loopState(),
                        false,
                        mediaPlayer!!.currentPosition,
                        manageOrder.swipeState()
                    )
                )
            } else {
                downBarTrackCommunication.stop()
                playerCommunication.update(
                    PlayerState.Base(
                        track,
                        manageOrder.isRandom(),
                        manageOrder.loopState(),
                        true,
                        mediaPlayer!!.currentPosition,
                        manageOrder.swipeState()
                    )
                )
                notificationManager.notify(
                    NOTIFICATION_ID,
                    makeNotification(cachedTitle, cachedArtist, cachedIcon, true)
                )
                mediaPlayer?.pause()
            }
        }

        override fun next() {
            if (manageOrder.canGoNext()) {
                if (manageOrder.loopState() == LoopState.LoopTrack)
                    manageOrder.changeLoop(mediaPlayer!!)

                isPlaying = true
                val track = manageOrder.next()
                track.start(this, contentResolver)
                playerCommunication.update(
                    PlayerState.Base(
                        track,
                        manageOrder.isRandom(),
                        manageOrder.loopState(), false, -1,
                        manageOrder.swipeState()
                    )
                )

                downBarTrackCommunication.setTrack(track, this, true)
            }
        }


        override fun previous() {
            if (currentPosition() < TIME_TO_PREVIOUS_MAKE_RESTART && manageOrder.canGoPrevious()) {
                if (manageOrder.loopState() == LoopState.LoopTrack)
                    manageOrder.changeLoop(mediaPlayer!!)

                isPlaying = true
                val track = manageOrder.previous()
                track.start(this, contentResolver)
                downBarTrackCommunication.setTrack(track, this, true)
                playerCommunication.update(
                    PlayerState.Base(track, manageOrder.isRandom(), manageOrder.loopState(), false, 0,
                        manageOrder.swipeState())
                )
            } else {
                val track = manageOrder.actualTrack()
                track.startAgain(this, contentResolver)
                playerCommunication.update(
                    PlayerState.Base(track, manageOrder.isRandom(), manageOrder.loopState(), false, 0,
                        manageOrder.swipeState())
                )
            }
        }

        override fun finish() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            playerCommunication.update(PlayerState.Finish)
            downBarTrackCommunication.close()
            notificationManager.notify(
                NOTIFICATION_ID,
                makeEmptyNotification()
            )
        }

        override fun open(
            list: List<AudioUi>,
            audio: AudioUi,
            position: Int,
            orderType: OrderType
        ) {
            isPlaying = true
            manageOrder.generate(list, position, orderType)
            audio.start(this, contentResolver)
        }

        override fun stop() {
            if (isPlaying) play()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        override fun isPlaying() = mediaPlayer?.isPlaying ?: false

        override fun changeRandom() {
            manageOrder.changeRandom()
            playerCommunication.update(
                PlayerState.Base(
                    manageOrder.actualTrack(),
                    manageOrder.isRandom(),
                    manageOrder.loopState(),
                    !(mediaPlayer?.isPlaying ?: false),
                    mediaPlayer?.currentPosition ?: 0,
                    manageOrder.swipeState()
                )
            )
        }

        override fun changeLoop() {
            manageOrder.changeLoop(mediaPlayer)
            playerCommunication.update(
                PlayerState.Base(
                    manageOrder.actualTrack(),
                    manageOrder.isRandom(),
                    manageOrder.loopState(),
                    !(mediaPlayer?.isPlaying ?: false),
                    mediaPlayer?.currentPosition ?: 0,
                    manageOrder.swipeState()
                )
            )
        }

        override fun onDestroy() {
            mediaPlayer?.reset()
            mediaPlayer?.release()
            super.onDestroy()
        }

        private lateinit var mediaSessionCompat: MediaSessionCompat

        private fun makeEmptyNotification(): Notification {
            return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground).build()
        }

        private fun makeNotification(
            title: String,
            text: String,
            icon: Bitmap?,
            isPause: Boolean,
        ): Notification {
            val pendingIntents = listOf(
                PREVIOUS_ACTION,
                PLAY_ACTION,
                NEXT_ACTION,
                STOP_ACTION
            ).map { action ->
                val intent =
                    Intent(applicationContext, NotificationActionsBroadcastReceiver::class.java)
                intent.action = action
                PendingIntent.getBroadcast(
                    applicationContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            val onClickIntent = Intent(this, MainActivity::class.java).apply {
                action = OPEN_PLAYER_ACTION
            }
            val onClickPendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                onClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val largeIcon = if (icon != null) icon
            else {
                val drawable =
                    ContextCompat.getDrawable(applicationContext, R.drawable.ic_launcher_background)
                val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                drawable?.setBounds(0, 0, canvas.width, canvas.height)
                drawable?.draw(canvas)
                bitmap
            }

            mediaSessionCompat.setMetadata(
                MediaMetadataCompat.Builder().putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    mediaPlayer!!.duration.toLong()
                ).build()
            )
            mediaSessionCompat.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .addCustomAction(PREVIOUS_ACTION, "Previous", R.drawable.previous_24)
                    .addCustomAction(NEXT_ACTION, "Next", R.drawable.next_24)
                    .addCustomAction(
                        PLAY_ACTION,
                        "Play",
                        if (isPause) R.drawable.play_24 else R.drawable.pause_24
                    )
                    .addCustomAction(STOP_ACTION, "Stop", R.drawable.close_24)
                    .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                    .setState(
                        if (isPause) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING,
                        mediaPlayer!!.currentPosition.toLong(),
                        if (isPause) 0f else 1f
                    ).build()
            )
            mediaSessionCompat.setCallback(object : MediaSessionCompat.Callback() {
                override fun onSeekTo(pos: Long) {
                    mediaPlayer?.seekTo(pos.toInt())
                    playerCommunication.update(
                        PlayerState.Base(
                            manageOrder.actualTrack(),
                            manageOrder.isRandom(),
                            manageOrder.loopState(),
                            isPause,
                            mediaPlayer!!.currentPosition,
                            manageOrder.swipeState()
                        )
                    )
                }

                override fun onCustomAction(action: String?, extras: Bundle?) {
                    super.onCustomAction(action, extras)
                    when (action) {
                        PREVIOUS_ACTION -> previous()
                        NEXT_ACTION -> next()
                        PLAY_ACTION -> play()
                        STOP_ACTION -> stop()
                    }
                }
            })

            return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(title)
                .setTicker(title)
                .setContentText(text)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(PRIORITY_MAX)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(R.drawable.previous_24, "Previous", pendingIntents[0])
                .addAction(
                    if (isPause) R.drawable.play_24 else R.drawable.pause_24,
                    "Play",
                    pendingIntents[1]
                )
                .addAction(R.drawable.next_24, "Next", pendingIntents[2])
                .addAction(R.drawable.close_24, "Stop", pendingIntents[3])
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSessionCompat.sessionToken)
                )
                .setContentIntent(onClickPendingIntent)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createChannel() {
            val notificationManager =
                this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(CHANNEL_ID, "Pomodoro", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Pomodoro time"
            notificationManager.createNotificationChannel(channel)
        }

        companion object {
            private const val NOTIFICATION_ID = 123456789
            private const val CHANNEL_ID = "Player"
            private const val TIME_TO_PREVIOUS_MAKE_RESTART = 2500
            const val PLAY_ACTION = "PLAY"
            const val NEXT_ACTION = "NEXT"
            const val PREVIOUS_ACTION = "PREVIOUS"
            const val STOP_ACTION = "STOP"
        }
    }
}