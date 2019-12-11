package com.thinkup.connectivity

import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.MeshModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface BleGroup: BleConnection {
    fun addGroup(name: String): Boolean
    fun removeGroup(group: Group): Boolean
    fun getGroupNodes(group: Group): List<MeshModel>
    fun getStatus(group: Group, model: VendorModel)
    fun getTtl(group: Group)
    fun addGroupNode(group: Group, meshNode: ProvisionedMeshNode?)
    fun removeGroupNode(group: Group, model: VendorModel)
}