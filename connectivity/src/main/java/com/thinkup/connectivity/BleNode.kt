package com.thinkup.connectivity

import androidx.lifecycle.LiveData
import com.thinkup.connectivity.messges.NO_CONFIG
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface BleNode : BleConnection {
    fun delete(node: ProvisionedMeshNode): LiveData<Boolean>
    fun deleteDB(node: ProvisionedMeshNode)
    fun identify(node: ProvisionedMeshNode)
    fun identify(nodes: List<ProvisionedMeshNode>)
    fun getStatus(node: ProvisionedMeshNode)
    fun controlMessage(node: ProvisionedMeshNode, params: Int, ack: Boolean = true)
    fun configMessage(
        node: ProvisionedMeshNode,
        id: Int = NO_CONFIG,
        timeoutConfig: Int = NO_CONFIG,
        timeout: Int = NO_CONFIG,
        ack: Boolean = true
    )

    fun setPeripheralMessage(
        node: ProvisionedMeshNode,
        shape: Int, color: Int, dimmer: Int, led: Int,
        fill: Int, gesture: Int, distance: Int, filter: Int, touch: Int, sound: Int,
        ack: Boolean = true
    )
}