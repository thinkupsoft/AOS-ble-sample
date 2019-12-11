package com.thinkup.connectivity.impl

import com.thinkup.connectivity.BleScanner
import com.thinkup.connectivity.scanner.ScannerRepository
import com.thinkup.connectivity.utils.Constants

class BleScannerImpl(private val repository: ScannerRepository) : BleScanner {

    override fun getState() = repository.getScannerState()

    override fun scan() {
        repository.registerBroadcastReceivers()
        repository.startScan(Constants.MESH_PROVISIONING_UUID)
    }

    override fun stop() {
        repository.stopScan()
        repository.unregisterBroadcastReceivers()
    }
}