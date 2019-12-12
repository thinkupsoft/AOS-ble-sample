package com.thinkup.connectivity.impl

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thinkup.connectivity.BleNode
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.mesh.NodeCallback
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.config.NodeConfigMessage
import com.thinkup.connectivity.messges.control.NodeControlMessage
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessage
import com.thinkup.connectivity.messges.status.NodeGetMessage
import com.thinkup.connectivity.utils.TimeoutLiveData
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*

class BleNodeImpl(context: Context, repository: NrfMeshRepository) : BaseBleImpl(context, repository), BleNode {

    private fun resetNode(node: ProvisionedMeshNode, result: MutableLiveData<Boolean>) {
        repository.nodeCallback = object : NodeCallback {
            override fun onDelete() {
                result.postValue(true)
                repository.getMeshNetworkLiveData().getMeshNetwork()?.deleteNode(node) ?: false
            }
        }
        val configNodeReset = ConfigNodeReset()
        sendMessage(node, configNodeReset)
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

    override fun getTtl(node: ProvisionedMeshNode) {
        val defaultTtlGet = ConfigDefaultTtlGet()
        sendMessage(node, defaultTtlGet)
    }

    override fun controlMessage(node: ProvisionedMeshNode, params: Int) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(node, NodeControlMessage(params, appKey, model.modelId, model.companyIdentifier))
                }
            }
        }
    }

    override fun configMessage(
        node: ProvisionedMeshNode,
        id: Int,
        timeoutConfig: Int,
        timeout: Int
    ) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(node, NodeConfigMessage(id, timeoutConfig, timeout, appKey, model.modelId, model.companyIdentifier))
                }
            }
        }
    }

    override fun setPeripheralMessage(
        node: ProvisionedMeshNode,
        shape: Int, color: Int, dimmer: Int, led: Int,
        fill: Int, gesture: Int, distance: Int, filter: Int, touch: Int, sound: Int
    ) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(
                        node, NodePeripheralMessage(
                            shape, color, dimmer = dimmer, led = led, fill = fill, gesture = gesture,
                            distance = distance, filter = filter, touch = touch, sound = sound,
                            appKey = appKey, modelId = model.modelId, compId = model.companyIdentifier
                        )
                    )
                }
            }
        }
    }
}