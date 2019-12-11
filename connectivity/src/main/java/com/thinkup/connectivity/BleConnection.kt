package com.thinkup.connectivity

import androidx.lifecycle.LiveData
import com.thinkup.connectivity.messges.event.NodeEventStatus
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface BleConnection {
    // connection status
    fun isConnected(): LiveData<Boolean>

    fun checkConnectivity(): Boolean

    fun getProvisionedNodes(): LiveData<List<ProvisionedMeshNode>>

    fun getNodes(): List<ProvisionedMeshNode>?

    fun getGroups(): LiveData<List<Group>>

    fun getMessages(): LiveData<MeshMessage?>

    fun getEvents(): LiveData<NodeEventStatus>

    fun autoConnect(onConnect: (() -> Unit)? = null)

    fun disconnect()

    // nodes
    fun sendMessage(node: ProvisionedMeshNode, message: MeshMessage)

    // groups
    fun sendMessage(group: Group, message: MeshMessage)

}