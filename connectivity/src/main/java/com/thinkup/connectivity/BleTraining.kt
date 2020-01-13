package com.thinkup.connectivity

import com.thinkup.connectivity.common.FastOptions
import com.thinkup.connectivity.messges.event.NodeEventStatus
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface BleTraining: BleConnection {
    fun set(groups: List<Group>? = null, options: FastOptions, callback: TrainingCallback)
    fun startTraining()

    interface TrainingCallback {
        fun onSettingStart()
        fun onSettingComplete()
        fun onCountdown()
        fun onStartTraining()
        fun onAction(group: Group?, node: ProvisionedMeshNode?, event: NodeEventStatus)
        fun onStopTraining()
        fun onCompleteTraining()
    }
}