package com.thinkup.connectivity.scanner

import androidx.lifecycle.ViewModel
import com.thinkup.connectivity.utils.Constants
import java.util.*

class ScannerViewModel(private val repository: ScannerRepository) : ViewModel() {

    init {
        repository.registerBroadcastReceivers()
    }

    fun getState() = repository.getScannerState()

    fun scan(uuid: UUID = Constants.MESH_PROVISIONING_UUID) = repository.startScan(uuid)

    fun stop() = repository.stopScan()

    override fun onCleared() {
        super.onCleared()
        repository.unregisterBroadcastReceivers()
    }
}