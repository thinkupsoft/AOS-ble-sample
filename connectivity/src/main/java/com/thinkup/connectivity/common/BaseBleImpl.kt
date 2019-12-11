package com.thinkup.connectivity.common

import android.content.Context
import androidx.lifecycle.*
import com.thinkup.connectivity.BleConnection
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.MeshModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

open class BaseBleImpl(private val context: Context, protected val repository: NrfMeshRepository) : BleConnection {

    protected var status = MutableLiveData<Status>()

    override fun isConnected() = repository.isConnectedToProxy()

    override fun getProvisionedNodes(): LiveData<List<ProvisionedMeshNode>> {
        return repository.getNodes()
    }

    override fun getNodes() = getProvisionedNodes().value

    override fun getGroups(): LiveData<List<Group>> {
        return repository.getGroups()
    }

    override fun disconnect() {
        repository.disconnect()
    }

    override fun autoConnect(onConnect: (() -> Unit)?) {
        repository.autoConnect(context).observeForever {
            if (repository.getBleMeshManager().isDeviceReady()) onConnect?.invoke()
        }
    }

    protected fun bindAppKey() = repository.getMeshNetworkLiveData().getAppKeys()

    protected fun getAppKey(index: Int) = repository.getMeshNetworkLiveData().getMeshNetwork()?.getAppKey(index)

    override fun sendMessage(node: ProvisionedMeshNode, message: MeshMessage) = sendMessage(node.unicastAddress, message)

    override fun sendMessage(group: Group, message: MeshMessage) = sendMessage(group.address, message)

    private fun sendMessage(unicastAddress: Int, message: MeshMessage) {
        try {
            if (!checkConnectivity()) autoConnect { sendMessage(unicastAddress, message) }
            else {
                repository.getMeshManagerApi().createMeshPdu(unicastAddress, message)
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
    }

    override fun getMessages() = repository.getMeshMessageLiveData()

    override fun getEvents() = repository.getEventMessageLiveData()

    override fun checkConnectivity(): Boolean {
        return repository.isConnectedToProxy().value ?: false
    }

    protected fun getElement(meshNode: ProvisionedMeshNode): Element? {
        return if (meshNode.elements.values.isNotEmpty()) meshNode.elements.values.elementAt(0) else null
    }

    protected inline fun <reified T : MeshModel> getModel(element: Element): T? {
        return element.meshModels.map { it.value }.filterIsInstance<T>()[0]
    }
}