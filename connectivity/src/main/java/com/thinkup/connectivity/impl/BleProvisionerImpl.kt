package com.thinkup.connectivity.impl

import android.content.Context
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thinkup.connectivity.BleProvisioner
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.mesh.ProvisionCallback
import com.thinkup.connectivity.mesh.provision
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.config.NodeConfigMessageUnacked
import com.thinkup.connectivity.provisioning.Status
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import com.thinkup.connectivity.utils.TimeoutLiveData
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.Provisioner
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.meshprovisioner.transport.*

class BleProvisionerImpl(context: Context, repository: NrfMeshRepository) : BaseBleImpl(context, repository), BleProvisioner, ProvisionCallback {

    private var status = MutableLiveData<Status>()
    private var device: ExtendedBluetoothDevice? = null
    private var meshNode: ProvisionedMeshNode? = null

    override fun getStatus(): LiveData<Status> = status

    override fun connect(
        context: Context,
        device: ExtendedBluetoothDevice
    ): LiveData<Status> {
        status = TimeoutLiveData(PROVISION_TIMEOUT, Status.TIMEOUT) {
            meshNode?.let { forceDelete(it.unicastAddress) }
        }
        status.postValue(Status.CONNECTING)
        this.device = device
        repository.provision(context, device, this)
        return status
    }

    private fun provisioningAction(node: UnprovisionedMeshNode) {
        repository.getMeshManagerApi().startProvisioning(node)
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

    private fun bindNodeKey(meshNode: ProvisionedMeshNode) {
        status.postValue(Status.BINDING_KEY)
        val appKey: ApplicationKey = repository.getMeshNetworkLiveData().getSelectedAppKey()
        val index: Int = meshNode.addedNetKeys[0].index
        val networkKey: NetworkKey = repository.getMeshNetworkLiveData().getNetworkKeys().get(index)
        val configAppKeyAdd = ConfigAppKeyAdd(networkKey, appKey)
        sendMessage(meshNode, configAppKeyAdd)
    }

    private fun bindAppKey(meshNode: ProvisionedMeshNode) {
        val element: Element? = getElement(meshNode)
        if (element != null) {
            val model: MeshModel? = getModel<VendorModel>(element)
            if (model != null) {
                status.postValue(Status.BINDING_KEY)
                val configModelAppUnbind = ConfigModelAppBind(element.elementAddress, model.modelId, meshNode.addedAppKeys[0].index)
                sendMessage(meshNode, configModelAppUnbind)
            }
        }
    }

    private fun updateName(meshNode: ProvisionedMeshNode) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        network?.updateNodeName(meshNode, getNextNameAvailable())
    }

    private fun setPublication(meshNode: ProvisionedMeshNode, element: Element, model: MeshModel) {
        status.postValue(Status.SET_PUBLISH_ADDRESS)
        sendMessage(
            meshNode, ConfigModelPublicationSet(
                element.elementAddress, "0001".toInt(16), 0, false,
                0xFF, 0, 0, 1, 1, model.modelId
            )
        )
    }

    private fun configMessage(meshNode: ProvisionedMeshNode, element: Element, model: MeshModel) {
        val appKey = getAppKey(model.boundAppKeyIndexes[0])
        appKey?.let {
            val vendorModel = model as VendorModel
            sendMessage(
                meshNode,
                NodeConfigMessageUnacked(
                    meshNode.nodeName.toInt(), NO_CONFIG,
                    0, appKey, model.modelId, vendorModel.companyIdentifier
                )
            )
        }
    }

    // Provision Callback

    override fun deviceReady() {
        // to avoid double callbacks
        if (status.value?.priority ?: Status.READY.priority > Status.READY.priority) return
        status.postValue(Status.READY)
        device?.let {
            status.postValue(Status.DISCOVERING)
            repository.identifyNode(it)
        }
    }

    override fun deviceFail() {
        status.postValue(Status.ERROR)
    }

    override fun identify(node: UnprovisionedMeshNode) {
        // to avoid double callbacks
        if (status.value?.priority ?: Status.INITIALIZING.priority > Status.INITIALIZING.priority) return
        status.postValue(Status.INITIALIZING)
        device?.name = repository.getMeshNetworkLiveData().getNodeName()!!
        val capabilities = node.provisioningCapabilities
        capabilities?.let { caps ->
            status.postValue(Status.NODE_FOUND)
            repository.getMeshNetworkLiveData().getMeshNetwork()?.let { network ->
                status.postValue(Status.PROVISIONING_PROGRESS)
                val elementCount = caps.numberOfElements.toInt()
                val provisioner: Provisioner = network.selectedProvisioner
                val unicast: Int = network.nextAvailableUnicastAddress(elementCount, provisioner)
                network.assignUnicastAddress(unicast)
                Handler().postDelayed({ provisioningAction(node) }, 200)
            }
        }
    }

    override fun provisionFail() {
        status.postValue(Status.ERROR)
    }

    override fun provisionComplete(meshNode: ProvisionedMeshNode) {
        this.meshNode = meshNode
        bindNodeKey(meshNode)
    }

    override fun bindNodeKeyComplete(meshNode: ProvisionedMeshNode) {
        this.meshNode = meshNode
        bindAppKey(meshNode)
    }

    override fun bindAppKeyComplete(meshNode: ProvisionedMeshNode, element: Element, model: MeshModel) {
        this.meshNode = meshNode
        updateName(meshNode)
        setPublication(meshNode, element, model)
    }

    override fun setPublicationComplete(meshNode: ProvisionedMeshNode, element: Element, model: MeshModel) {
        status.postValue(Status.FULL_CONFIGURED)
        configMessage(meshNode, element, model)
    }
}