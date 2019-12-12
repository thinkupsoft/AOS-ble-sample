package com.thinkup.connectivity.mesh

import android.content.Context
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.MeshModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

fun NrfMeshRepository.resetNode() {
    nodeCallback?.onDelete()
}

fun NrfMeshRepository.provision(
    context: Context,
    device: ExtendedBluetoothDevice,
    provisionCallback: ProvisionCallback
) {
    this.provisionCallback = provisionCallback
    connect(context, device, false)
}

fun NrfMeshRepository.deviceReadyProvision() {
    provisionCallback?.deviceReady()
}

fun NrfMeshRepository.deviceFailProvision() {
    provisionCallback?.deviceFail()
}

fun NrfMeshRepository.identifyProvision(node: UnprovisionedMeshNode) {
    provisionCallback?.identify(node)
}

fun NrfMeshRepository.provisionFail() {
    provisionCallback?.provisionFail()
}

fun NrfMeshRepository.provisionComplete(meshNode: ProvisionedMeshNode) {
    provisionCallback?.provisionComplete(meshNode)
}

fun NrfMeshRepository.bindNodeKeyComplete(meshNode: ProvisionedMeshNode) {
    provisionCallback?.bindNodeKeyComplete(meshNode)
}

fun NrfMeshRepository.bindAppKeyComplete(meshNode: ProvisionedMeshNode, element: Element, model: MeshModel) {
    provisionCallback?.bindAppKeyComplete(meshNode, element, model)
}

fun NrfMeshRepository.setPublicationComplete(meshNode: ProvisionedMeshNode, element: Element, model: MeshModel) {
    provisionCallback?.setPublicationComplete(meshNode, element, model)
}

interface ProvisionCallback {
    fun deviceReady()
    fun deviceFail()
    fun identify(node: UnprovisionedMeshNode)
    fun provisionFail()
    fun provisionComplete(meshNode: ProvisionedMeshNode)
    fun bindNodeKeyComplete(meshNode: ProvisionedMeshNode)
    fun bindAppKeyComplete(meshNode: ProvisionedMeshNode, element: Element, model: MeshModel)
    fun setPublicationComplete(meshNode: ProvisionedMeshNode, element: Element, model: MeshModel)
}

interface NodeCallback {
    fun onDelete()
}