package com.thinkup.connectivity.groups

import android.content.Context
import androidx.lifecycle.LiveData
import com.thinkup.connectivity.common.BaseBleViewModel
import com.thinkup.connectivity.exceptions.AppKeyException
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.status.NodeGetMessage
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress

class GroupViewModel(context: Context, repository: NrfMeshRepository) : BaseBleViewModel(context, repository) {

    fun getGroups(): LiveData<List<Group>> {
        return repository.getGroups()
    }

    fun addGroup(name: String): Boolean {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val newGroup = network?.createGroup(network.selectedProvisioner, name)
        newGroup?.let {
            return network.addGroup(it)
        }
        return false
    }

    fun removeGroup(group: Group): Boolean {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        return network?.removeGroup(group) == true
    }

    fun getGroupNodes(group: Group): List<MeshModel> {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        return network?.getModels(group) ?: listOf()
    }

    fun getStatus(group: Group, model: VendorModel) {
        val appKey = getAppKey(model?.boundAppKeyIndexes[0] ?: 0)
        appKey?.let {
            sendMessage(group, NodeGetMessage(appKey, model.modelId, model.companyIdentifier))
        } ?: run {
            throw AppKeyException()
        }
    }

    fun getTtl(group: Group) {
        val defaultTtlGet = ConfigDefaultTtlGet()
        sendMessage(group, defaultTtlGet)
    }

    fun addGroupNode(group: Group, meshNode: ProvisionedMeshNode?) {
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

    fun removeGroupNode(group: Group, model: VendorModel) {
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