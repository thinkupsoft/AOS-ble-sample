package com.thinkup.connectivity.provisioning

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.thinkup.connectivity.common.BaseBleViewModel
import com.thinkup.connectivity.common.Status
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import com.thinkup.connectivity.utils.ProvisionerStates
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.Provisioner
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.meshprovisioner.transport.*

class ProvisioningViewModel(context: Context, repository: NrfMeshRepository) : BaseBleViewModel(context, repository) {

    init {
        status = repository.getConnectionState()
    }

    fun removeOberservers(lco: LifecycleOwner) = status.removeObservers(lco)

    fun connect(
        lco: LifecycleOwner,
        context: Context,
        device: ExtendedBluetoothDevice,
        connectToNetwork: Boolean = false
    ): LiveData<Status> {
        repository.getMeshNetworkLiveData().resetSelectedAppKey()
        deviceReady(lco, device)
        repository.connect(context, device, connectToNetwork)
        return status
    }

    private fun deviceReady(lco: LifecycleOwner, device: ExtendedBluetoothDevice) {
        repository.isDeviceReady().observe(lco, Observer {
            if (repository.getBleMeshManager().isDeviceReady()) {
                if (!repository.isProvisioningComplete()) {
                    identifyAction(lco, device)
                } else {
                    status.postValue(Status.PROVISIONING_COMPLETE)
                }
            }
        })
    }

    private fun identifyAction(lco: LifecycleOwner, device: ExtendedBluetoothDevice) {
        device.name = repository.getMeshNetworkLiveData().getNodeName()!!
        identifyNode(device).observe(lco, Observer { meshNode ->
            meshNode?.let { node ->
                val capabilities = node.provisioningCapabilities
                capabilities?.let { capabilities ->
                    repository.getMeshNetworkLiveData().getMeshNetwork()?.let { network ->
                        val elementCount = capabilities.numberOfElements.toInt()
                        val provisioner: Provisioner = network.getSelectedProvisioner()
                        val unicast: Int = network.nextAvailableUnicastAddress(elementCount, provisioner)
                        network.assignUnicastAddress(unicast)
                        status.postValue(Status.READY)
                    }
                }
            }
        })
    }

    fun provisioningAction(lco: LifecycleOwner) {
        provisioning().observe(lco, Observer {
            when (it.getProvisionerProgress()?.state) {
                ProvisionerStates.PROVISIONING_START -> status.postValue(Status.PROVISIONING_START)
                ProvisionerStates.PROVISIONING_COMPLETE -> status.postValue(Status.PROVISIONING_COMPLETE)
                ProvisionerStates.PROVISIONING_FAILED -> status.postValue(Status.PROVISIONING_FAILED)
                ProvisionerStates.NETWORK_TRANSMIT_STATUS_RECEIVED -> {
                    if (repository.isProvisioningComplete()) {
                        status.postValue(Status.PROVISIONING_COMPLETE)
                        val node = repository.getProvisionedMeshNode().value
                        if (node?.elements?.isNotEmpty() == true) {
                            bindNodeKey(node)
                            repository.getProvisioningState().removeObservers(lco)
                            bindAppKey(lco, node)
                        }
                    } else status.postValue(Status.PROVISIONING_PROGRESS)
                }
                else -> status.postValue(Status.PROVISIONING_PROGRESS)
            }
        })
    }

    private fun bindNodeKey(node: ProvisionedMeshNode) {
        val appKey: ApplicationKey = repository.getMeshNetworkLiveData().getSelectedAppKey()
        val index: Int = node.addedNetKeys[0].index
        val networkKey: NetworkKey = repository.getMeshNetworkLiveData().getNetworkKeys().get(index)
        val configAppKeyAdd = ConfigAppKeyAdd(networkKey, appKey)
        sendMessage(node, configAppKeyAdd)
    }

    private fun getBindAppKey(): Int {
        return bindAppKey()[0]?.keyIndex ?: 0
    }

    private fun updateName(node: ProvisionedMeshNode) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        network?.updateNodeName(node, getNextNameAvailable())
    }

    private fun bindAppKey(lco: LifecycleOwner, meshNode: ProvisionedMeshNode?) {
        status.postValue(Status.BINDING_KEY)
        if (meshNode != null) {
            val element: Element? = getElement(meshNode)
            if (element != null) {
                val model: MeshModel? = getModel<VendorModel>(element)
                if (model != null) {
                    val configModelAppUnbind = ConfigModelAppBind(element.elementAddress, model.modelId, meshNode.addedAppKeys[0].index)
                    sendMessage(meshNode, configModelAppUnbind)
                    repository.getSelectedModel()?.observe(lco, Observer {
                        updateName(meshNode)
                        status.postValue(Status.FULL_CONFIGURED)
                    })
                }
            }
        }
    }

    private fun getNextNameAvailable(): String {
        var nextName = '1'
        val nodes = getNodes()
        nodes?.let {
            val names = it.map { n -> n.nodeName }
            while (names.contains(nextName.toString())) nextName += 1
        }
        return nextName.toString()
    }

    private fun identifyNode(device: ExtendedBluetoothDevice): LiveData<UnprovisionedMeshNode?> {
        repository.identifyNode(device)
        return repository.getUnprovisionedMeshNode()
    }

    private fun provisioning(): ProvisioningStatusLiveData {
        val node = repository.getUnprovisionedMeshNode().value
        node?.let {
            repository.getMeshManagerApi().startProvisioning(it)
        }
        return repository.getProvisioningState()
    }
}