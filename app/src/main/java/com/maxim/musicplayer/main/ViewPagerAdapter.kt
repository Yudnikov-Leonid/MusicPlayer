package com.maxim.musicplayer.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.maxim.musicplayer.audioList.presentation.AudioListFragment
import com.maxim.musicplayer.favoriteList.presentation.FavoriteListFragment

class ViewPagerAdapter(fragment: FragmentActivity): FragmentStateAdapter(fragment) {
    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AudioListFragment()
            else -> FavoriteListFragment()
        }
    }
}