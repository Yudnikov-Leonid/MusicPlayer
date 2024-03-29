package com.maxim.musicplayer.downBar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.maxim.musicplayer.R
import com.maxim.musicplayer.audioList.presentation.ArtImageView
import com.maxim.musicplayer.core.App

class DownBarView : FrameLayout {
    //region constructors
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
    //endregion

    private lateinit var viewModel: DownBarViewModel

    init {
        inflate(context, R.layout.down_bar, this)
        visibility = View.GONE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel = (context.applicationContext as App).viewModel(DownBarViewModel::class.java)
        viewModel.init()
        val imageView = findViewById<ArtImageView>(R.id.artImageView)
        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val artistTextView = findViewById<TextView>(R.id.artistTextView)
        val playButton = findViewById<ImageButton>(R.id.playButton)
        viewModel.observe(findViewTreeLifecycleOwner()!!) {
            visibility = View.VISIBLE
            it.show(this, imageView, titleTextView, artistTextView, playButton)
        }
        findViewById<View>(R.id.downBarRoot).setOnClickListener {
            viewModel.open()
        }
        playButton.setOnClickListener {
            viewModel.play()
        }
    }
}