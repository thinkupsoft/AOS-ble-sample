package com.thinkup.connectivity

import com.thinkup.connectivity.common.FastOptions
import com.thinkup.connectivity.common.TrainingCallback
import no.nordicsemi.android.meshprovisioner.Group

interface BleFastTraining: BleConnection {
    fun set(groups: List<Group>? = null, options: FastOptions, callback: TrainingCallback)
    fun startTraining()
    fun stopTraining()
}