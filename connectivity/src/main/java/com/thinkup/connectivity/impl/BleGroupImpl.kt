package com.thinkup.connectivity.impl

import android.content.Context
import android.util.Log
import com.thinkup.connectivity.BleGroup
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.exceptions.AppKeyException
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.ColorParams
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.PeripheralParams
import com.thinkup.connectivity.messges.ShapeParams
import com.thinkup.connectivity.messges.config.NodeConfigMessageUnacked
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import com.thinkup.connectivity.messges.peripheral.NodePrePeripheralMessageUnacked
import com.thinkup.connectivity.messges.peripheral.NodeStepPeripheralMessage
import com.thinkup.connectivity.messges.peripheral.NodeStepPeripheralMessageUnacked
import com.thinkup.connectivity.messges.status.NodeGetMessage
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress

class BleGroupImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BaseBleImpl(context, setting, repository), BleGroup {

    companion object {
        const val GROUP_A = "A"
        const val GROUP_B = "B"
        const val GROUP_C = "C"
        const val GROUP_D = "D"
    }

    override fun addGroup(name: String): Boolean {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val newGroup = network?.createGroup(network.selectedProvisioner, name)
        newGroup?.let {
            if (network.addGroup(it)) {
                repository.getMeshManagerApi().updateDb()
                return true
            }
        }
        return false
    }

    override fun removeGroup(group: Group): Boolean {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        if (network?.removeGroup(group) == true) {
            repository.getMeshManagerApi().updateDb()
            return true
        }
        return false
    }

    override fun getStatus(group: Group, model: VendorModel) {
        val appKey = getAppKey(model.boundAppKeyIndexes[0] ?: 0)
        appKey?.let {
            sendMessage(group, NodeGetMessage(appKey, model.modelId, model.companyIdentifier))
        } ?: run {
            throw AppKeyException()
        }
    }

    override fun addGroupNode(group: Group, meshNode: ProvisionedMeshNode?) {
        // subscription
        if (meshNode != null) {
            val element: Element? = getElement(meshNode)
            if (element != null) {
                val elementAddress = element.elementAddress
                val model: MeshModel? = getModel<VendorModel>(element)
                if (model != null) {
                    val modelIdentifier = model.modelId
                    val configModelSubscriptionAdd: MeshMessage
                    configModelSubscriptionAdd = ConfigModelSubscriptionAdd(elementAddress, group.address, modelIdentifier)
                    sendMessage(meshNode, configModelSubscriptionAdd)
                }
            }
        }
    }

    override fun removeGroupNode(group: Group, meshNode: ProvisionedMeshNode) {
        val address: Int = group.address
        val element: Element? = getElement(meshNode)
        if (element != null) {
            val model: MeshModel? = getModel<VendorModel>(element)
            if (model != null) {
                var subscriptionDelete: MeshMessage? = null
                if (MeshAddress.isValidGroupAddress(address)) {
                    subscriptionDelete = ConfigModelSubscriptionDelete(element.elementAddress, address, model.modelId)
                }
                if (subscriptionDelete != null) {
                    sendMessage(meshNode, subscriptionDelete)
                }
            }
        }
    }

    override fun identify(groups: List<Group>) {
        bulkMessaging(groups) {
            identify(it)
            delay(BULK_DELAY)
        }
    }

    override fun identify(group: Group) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val models = network?.getModels(group)
        if (models?.isNotEmpty() == true) {
            val model = models[0] as VendorModel
            val appKey = getAppKey(model.boundAppKeyIndexes[0])
            appKey?.let {
                peripheralMessage(group, identifyMessage(group, appKey, model.modelId, model.companyIdentifier))
            }
        }
    }

    override fun controlMessage(group: Group, params: Int, timeout: Int) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val models = network?.getModels(group)
        if (models?.isNotEmpty() == true) {
            val model = models[0] as VendorModel
            val appKey = getAppKey(model.boundAppKeyIndexes[0])
            appKey?.let {
                sendMessage(group, NodeControlMessageUnacked(params.toByte(), timeout, appKey, model.modelId, model.companyIdentifier))
            }
        }
    }

    override fun setPrePeripheralMessage(
        group: Group, dimmer: Int, gesture: Int, distance: Int, sound: Int
    ) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val models = network?.getModels(group)
        if (models?.isNotEmpty() == true) {
            val model = models[0] as VendorModel
            val appKey = getAppKey(model.boundAppKeyIndexes[0])
            appKey?.let {
                sendMessage(
                    group, NodePrePeripheralMessageUnacked(
                        dimmer = dimmer, gesture = gesture, distance = distance, sound = sound,
                        appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier
                    )
                )
            }
        }
    }

    override fun setStepPeripheralMessage(
        group: Group, shape: Int, color: Int, led: Int
    ) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val models = network?.getModels(group)
        if (models?.isNotEmpty() == true) {
            val model = models[0] as VendorModel
            val appKey = getAppKey(model.boundAppKeyIndexes[0])
            appKey?.let {
                sendMessage(
                    group, NodeStepPeripheralMessageUnacked(
                        shape = shape, color = color, led = led,
                        appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier
                    )
                )
            }
        }
    }

    private fun identifyMessage(group: Group, appKey: ApplicationKey, modelId: Int, companyIdentifier: Int): NodeStepPeripheralMessageUnacked {
        var shape = ShapeParams.LETTER_A
        var color = ColorParams.COLOR_GREEN
        when {
            group.name.endsWith(GROUP_A) -> {
                shape = ShapeParams.LETTER_A
                color = ColorParams.COLOR_GREEN
            }
            group.name.endsWith(GROUP_B) -> {
                shape = ShapeParams.LETTER_B
                color = ColorParams.COLOR_RED
            }
            group.name.endsWith(GROUP_C) -> {
                shape = ShapeParams.LETTER_C
                color = ColorParams.COLOR_BLUE
            }
            group.name.endsWith(GROUP_D) -> {
                shape = ShapeParams.LETTER_D
                color = ColorParams.COLOR_YELLOW
            }
        }
        return NodeStepPeripheralMessageUnacked(
            shape, color, PeripheralParams.LED_PERMANENT, appKey, modelId, companyIdentifier
        )
    }

    private fun peripheralMessage(group: Group, message: NodeStepPeripheralMessageUnacked) {
        Log.d("TKUP-NEURAL::IDY::", message.toString())
        autoOffLedMessage(group, message)
    }

    override fun configMessage(
        group: Group,
        id: Int
    ) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val models = network?.getModels(group)
        if (models?.isNotEmpty() == true) {
            val model = models[0] as VendorModel
            val appKey = getAppKey(model.boundAppKeyIndexes[0])
            appKey?.let {
                sendMessage(group, NodeConfigMessageUnacked(id, appKey, model.modelId, model.companyIdentifier))
            }
        }
    }
}