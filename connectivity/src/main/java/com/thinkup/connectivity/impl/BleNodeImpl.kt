package com.thinkup.connectivity.impl

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thinkup.connectivity.BleNode
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.mesh.NodeCallback
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.ColorParams
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.PeripheralParams
import com.thinkup.connectivity.messges.ShapeParams
import com.thinkup.connectivity.messges.config.NodeConfigMessage
import com.thinkup.connectivity.messges.config.NodeConfigMessageUnacked
import com.thinkup.connectivity.messges.control.NodeControlMessage
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessage
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessageUnacked
import com.thinkup.connectivity.messges.status.NodeGetMessage
import com.thinkup.connectivity.utils.TimeoutLiveData
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.ConfigDefaultTtlGet
import no.nordicsemi.android.meshprovisioner.transport.ConfigNodeReset
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class BleNodeImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BaseBleImpl(context, setting, repository), BleNode {

    companion object {
        const val NUMBER_0 = '0'
        const val NUMBER_1 = '1'
        const val NUMBER_2 = '2'
        const val NUMBER_3 = '3'
        const val NUMBER_4 = '4'
        const val NUMBER_5 = '5'
        const val NUMBER_6 = '6'
        const val NUMBER_7 = '7'
        const val NUMBER_8 = '8'
        const val NUMBER_9 = '9'
    }

    private fun resetNode(node: ProvisionedMeshNode, result: MutableLiveData<Boolean>) {
        repository.nodeCallback = object : NodeCallback {
            override fun onDelete() {
                result.postValue(true)
                deleteDB(node)
            }
        }
        val configNodeReset = ConfigNodeReset()
        sendMessage(node, configNodeReset)
    }

    override fun deleteDB(node: ProvisionedMeshNode) {
        repository.getMeshNetworkLiveData().getMeshNetwork()?.deleteNode(node)
    }

    override fun delete(node: ProvisionedMeshNode): LiveData<Boolean> {
        val result = TimeoutLiveData(ACTION_TIMEOUT, false)
        repository.setSelectedMeshNode(node)
        resetNode(node, result)
        return result
    }

    override fun getStatus(node: ProvisionedMeshNode) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(node, NodeGetMessage(appKey, model.modelId, model.companyIdentifier))
                }
            }
        }
    }

    override fun controlMessage(node: ProvisionedMeshNode, params: Int, ack: Boolean) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(
                        node,
                        if (ack) NodeControlMessage(params, appKey, model.modelId, model.companyIdentifier)
                        else NodeControlMessage(params, appKey, model.modelId, model.companyIdentifier)
                    )
                }
            }
        }
    }

    override fun configMessage(
        node: ProvisionedMeshNode,
        id: Int,
        timeoutConfig: Int,
        timeout: Int,
        ack: Boolean
    ) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(
                        node,
                        if (ack) NodeConfigMessage(id, timeoutConfig, timeout, appKey, model.modelId, model.companyIdentifier)
                        else NodeConfigMessageUnacked(id, timeoutConfig, timeout, appKey, model.modelId, model.companyIdentifier)
                    )
                }
            }
        }
    }

    override fun setPeripheralMessage(
        node: ProvisionedMeshNode,
        shape: Int, color: Int, dimmer: Int, led: Int,
        fill: Int, gesture: Int, distance: Int, filter: Int, touch: Int, sound: Int,
        ack: Boolean
    ) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(
                        node,
                        if (ack) NodePeripheralMessage(
                            shape, color, dimmer = dimmer, led = led, fill = fill, gesture = gesture,
                            distance = distance, filter = filter, touch = touch, sound = sound,
                            appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier
                        )
                        else NodePeripheralMessageUnacked(
                            shape, color, dimmer = dimmer, led = led, fill = fill, gesture = gesture,
                            distance = distance, filter = filter, touch = touch, sound = sound,
                            appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier
                        )
                    )
                }
            }
        }
    }

    override fun identify(nodes: List<ProvisionedMeshNode>) {
        executeService {
            nodes.forEach {
                identify(it)
                delay(BULK_DELAY)
            }
        }
    }

    override fun identify(node: ProvisionedMeshNode) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    executeService {
                        peripheralMessage(node, identifyMessage(node, appKey, model.modelId, model.companyIdentifier))
                    }
                }
            }
        }
    }

    private fun identifyMessage(
        node: ProvisionedMeshNode,
        appKey: ApplicationKey,
        modelId: Int,
        companyIdentifier: Int
    ): NodePeripheralMessageUnacked {
        var shape = ShapeParams.NUMBER_0
        var color = ColorParams.COLOR_GREEN
        when {
            node.nodeName.endsWith(NUMBER_0) -> shape = ShapeParams.NUMBER_0
            node.nodeName.endsWith(NUMBER_1) -> shape = ShapeParams.NUMBER_1
            node.nodeName.endsWith(NUMBER_2) -> shape = ShapeParams.NUMBER_2
            node.nodeName.endsWith(NUMBER_3) -> shape = ShapeParams.NUMBER_3
            node.nodeName.endsWith(NUMBER_4) -> shape = ShapeParams.NUMBER_4
            node.nodeName.endsWith(NUMBER_5) -> shape = ShapeParams.NUMBER_5
            node.nodeName.endsWith(NUMBER_6) -> shape = ShapeParams.NUMBER_6
            node.nodeName.endsWith(NUMBER_7) -> shape = ShapeParams.NUMBER_7
            node.nodeName.endsWith(NUMBER_8) -> shape = ShapeParams.NUMBER_8
            node.nodeName.endsWith(NUMBER_9) -> shape = ShapeParams.NUMBER_9
        }
        return NodePeripheralMessageUnacked(
            shape, color, NO_CONFIG, NO_CONFIG, NO_CONFIG, NO_CONFIG, PeripheralParams.LED_PERMANENT,
            NO_CONFIG, NO_CONFIG, NO_CONFIG, NO_CONFIG, NO_CONFIG, NO_CONFIG, appKey, modelId, companyIdentifier
        )
    }

    private fun peripheralMessage(node: ProvisionedMeshNode, message: NodePeripheralMessageUnacked) {
        sendMessage(node, message)
    }
}