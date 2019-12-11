package com.thinkup.connectivity

import com.thinkup.connectivity.scanner.ScannerLiveData

interface BleScanner {

    fun getState(): ScannerLiveData

    fun scan()

    fun stop()
}