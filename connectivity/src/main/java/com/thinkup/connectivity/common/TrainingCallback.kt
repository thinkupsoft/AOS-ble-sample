package com.thinkup.connectivity.common

import com.thinkup.connectivity.messges.EventType
import com.thinkup.connectivity.messges.event.NodeEventStatus
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface TrainingCallback {
    fun onSettingStart()
    fun onSettingComplete()
    fun onCountdown()
    fun onStartTraining()
    fun onAction(group: Group?, node: ProvisionedMeshNode?, nodeEventStatus: NodeEventStatus, event: EventType?, time: Long)
    fun onStopTraining()
    fun onCompleteTraining()
}