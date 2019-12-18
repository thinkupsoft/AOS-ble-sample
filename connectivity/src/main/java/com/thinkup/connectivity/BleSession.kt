package com.thinkup.connectivity

interface BleSession : BleConnection {
    fun start()
    fun keepAlive()
    fun stop()
}