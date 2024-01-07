package com.maxim.musicplayer.cope.presentation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

interface Screen {
    fun show(fragmentManager: FragmentManager, containerId: Int)

    abstract class Add(private val fragmentClass: Class<out Fragment>) : Screen {
        override fun show(fragmentManager: FragmentManager, containerId: Int) {
            fragmentManager.beginTransaction()
                .add(containerId, fragmentClass.getDeclaredConstructor().newInstance())
                .addToBackStack(fragmentClass.simpleName)
                .commit()
        }
    }

    abstract class AddSingleton(private val fragmentClass: Class<out Fragment>) : Screen {
        override fun show(fragmentManager: FragmentManager, containerId: Int) {
            val index = fragmentManager.backStackEntryCount - 1
            if ((index >= 0 && fragmentManager.getBackStackEntryAt(index).name != fragmentClass.simpleName) || index == -1) {
                fragmentManager.beginTransaction()
                    .add(containerId, fragmentClass.getDeclaredConstructor().newInstance())
                    .addToBackStack(fragmentClass.simpleName)
                    .commit()
            }
        }
    }

    abstract class Replace(private val fragmentClass: Class<out Fragment>) : Screen {
        override fun show(fragmentManager: FragmentManager, containerId: Int) {
            fragmentManager.beginTransaction()
                .replace(containerId, fragmentClass.getDeclaredConstructor().newInstance())
                .commit()
        }
    }

    object Pop : Screen {
        override fun show(fragmentManager: FragmentManager, containerId: Int) {
            fragmentManager.popBackStack()
        }
    }
}