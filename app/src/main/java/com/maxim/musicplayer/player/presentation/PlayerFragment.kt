package com.maxim.musicplayer.player.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import com.maxim.musicplayer.cope.presentation.BaseFragment
import com.maxim.musicplayer.cope.ProvideMediaService
import com.maxim.musicplayer.databinding.FragmentPlayerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerFragment : BaseFragment<FragmentPlayerBinding, PlayerViewModel>() {
    override fun viewModelClass() = PlayerViewModel::class.java
    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPlayerBinding.inflate(inflater, container, false)

    private var coroutineIsRunning = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.goBack()
            }
        }

        super.onViewCreated(view, savedInstanceState)

        viewModel.observe(this) {
            it.show(
                binding.artImageView,
                binding.playerTitleTextView,
                binding.artistTextView,
                binding.playButton,
                binding.randomOrderButton,
                binding.loopOrderButton,
                binding.seekBar,
                binding.actualTimeTextView,
                binding.durationTextView,
                binding.favoriteButton
            )
            it.finish(viewModel)
        }

        binding.favoriteButton.setOnClickListener {
            viewModel.saveToFavorites()
        }

        binding.playButton.setOnClickListener {
            viewModel.play()
        }

        binding.nextButton.setOnClickListener {
            viewModel.next()
        }

        binding.previousButton.setOnClickListener {
            viewModel.previous()
        }

        binding.loopOrderButton.setOnClickListener {
            viewModel.changeLoop()
        }

        binding.randomOrderButton.setOnClickListener {
            viewModel.changeRandom()
        }

        val mediaService = (requireContext().applicationContext as ProvideMediaService).mediaService()
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaService.seekTo(progress)
                    binding.actualTimeTextView.showTime(mediaService.currentPosition() / 1000)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        CoroutineScope(Dispatchers.Default).launch {
            delay(1300)
            while (coroutineIsRunning) {
                withContext(Dispatchers.Main) {
                    if (mediaService.isPlaying()) {
                        val currentPosition = mediaService.currentPosition()
                        binding.seekBar.progress = currentPosition
                        binding.actualTimeTextView.showTime(currentPosition / 1000)
                    }
                }
                delay(1000)
            }
        }

        viewModel.init(savedInstanceState == null)
    }

    override fun onDestroyView() {
        coroutineIsRunning = false
        super.onDestroyView()
    }
}