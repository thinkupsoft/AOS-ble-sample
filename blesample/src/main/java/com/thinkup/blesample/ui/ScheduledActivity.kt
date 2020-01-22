package com.thinkup.blesample.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.thinkup.blesample.R
import com.thinkup.connectivity.BleScheduleTraining
import com.thinkup.connectivity.common.*
import com.thinkup.connectivity.messges.ColorParams
import com.thinkup.connectivity.messges.EventType
import com.thinkup.connectivity.messges.PeripheralParams
import com.thinkup.connectivity.messges.ShapeParams
import com.thinkup.connectivity.messges.event.NodeEventStatus
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.ext.android.inject
import java.util.*

class ScheduledActivity : AppCompatActivity(), TrainingCallback {

    val bleScheduleTraining: BleScheduleTraining by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduled)

        val options = ScheduleOptions(
            listOf(
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.CROSS, ShapeParams.ARROW_UP), ColorParams.COLOR_BLUE, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(1, listOf(ShapeParams.LETTER_A), ColorParams.COLOR_YELLOW, PeripheralParams.LED_PERMANENT)
                    ),
                    2000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.X, ShapeParams.ARROW_DOWN), ColorParams.COLOR_BLUE, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(1, listOf(ShapeParams.LETTER_B), ColorParams.COLOR_WITHE, PeripheralParams.LED_PERMANENT)
                    ),
                    2000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.CIRCLE, ShapeParams.ARROW_LEFT), ColorParams.COLOR_BLUE, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(1, listOf(ShapeParams.LETTER_C), ColorParams.COLOR_CYAN, PeripheralParams.LED_PERMANENT)
                    ),
                    2000, 100
                )
            ),
            StarterMethod.INMEDIATELY,
            false, false, 1, 0, true
        )

        bleScheduleTraining.set(bleScheduleTraining.getGroups().value!!, options, this)
    }

    override fun onSettingStart() {

    }

    override fun onSettingComplete() {
        bleScheduleTraining.startTraining()
    }

    override fun onCountdown() {

    }

    override fun onStartTraining() {

    }

    override fun onAction(group: Group?, node: ProvisionedMeshNode?, nodeEventStatus: NodeEventStatus, event: EventType?, time: Long) {

    }

    override fun onStopTraining() {

    }

    override fun onCompleteTraining() {

    }
}
