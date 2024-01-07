package com.maxim.musicplayer.cope.presentation

interface Navigation {
    interface Update : Communication.Update<Screen>
    interface Observe : Communication.Observe<Screen>
    interface Mutable : Update, Observe
    class Base : Communication.Single<Screen>(), Mutable {
        override fun update(value: Screen) {
            if (value != liveData.value) liveData.value = value
        }
    }
}