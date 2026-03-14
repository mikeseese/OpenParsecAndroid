package com.aigch.openparsec

import android.app.Application

class OpenParsecApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        com.aigch.openparsec.parsec.CParsec.destroy()
    }

    companion object {
        lateinit var instance: OpenParsecApp
            private set
    }
}
