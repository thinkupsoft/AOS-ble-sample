package com.thinkup.connectivity

import com.thinkup.connectivity.common.ScheduleOptions
import com.thinkup.connectivity.common.TrainingCallback
import no.nordicsemi.android.meshprovisioner.Group

interface BleScheduleTraining : BleConnection {
    fun set(groups: List<Group>?, options: ScheduleOptions, callback: TrainingCallback)
    fun startTraining()
    fun stopTraining()
}