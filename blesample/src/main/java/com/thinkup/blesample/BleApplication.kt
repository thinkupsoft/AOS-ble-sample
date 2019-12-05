package com.thinkup.blesample

import android.app.Application

class BleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DiWrapper.start(this)
    }
}