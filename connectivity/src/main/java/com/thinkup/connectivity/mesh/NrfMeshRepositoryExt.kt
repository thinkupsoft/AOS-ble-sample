package com.thinkup.connectivity.mesh

import android.content.Context
import android.os.Handler
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

fun NrfMeshRepository.resetNode() {
    isSending = false
    nodeCallback?.onDelete()
}

fun NrfMeshRepository.provision(
    context: Context,
    device: ExtendedBluetoothDevice,
    provisionCallback: ProvisionCallback
) {
    isSending = true
    this.provisionCallback = provisionCallback
    disconnect()
    Handler().postDelayed({ connect(context, device, false) }, 1000)
}

fun NrfMeshRepository.deviceReadyProvision(isProvisioning: Boolean) {
    this.isSending = isProvisioning
    provisionCallback?.deviceReady()
}

fun NrfMeshRepository.deviceFailProvision(isProvisioning: Boolean) {
    this.isSending = isProvisioning
    provisionCallback?.deviceFail()
}

fun NrfMeshRepository.identifyProvision(node: UnprovisionedMeshNode) {
    isSending = true
    provisionCallback?.identify(node)
}

fun NrfMeshRepository.provisionFail() {
    isSending = false
    provisionCallback?.provisionFail()
}

fun NrfMeshRepository.provisionComplete(meshNode: ProvisionedMeshNode) {
    isSending = true
    provisionCallback?.provisionComplete(meshNode)
}

fun NrfMeshRepository.bindNodeKeyComplete(meshNode: ProvisionedMeshNode) {
    isSending = true
    provisionCallback?.bindNodeKeyComplete(meshNode)
}

fun NrfMeshRepository.bindAppKeyComplete(meshNode: ProvisionedMeshNode) {
    isSending = true
    provisionCallback?.bindAppKeyComplete(meshNode)
}

fun NrfMeshRepository.setPublicationComplete(meshNode: ProvisionedMeshNode) {
    provisionCallback?.setPublicationComplete(meshNode)
}

interface ProvisionCallback {
    fun deviceReady()
    fun deviceFail()
    fun identify(node: UnprovisionedMeshNode)
    fun provisionFail()
    fun provisionComplete(meshNode: ProvisionedMeshNode)
    fun bindNodeKeyComplete(meshNode: ProvisionedMeshNode)
    fun bindAppKeyComplete(meshNode: ProvisionedMeshNode)
    fun setPublicationComplete(meshNode: ProvisionedMeshNode)
}

interface NodeCallback {
    fun onDelete()
}