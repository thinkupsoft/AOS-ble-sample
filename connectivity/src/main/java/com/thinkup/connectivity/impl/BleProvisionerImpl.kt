package com.thinkup.connectivity.impl

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thinkup.connectivity.BleProvisioner
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.mesh.ProvisionCallback
import com.thinkup.connectivity.mesh.provision
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.config.NodeConfigMessageUnacked
import com.thinkup.connectivity.provisioning.Status
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import com.thinkup.connectivity.utils.TimeoutLiveData
import no.nordicsemi.android.meshprovisioner.Provisioner
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.meshprovisioner.transport.*

/**
 * STEPS:
 *      1 - CONNECTING - Disconnect to provisioned node (if necessary) and connect to BLE Device (Unprovisioned)
 *      2 - READY, DISCOVERING - Identify the node capabilities
 *      3 - INITIALIZING, NODE_FOUND, PROVISIONING_PROGRESS - Assign an unicast address and start the message to provision
 *      4 - SETTING - Reconnect to device like a proxy and exchange messages to set (ConfigCompositionDataGet, ConfigDefaultTtlGet,
 *          ConfigAppKeyAdd, ConfigNetworkTransmitSet)
 *       5 - BINDING_APP_KEY - Set appKey credential value to the vendor model
 *      6 - SET_PUBLISH_ADDRESS - Set address publication to send message. This address will receive all messages sent from the node
 *      7 - FULL_CONFIGURED - Complete the provisioning and send a config message to visually identify the number assigned to the node
 */
class BleProvisionerImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BaseBleImpl(context, setting, repository),
    BleProvisioner, ProvisionCallback {

    private var status = MutableLiveData<Status>()
    private var device: ExtendedBluetoothDevice? = null
    private var meshNode: ProvisionedMeshNode? = null
    private val handler = Handler(Looper.getMainLooper())
    private var runnable = Runnable {}

    private fun handlerBindAppKey(meshNode: ProvisionedMeshNode) {
        if (status.value?.priority ?: Status.BINDING_APP_KEY.priority > Status.BINDING_APP_KEY.priority) return
        runnable = Runnable { handlerBindAppKey(meshNode) }
        bindAppKey(meshNode)
        handler.postDelayed(runnable, STEP_TIMEOUT)
    }

    override fun getStatus(): LiveData<Status> = status

    override fun connect(
        context: Context,
        device: ExtendedBluetoothDevice
    ): LiveData<Status> {
        status = TimeoutLiveData(timeout = PROVISION_TIMEOUT, default = Status.TIMEOUT, control = Status.FULL_CONFIGURED) {
            handler.removeCallbacks(runnable)
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

    private fun bindAppKey(meshNode: ProvisionedMeshNode) {
        val element: Element? = getElement(meshNode)
        if (element != null) {
            val model: MeshModel? = getModel<VendorModel>(element)
            if (model != null) {
                status.postValue(Status.BINDING_APP_KEY)
                if (meshNode.addedAppKeys.isNotEmpty()) {
                    val configModelAppUnbind = ConfigModelAppBind(element.elementAddress, model.modelId, meshNode.addedAppKeys[0].index)
                    sendMessage(meshNode, configModelAppUnbind, true)
                }
            }
        }
    }

    private fun updateName(meshNode: ProvisionedMeshNode) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        network?.updateNodeName(meshNode, getNextNameAvailable())
    }

    private fun setPublication(meshNode: ProvisionedMeshNode) {
        val element: Element? = getElement(meshNode)
        if (element != null) {
            val model: MeshModel? = getModel<VendorModel>(element)
            if (model != null) {
                status.postValue(Status.SET_PUBLISH_ADDRESS)
                sendMessage(
                    meshNode, ConfigModelPublicationSet(
                        element.elementAddress, "0001".toInt(16), 0, false,
                        0xFF, 0, 0, 1, 1, model.modelId
                    ), true
                )
            }
        }
    }

    private fun configMessage(meshNode: ProvisionedMeshNode) {
        val element: Element? = getElement(meshNode)
        if (element != null) {
            val model: MeshModel? = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    val vendorModel = model as VendorModel
                    sendMessage(
                        meshNode,
                        NodeConfigMessageUnacked(
                            meshNode.nodeName.toInt(), appKey, model.modelId, vendorModel.companyIdentifier
                        ), repository.isSending
                    )
                }
            }
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
                Handler(Looper.getMainLooper()).postDelayed({ provisioningAction(node) }, 200)
            }
        }
    }

    override fun provisionFail() {
        status.postValue(Status.ERROR)
    }

    override fun provisionComplete(meshNode: ProvisionedMeshNode) {
        this.meshNode = meshNode
        status.postValue(Status.SETTING)
    }

    override fun bindNodeKeyComplete(meshNode: ProvisionedMeshNode) {
        this.meshNode = meshNode
        handler.removeCallbacks(runnable)
        handlerBindAppKey(meshNode)
    }

    override fun bindAppKeyComplete(meshNode: ProvisionedMeshNode) {
        this.meshNode = meshNode
        handler.removeCallbacks(runnable)
        updateName(meshNode)
        setPublication(meshNode)
    }

    override fun setPublicationComplete(meshNode: ProvisionedMeshNode) {
        this.meshNode = meshNode
        repository.isSending = false
        handler.removeCallbacks(runnable)
        status.postValue(Status.FULL_CONFIGURED)
        if (setting.enabledProvisionConfig())
            Handler(Looper.getMainLooper()).postDelayed({ configMessage(meshNode) }, STEP_TIMEOUT)
    }
}