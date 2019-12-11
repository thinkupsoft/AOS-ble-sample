package com.thinkup.connectivity

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.thinkup.connectivity.common.Status
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice

interface BleProvisioner: BleConnection {
    fun getStatus(): LiveData<Status>

    fun connect(
        lifecycleOwner: LifecycleOwner,
        context: Context,
        device: ExtendedBluetoothDevice,
        connectToNetwork: Boolean = false
    ): LiveData<Status>

    fun provisioningAction(lifecycleOwner: LifecycleOwner)
}