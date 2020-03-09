package com.thinkup.connectivity

import androidx.lifecycle.LiveData
import com.thinkup.connectivity.impl.BleSessionImpl

interface BleSession : BleConnection {
    fun start()
    fun stop()
    fun state(): LiveData<BleSessionImpl.ConnectionState>
}