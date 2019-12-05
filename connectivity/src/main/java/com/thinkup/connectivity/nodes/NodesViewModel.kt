package com.thinkup.connectivity.nodes

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.thinkup.connectivity.common.BaseBleViewModel
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.config.NodeConfigMessage
import com.thinkup.connectivity.messges.control.NodeControlMessage
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessage
import com.thinkup.connectivity.messges.status.NodeGetMessage
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*

class NodesViewModel(context: Context, repository: NrfMeshRepository) : BaseBleViewModel(context, repository) {

    private fun resetNode(node: ProvisionedMeshNode): LiveData<MeshMessage?> {
        val configNodeReset = ConfigNodeReset()
        sendMessage(node, configNodeReset)
        return repository.getMeshMessageLiveData()
    }

    fun delete(lco: LifecycleOwner, node: ProvisionedMeshNode): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        repository.setSelectedMeshNode(node)
        resetNode(node).observe(lco, Observer {
            if (it !is ConfigNodeResetStatus) result.postValue(false)
            else result.postValue(
                repository.getMeshNetworkLiveData().getMeshNetwork()?.deleteNode(node) ?: false
            )
        })
        return result
    }

    fun getStatus(node: ProvisionedMeshNode) {
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

    fun getTtl(node: ProvisionedMeshNode) {
        val defaultTtlGet = ConfigDefaultTtlGet()
        sendMessage(node, defaultTtlGet)
    }

    fun controlMessage(node: ProvisionedMeshNode, params: Int) {
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

    fun configMessage(
        node: ProvisionedMeshNode,
        id: Int = NO_CONFIG,
        timeoutConfig: Int = NO_CONFIG,
        timeout: Int = NO_CONFIG,
        flow: Int = NO_CONFIG
    ) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendMessage(node, NodeConfigMessage(id, timeoutConfig, timeout, flow, appKey, model.modelId, model.companyIdentifier))
                }
            }
        }
    }

    fun setPeripheralMessage(
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