package com.thinkup.connectivity

import android.content.Context
import androidx.lifecycle.LiveData
import com.thinkup.connectivity.provisioning.Status
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice

interface BleProvisioner: BleConnection {
    fun getStatus(): LiveData<Status>

    fun connect(
        context: Context,
        device: ExtendedBluetoothDevice
    ): LiveData<Status>
}