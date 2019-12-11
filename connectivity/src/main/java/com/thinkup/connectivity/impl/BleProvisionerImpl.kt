package com.thinkup.connectivity.impl

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.thinkup.connectivity.BleProvisioner
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.common.Status
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.config.NodeConfigMessage
import com.thinkup.connectivity.provisioning.ProvisioningStatusLiveData
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import com.thinkup.connectivity.utils.ProvisionerStates
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.Provisioner
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils

class BleProvisionerImpl(context: Context, repository: NrfMeshRepository) : BaseBleImpl(context, repository), BleProvisioner {

    init {
        status = repository.getConnectionState()
    }

    override fun getStatus(): LiveData<Status> = status

    override fun connect(
        lifecycleOwner: LifecycleOwner,
        context: Context,
        device: ExtendedBluetoothDevice,
        connectToNetwork: Boolean
    ): LiveData<Status> {
        repository.getMeshNetworkLiveData().resetSelectedAppKey()
        deviceReady(lifecycleOwner, device)
        repository.connect(context, device, connectToNetwork)
        return status
    }

    private fun deviceReady(lifecycleOwner: LifecycleOwner, device: ExtendedBluetoothDevice) {
        repository.isDeviceReady().observe(lifecycleOwner, Observer {
            if (repository.getBleMeshManager().isDeviceReady()) {
                if (!repository.isProvisioningComplete()) {
                    identifyAction(lifecycleOwner, device)
                } else {
                    status.postValue(Status.PROVISIONING_COMPLETE)
                }
            }
        })
    }

    private fun identifyAction(lifecycleOwner: LifecycleOwner, device: ExtendedBluetoothDevice) {
        device.name = repository.getMeshNetworkLiveData().getNodeName()!!
        identifyNode(device).observe(lifecycleOwner, Observer { meshNode ->
            meshNode?.let { node ->
                val capabilities = node.provisioningCapabilities
                capabilities?.let { caps ->
                    repository.getMeshNetworkLiveData().getMeshNetwork()?.let { network ->
                        val elementCount = caps.numberOfElements.toInt()
                        val provisioner: Provisioner = network.selectedProvisioner
                        val unicast: Int = network.nextAvailableUnicastAddress(elementCount, provisioner)
                        network.assignUnicastAddress(unicast)
                        status.postValue(Status.READY)
                    }
                }
            }
        })
    }

    override fun provisioningAction(lifecycleOwner: LifecycleOwner) {
        provisioning().observe(lifecycleOwner, Observer {
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
                            repository.getProvisioningState().removeObservers(lifecycleOwner)
                            bindAppKey(lifecycleOwner, node)
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

    private fun updateName(node: ProvisionedMeshNode) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        network?.updateNodeName(node, getNextNameAvailable())
    }

    private fun bindAppKey(lifecycleOwner: LifecycleOwner, meshNode: ProvisionedMeshNode?) {
        status.postValue(Status.BINDING_KEY)
        if (meshNode != null) {
            val element: Element? = getElement(meshNode)
            if (element != null) {
                val model: MeshModel? = getModel<VendorModel>(element)
                if (model != null) {
                    val configModelAppUnbind = ConfigModelAppBind(element.elementAddress, model.modelId, meshNode.addedAppKeys[0].index)
                    sendMessage(meshNode, configModelAppUnbind)
                    repository.getSelectedModel()?.observe(lifecycleOwner, Observer {
                        updateName(meshNode)
                        setPublication(lifecycleOwner, meshNode, element, model)
                    })
                }
            }
        }
    }

    private fun setPublication(lifecycleOwner: LifecycleOwner, meshNode: ProvisionedMeshNode, element: Element, model: MeshModel) {
        status.postValue(Status.SET_PUBLISH_ADDRESS)
        getMessages().observe(lifecycleOwner, Observer {
            if (it is ConfigModelPublicationStatus) {
                getMessages().removeObservers(lifecycleOwner)
                configMessage(meshNode, model as VendorModel, meshNode.nodeName.toInt())
                status.postValue(Status.FULL_CONFIGURED)
            }
        })
        sendMessage(
            meshNode, ConfigModelPublicationSet(
                element.elementAddress, "0001".toInt(16), 0, false,
                0xFF, 0, 0, 1, 1, model.modelId
            )
        )
    }

    private fun configMessage(
        node: ProvisionedMeshNode,
        model: VendorModel,
        id: Int
    ) {
        val appKey = getAppKey(model.boundAppKeyIndexes[0])
        appKey?.let {
            sendMessage(node, NodeConfigMessage(id, NO_CONFIG, 0, appKey, model.modelId, model.companyIdentifier))
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