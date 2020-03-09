package com.thinkup.connectivity.impl

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thinkup.connectivity.BleNode
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.mesh.NodeCallback
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.ColorParams
import com.thinkup.connectivity.messges.OpCodes
import com.thinkup.connectivity.messges.PeripheralParams
import com.thinkup.connectivity.messges.ShapeParams
import com.thinkup.connectivity.messges.config.NodeConfigMessage
import com.thinkup.connectivity.messges.config.NodeConfigMessageUnacked
import com.thinkup.connectivity.messges.control.NodeControlMessage
import com.thinkup.connectivity.messges.peripheral.NodePrePeripheralMessage
import com.thinkup.connectivity.messges.peripheral.NodePrePeripheralMessageUnacked
import com.thinkup.connectivity.messges.peripheral.NodeStepPeripheralMessage
import com.thinkup.connectivity.messges.peripheral.NodeStepPeripheralMessageUnacked
import com.thinkup.connectivity.messges.status.NodeGetMessage
import com.thinkup.connectivity.utils.TimeoutLiveData
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.models.VendorModel
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

    override fun controlMessage(node: ProvisionedMeshNode, params: Int, timeout: Int, ack: Boolean) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(
                        node,
                        if (ack) NodeControlMessage(params.toByte(), timeout, appKey, model.modelId, model.companyIdentifier)
                        else NodeControlMessage(params.toByte(), timeout, appKey, model.modelId, model.companyIdentifier)
                    )
                }
            }
        }
    }

    override fun configMessage(
        node: ProvisionedMeshNode,
        id: Int,
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
                        if (ack) NodeConfigMessage(id, appKey, model.modelId, model.companyIdentifier)
                        else NodeConfigMessageUnacked(id, appKey, model.modelId, model.companyIdentifier)
                    )
                }
            }
        }
    }

    override fun setPrePeripheralMessage(
        node: ProvisionedMeshNode, dimmer: Int, gesture: Int, distance: Int, sound: Int, ack: Boolean
    ) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(
                        node,
                        if (ack) NodePrePeripheralMessage(
                            dimmer = dimmer, gesture = gesture, distance = distance, sound = sound,
                            appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier
                        )
                        else NodePrePeripheralMessageUnacked(
                            dimmer = dimmer, gesture = gesture, distance = distance, sound = sound,
                            appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier
                        )
                    )
                }
            }
        }
    }

    override fun setStepPeripheralMessage(
        node: ProvisionedMeshNode, shape: Int, color: Int, led: Int, ack: Boolean
    ) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(
                        node,
                        if (ack) NodeStepPeripheralMessage(
                            shape = shape, color = color, led = led,
                            appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier
                        )
                        else NodeStepPeripheralMessageUnacked(
                            shape = shape, color = color, led = led,
                            appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier
                        )
                    )
                }
            }
        }
    }

    override fun identify(nodes: List<ProvisionedMeshNode>) {
        nodes.forEach {
            executeService {
                identify(it)
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
                    peripheralMessage(node, identifyMessage(node, appKey, model.modelId, model.companyIdentifier))
                }
            }
        }
    }

    override fun showBatteryPercentage(node: ProvisionedMeshNode) {
        val color = getColorOfBattery(node.batteryLevel)
        val shape = getShapeOfId(node.nodeName.toInt())
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    val msg = NodeStepPeripheralMessageUnacked(
                        shape, color, PeripheralParams.LED_PERMANENT, appKey, model.modelId, model.companyIdentifier, OpCodes.getUnicastMask(node.nodeName.toInt()))
                    peripheralMessage(node, msg)
                }
            }
        }
    }

    private fun getColorOfBattery(batteryLevel: Int): Int {
        return when {
            batteryLevel <= 15 -> ColorParams.COLOR_RED
            batteryLevel <= 45 -> ColorParams.COLOR_YELLOW
            else -> ColorParams.COLOR_GREEN
        }
    }

    private fun getShapeOfId(id: Int): Int {
        return when (id) {
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

    }

    private fun identifyMessage(
        node: ProvisionedMeshNode,
        appKey: ApplicationKey,
        modelId: Int,
        companyIdentifier: Int
    ): NodeStepPeripheralMessageUnacked {
        var shape = ShapeParams.NUMBER_0
        val color = ColorParams.COLOR_GREEN
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
        return NodeStepPeripheralMessageUnacked(
            shape, color, PeripheralParams.LED_PERMANENT, appKey, modelId, companyIdentifier, OpCodes.getUnicastMask(node.nodeName.toInt())
        )
    }

    private fun peripheralMessage(node: ProvisionedMeshNode, message: NodeStepPeripheralMessageUnacked) {
        Log.d("TKUP-NEURAL::IDY::", message.toString())
        autoOffLedMessage(node, message)
    }

}