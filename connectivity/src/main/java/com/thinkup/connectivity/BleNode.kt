package com.thinkup.connectivity

import androidx.lifecycle.LiveData
import com.thinkup.connectivity.messges.NO_CONFIG
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface BleNode : BleConnection {
    fun delete(node: ProvisionedMeshNode): LiveData<Boolean>
    fun deleteDB(node: ProvisionedMeshNode)
    fun identify(node: ProvisionedMeshNode)
    fun showBatteryPercentage(node: ProvisionedMeshNode)
    fun identify(nodes: List<ProvisionedMeshNode>)
    fun getStatus(node: ProvisionedMeshNode)
    fun controlMessage(node: ProvisionedMeshNode, params: Int, timeout: Int = NO_CONFIG, ack: Boolean = true)
    fun configMessage(node: ProvisionedMeshNode, id: Int = NO_CONFIG, ack: Boolean = true)
    fun setPrePeripheralMessage(node: ProvisionedMeshNode, dimmer: Int, gesture: Int, distance: Int, sound: Int, ack: Boolean = true)
    fun setStepPeripheralMessage(node: ProvisionedMeshNode, shape: Int, color: Int, led: Int, ack: Boolean = true)
}