package com.thinkup.connectivity.provisioning

import com.thinkup.connectivity.utils.ProvisionerStates

data class ProvisionerProgress(
    val state: ProvisionerStates? = null,
    val statusReceived: Int = 0
)