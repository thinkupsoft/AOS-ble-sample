package com.thinkup.connectivity.common

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.MeshModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

open class BaseBleViewModel(private val context: Context, protected val repository: NrfMeshRepository) : ViewModel() {

    protected var status = MutableLiveData<Status>()

    fun isConnected() = repository.isConnectedToProxy()

    fun loadNodes(): LiveData<List<ProvisionedMeshNode>> {
        return repository.getNodes()
    }

    fun getNodes() = loadNodes().value

    fun disconnect() {
        repository.disconnect()
    }

    fun autoConnect(onConnect: (() -> Unit)? = null) {
        repository.autoConnect(context).observeForever {
            if (repository.getBleMeshManager().isDeviceReady()) onConnect?.invoke()
        }
    }

    protected fun bindAppKey() = repository.getMeshNetworkLiveData().getAppKeys()

    protected fun getAppKey(index: Int) = repository.getMeshNetworkLiveData().getMeshNetwork()?.getAppKey(index)

    fun sendMessage(node: ProvisionedMeshNode, message: MeshMessage) = sendMessage(node.unicastAddress, message)

    fun sendMessage(group: Group, message: MeshMessage) = sendMessage(group.address, message)

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

    fun getMessages() = repository.getMeshMessageLiveData()

    fun checkConnectivity(): Boolean {
        return repository.isConnectedToProxy()?.value ?: false
    }

    protected fun getElement(meshNode: ProvisionedMeshNode): Element? {
        return if (meshNode.elements.values.isNotEmpty()) meshNode.elements.values.elementAt(0) else null
    }

    protected inline fun <reified T : MeshModel> getModel(element: Element): T? {
        return element.meshModels.map { it.value }.filterIsInstance<T>()[0]
    }
}