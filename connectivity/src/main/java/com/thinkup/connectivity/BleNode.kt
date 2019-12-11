package com.thinkup.connectivity

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.thinkup.connectivity.messges.NO_CONFIG
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface BleNode: BleConnection {
    fun delete(lifecycleOwner: LifecycleOwner, node: ProvisionedMeshNode): LiveData<Boolean>
    fun getStatus(node: ProvisionedMeshNode)
    fun getTtl(node: ProvisionedMeshNode)
    fun controlMessage(node: ProvisionedMeshNode, params: Int)
    fun configMessage(
        node: ProvisionedMeshNode,
        id: Int = NO_CONFIG,
        timeoutConfig: Int = NO_CONFIG,
        timeout: Int = NO_CONFIG
    )
    fun setPeripheralMessage(
        node: ProvisionedMeshNode,
        shape: Int, color: Int, dimmer: Int, led: Int,
        fill: Int, gesture: Int, distance: Int, filter: Int, touch: Int, sound: Int
    )
}