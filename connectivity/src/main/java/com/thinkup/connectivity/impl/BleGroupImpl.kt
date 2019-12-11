package com.thinkup.connectivity.impl

import android.content.Context
import com.thinkup.connectivity.BleGroup
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.exceptions.AppKeyException
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.status.NodeGetMessage
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress

class BleGroupImpl(context: Context, repository: NrfMeshRepository) : BaseBleImpl(context, repository), BleGroup {

    override fun addGroup(name: String): Boolean {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val newGroup = network?.createGroup(network.selectedProvisioner, name)
        newGroup?.let {
            return network.addGroup(it)
        }
        return false
    }

    override fun removeGroup(group: Group): Boolean {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        return network?.removeGroup(group) == true
    }

    override fun getGroupNodes(group: Group): List<MeshModel> {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        return network?.getModels(group) ?: listOf()
    }

    override fun getStatus(group: Group, model: VendorModel) {
        val appKey = getAppKey(model.boundAppKeyIndexes[0] ?: 0)
        appKey?.let {
            sendMessage(group, NodeGetMessage(appKey, model.modelId, model.companyIdentifier))
        } ?: run {
            throw AppKeyException()
        }
    }

    override fun getTtl(group: Group) {
        val defaultTtlGet = ConfigDefaultTtlGet()
        sendMessage(group, defaultTtlGet)
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

    override fun removeGroupNode(group: Group, model: VendorModel) {
        val address: Int = group.address
        val meshNode = repository.getSelectedMeshNode()?.value
        if (meshNode != null) {
            val element: Element? = getElement(meshNode)
            if (element != null) {
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
}