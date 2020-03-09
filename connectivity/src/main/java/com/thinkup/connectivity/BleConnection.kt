package com.thinkup.connectivity

import androidx.lifecycle.LiveData
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.utils.EventObserver
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface BleConnection {
    fun settings(): BleSetting
    // connection status
    fun isConnected(): LiveData<Boolean>

    fun getBatteryAverage(): LiveData<Int?>

    fun checkConnectivity(): Boolean

    fun getProvisionedNodes(): LiveData<List<ProvisionedMeshNode>>

    fun getNodes(): List<ProvisionedMeshNode>?

    fun getGroups(): LiveData<List<Group>>

    fun getGroupNodes(group: Group): List<ProvisionedMeshNode>

    fun getMessages(): EventObserver<MeshMessage?>

    fun getEvents(): EventObserver<NodeEventStatus?>

    fun autoConnect(onConnect: (() -> Unit)? = null)

    fun disconnect()

    fun forceDelete(unicastAddress: Int)

    // nodes
    fun sendMessage(node: ProvisionedMeshNode, message: MeshMessage, isBlocking: Boolean = false)

    // groups
    fun sendMessage(group: Group, message: MeshMessage, isBlocking: Boolean = false)

}