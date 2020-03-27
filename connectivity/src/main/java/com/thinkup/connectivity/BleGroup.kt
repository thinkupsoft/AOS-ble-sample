package com.thinkup.connectivity

import com.thinkup.connectivity.messges.NO_CONFIG
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface BleGroup : BleConnection {
    fun addGroup(name: String): Boolean
    fun removeGroup(group: Group): Boolean
    fun getStatus(group: Group, model: VendorModel)
    fun addGroupNode(group: Group, meshNode: ProvisionedMeshNode?)
    fun removeGroupNode(group: Group, meshNode: ProvisionedMeshNode)
    fun identify(group: Group)
    fun identify(groups: List<Group>)
    fun controlMessage(group: Group, params: Int, timeout: Int = NO_CONFIG)
    fun setPrePeripheralMessage(group: Group, dimmer: Int, gesture: Int, distance: Int, sound: Int)
    fun setStepPeripheralMessage(group: Group, shape: Int, color: Int, led: Int)
    fun configMessage(group: Group, id: Int = NO_CONFIG)
}