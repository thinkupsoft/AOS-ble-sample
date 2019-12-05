package com.thinkup.connectivity.provisioning

import androidx.lifecycle.LiveData
import com.thinkup.connectivity.utils.ProvisionerStates
import com.thinkup.connectivity.utils.ProvisionerStates.*

class ProvisioningStatusLiveData : LiveData<ProvisioningStatusLiveData>() {
    private val provisioningProgress: MutableList<ProvisionerProgress> = mutableListOf()

    fun clear() {
        provisioningProgress.clear()
        postValue(this)
    }

    fun getStateList(): List<ProvisionerProgress>? {
        return provisioningProgress
    }


    fun getProvisionerProgress(): ProvisionerProgress? {
        return if (provisioningProgress.size == 0) null else provisioningProgress[provisioningProgress.size - 1]
    }

    fun onMeshNodeStateUpdated(state: ProvisionerStates?) {
        val provisioningProgress: ProvisionerProgress
        when (state) {
            PROVISIONING_INVITE -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_CAPABILITIES -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_START -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_PUBLIC_KEY_SENT -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_PUBLIC_KEY_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_AUTHENTICATION_STATIC_OOB_WAITING, PROVISIONING_AUTHENTICATION_OUTPUT_OOB_WAITING, PROVISIONING_AUTHENTICATION_INPUT_OOB_WAITING -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_AUTHENTICATION_INPUT_ENTERED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_INPUT_COMPLETE -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_CONFIRMATION_SENT -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_CONFIRMATION_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_RANDOM_SENT -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_RANDOM_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_DATA_SENT -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_COMPLETE -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONING_FAILED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            COMPOSITION_DATA_GET_SENT -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            COMPOSITION_DATA_STATUS_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            SENDING_DEFAULT_TTL_GET -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            DEFAULT_TTL_STATUS_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            SENDING_APP_KEY_ADD -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            APP_KEY_STATUS_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            SENDING_NETWORK_TRANSMIT_SET -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            NETWORK_TRANSMIT_STATUS_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            SENDING_BLOCK_ACKNOWLEDGEMENT -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            BLOCK_ACKNOWLEDGEMENT_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
            PROVISIONER_UNASSIGNED -> {
                provisioningProgress = ProvisionerProgress(state)
                this.provisioningProgress.add(provisioningProgress)
            }
        }
        postValue(this)
    }
}