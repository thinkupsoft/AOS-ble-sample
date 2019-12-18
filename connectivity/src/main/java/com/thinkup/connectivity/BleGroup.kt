package com.thinkup.connectivity

import com.thinkup.connectivity.messges.NO_CONFIG
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface BleGroup: BleConnection {
    fun addGroup(name: String): Boolean
    fun removeGroup(group: Group): Boolean
    fun getGroupNodes(group: Group): List<ProvisionedMeshNode>
    fun getStatus(group: Group, model: VendorModel)
    fun addGroupNode(group: Group, meshNode: ProvisionedMeshNode?)
    fun removeGroupNode(group: Group, meshNode: ProvisionedMeshNode)
    fun identify(group: Group)
    fun identify(groups: List<Group>)
    fun controlMessage(group: Group, params: Int)
    fun setPeripheralMessage(
        group: Group,
        shape: Int, color: Int, dimmer: Int, led: Int,
        fill: Int, gesture: Int, distance: Int, filter: Int, touch: Int, sound: Int
    )
    fun configMessage(
        group: Group,
        id: Int = NO_CONFIG,
        timeoutConfig: Int = NO_CONFIG,
        timeout: Int = NO_CONFIG
    )
}