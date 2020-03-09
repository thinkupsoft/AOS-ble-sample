package com.thinkup.connectivity.impl

import android.content.Context
import android.util.Log
import com.thinkup.connectivity.BleGroup
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.exceptions.AppKeyException
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
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

    override fun addGroup(name: String): Boolean {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val newGroup = network?.createGroup(network.selectedProvisioner, name)
        newGroup?.let {
            if (network.addGroup(it)) {
                repository.getMeshManagerApi().addGroupDb(it)
                return true
            }
        }
        return false
    }

    override fun removeGroup(group: Group): Boolean {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        if (network?.removeGroup(group) == true) {
            repository.getMeshManagerApi().deleteGroupDb(group)
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
            group.ids.add(meshNode.nodeName.toInt())
            repository.getMeshManagerApi().updateGroupDb(group)
        }
    }

    override fun removeGroupNode(group: Group, meshNode: ProvisionedMeshNode) {
        group.ids.remove(meshNode.nodeName.toInt())
        repository.getMeshManagerApi().updateGroupDb(group)
    }

    override fun identify(groups: List<Group>) {
        bulkMessaging(groups) {
            identify(it)
        }
    }

    override fun identify(group: Group) {
        val ids = mutableListOf<Int>()
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val nodes = getGroupNodes(group)
        val models = network?.getModels(group)
        if (models?.isNotEmpty() == true) {
            val model = models[0] as VendorModel
            val appKey = getAppKey(model.boundAppKeyIndexes[0])
            appKey?.let {
                nodes.forEachIndexed { index, node ->
                    val id = node.nodeName.toInt()
                    ids.add(id)
                    sendBroadcastMessage(identifyMessage(group, index, id, appKey, model.modelId, model.companyIdentifier))
                }
                autoOffLedMessage(ids, appKey, model.modelId, model.companyIdentifier)
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
                sendBroadcastMessage(NodeControlMessageUnacked(params.toByte(), timeout, appKey, model.modelId, model.companyIdentifier, OpCodes.getGroupMask(group.ids)))
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
                sendBroadcastMessage(
                     NodePrePeripheralMessageUnacked(
                        dimmer = dimmer, gesture = gesture, distance = distance, sound = sound,
                        appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier, destination = OpCodes.getGroupMask(group.ids)
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
                sendBroadcastMessage(
                     NodeStepPeripheralMessageUnacked(
                        shape = shape, color = color, led = led,
                        appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier, destination =  OpCodes.getGroupMask(group.ids)
                    )
                )
            }
        }
    }

    private fun identifyMessage(
        group: Group,
        index: Int,
        id: Int,
        appKey: ApplicationKey,
        modelId: Int,
        companyIdentifier: Int
    ): NodeStepPeripheralMessageUnacked {
        val groups = repository.getGroups().value
        var position = 0
        groups?.let {
            for (i in it.indices) {
                if (groups[i].address == group.address) {
                    position = i
                    break
                }
            }
        }
        val color = when (position) {
            0 -> ColorParams.COLOR_GREEN
            1 -> ColorParams.COLOR_RED
            2 -> ColorParams.COLOR_BLUE
            else -> ColorParams.COLOR_YELLOW
        }
        val shape = when (index + 1) {
            1 -> ShapeParams.NUMBER_1
            2 -> ShapeParams.NUMBER_2
            3 -> ShapeParams.NUMBER_3
            4 -> ShapeParams.NUMBER_4
            5 -> ShapeParams.NUMBER_5
            6 -> ShapeParams.NUMBER_6
            7 -> ShapeParams.NUMBER_7
            8 -> ShapeParams.NUMBER_8
            9 -> ShapeParams.NUMBER_9
            else -> ShapeParams.NUMBER_0
        }
        return NodeStepPeripheralMessageUnacked(
            shape, color, PeripheralParams.LED_PERMANENT, appKey, modelId, companyIdentifier, OpCodes.getUnicastMask(id)
        )
    }

//    private fun peripheralMessage(group: Group, message: NodeStepPeripheralMessageUnacked) {
//        Log.d("TKUP-NEURAL::IDY::", message.toString())
//        autoOffLedMessage(group, message)
//    }

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