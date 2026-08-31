package com.unidospelovolei

import android.app.Application

class VoleiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.iniciarSync()
    }
}
