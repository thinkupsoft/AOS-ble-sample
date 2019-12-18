package com.thinkup.blesample

import android.app.Application
import com.thinkup.connectivity.BleSession
import org.koin.android.ext.android.inject

class BleApplication : Application() {

    val session: BleSession by inject()

    override fun onCreate() {
        super.onCreate()
        DiWrapper.start(this)
        session.start()
    }

    override fun onTerminate() {
        super.onTerminate()
        session.stop()
    }
}