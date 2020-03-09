package com.thinkup.blesample.ui

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.blesample.renderers.EventRenderer
import com.thinkup.connectivity.BleScheduleTraining
import com.thinkup.connectivity.common.*
import com.thinkup.connectivity.messges.ColorParams
import com.thinkup.connectivity.messges.EventType
import com.thinkup.connectivity.messges.PeripheralParams
import com.thinkup.connectivity.messges.ShapeParams
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_scheduled.*
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.ext.android.inject

class ScheduledActivity : BaseActivity(), TrainingCallback {

    val bleScheduleTraining: BleScheduleTraining by inject()
    private val adapter = RendererAdapter()
    private val list = mutableListOf<NodeEventStatus>()

    override fun title(): String = "Scheduled Training"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduled)

        executeTraining1.setOnClickListener { training1() }
        executeTraining2.setOnClickListener { training2() }
        executeTraining3.setOnClickListener { training3() }
        executeTraining4.setOnClickListener { training4() }
        messages()
    }

    override fun onStop() {
        super.onStop()
        bleScheduleTraining.stopTraining()
    }

    private fun messages() {
        adapter.addRenderer(EventRenderer())
        trainingList.adapter = adapter
        trainingList.layoutManager = LinearLayoutManager(this)
        trainingList.isNestedScrollingEnabled = false
    }

    private fun training1() {
        // Grupos de minimo dos nodos (1-2)\n3 pasos
        val options = ScheduleOptions(
            listOf(
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.CROSS, ShapeParams.ARROW_UP), ColorParams.COLOR_BLUE, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(1, listOf(ShapeParams.LETTER_A), ColorParams.COLOR_YELLOW, PeripheralParams.LED_PERMANENT)
                    ),
                    4000, 100
                ),
                StepOption(
                    listOf(
                        //StepNodeConfig(0, listOf(ShapeParams.X, ShapeParams.ARROW_DOWN), ColorParams.COLOR_BLUE, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(1, listOf(ShapeParams.LETTER_B), ColorParams.COLOR_WITHE, PeripheralParams.LED_PERMANENT)
                    ),
                    6000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.CIRCLE, ShapeParams.ARROW_LEFT), ColorParams.COLOR_BLUE, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(1, listOf(ShapeParams.LETTER_C), ColorParams.COLOR_CYAN, PeripheralParams.LED_PERMANENT)
                    ),
                    4000, 100
                )
            ),
            StarterMethod.INMEDIATELY,
            false, false, 1, 0, false
        )
        val groups = bleScheduleTraining.getGroups().value!!

        val validGroups = groups.filter {
            val nodes = bleScheduleTraining.getGroupNodes(it)
            nodes.size >= 2
        }

        if (validGroups.isNotEmpty()) bleScheduleTraining.set(validGroups, options, this)
        else Toast.makeText(this, "Don't have any group with 2 nodes", Toast.LENGTH_SHORT).show()
    }

    private fun training2() {
        // Grupos de minimo tres nodos (1-2-3)\n3 pasos
        val options = ScheduleOptions(
            listOf(
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_1, ShapeParams.NUMBER_4), ColorParams.COLOR_BLUE, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(1, listOf(ShapeParams.NUMBER_2), ColorParams.COLOR_YELLOW, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(2, listOf(ShapeParams.NUMBER_3), ColorParams.COLOR_RED, PeripheralParams.LED_PERMANENT)
                    ),
                    3000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(1, listOf(ShapeParams.CROSS), ColorParams.COLOR_GREEN, PeripheralParams.LED_PERMANENT)
                    ),
                    1000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.CIRCLE), ColorParams.COLOR_CYAN, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(2, listOf(ShapeParams.SQUARE), ColorParams.COLOR_CYAN, PeripheralParams.LED_PERMANENT)
                    ),
                    4000, 100
                )
            ),
            StarterMethod.COUNTDOWN,
            false, false, 1, 0, true
        )
        val groups = bleScheduleTraining.getGroups().value!!

        val validGroups = groups.filter {
            val nodes = bleScheduleTraining.getGroupNodes(it)
            nodes.size >= 3
        }

        if (validGroups.isNotEmpty()) bleScheduleTraining.set(validGroups, options, this)
        else Toast.makeText(this, "Don't have any group with 3 nodes", Toast.LENGTH_SHORT).show()
    }

    private fun training3() {
        //Grupos de minimo un nodo (1)\n10 pasos
        val options = ScheduleOptions(
            listOf(
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_0), ColorParams.COLOR_RED, PeripheralParams.LED_FAST_FLASH)
                    ),
                    4000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_1), ColorParams.COLOR_GREEN, PeripheralParams.LED_FAST_FLASH)
                    ),
                    4000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_2), ColorParams.COLOR_CYAN, PeripheralParams.LED_FAST_FLASH)
                    ),
                    4000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_3), ColorParams.COLOR_RED, PeripheralParams.LED_FAST_FLASH)
                    ),
                    4000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_4), ColorParams.COLOR_GREEN, PeripheralParams.LED_FAST_FLASH)
                    ),
                    1000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_5), ColorParams.COLOR_CYAN, PeripheralParams.LED_FAST_FLASH)
                    ),
                    1000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_6), ColorParams.COLOR_RED, PeripheralParams.LED_FAST_FLASH)
                    ),
                    1000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_7), ColorParams.COLOR_GREEN, PeripheralParams.LED_FAST_FLASH)
                    ),
                    1000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_8), ColorParams.COLOR_CYAN, PeripheralParams.LED_FAST_FLASH)
                    ),
                    1000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.NUMBER_9), ColorParams.COLOR_RED, PeripheralParams.LED_FAST_FLASH)
                    ),
                    1000, 100
                )

            ),
            StarterMethod.DEACTIVATION,
            true, true, 1, 0, false
        )
        val groups = bleScheduleTraining.getGroups().value!!

        val validGroups = groups.filter {
            val nodes = bleScheduleTraining.getGroupNodes(it)
            nodes.size >= 1
        }

        if (validGroups.isNotEmpty()) bleScheduleTraining.set(validGroups, options, this)
        else Toast.makeText(this, "Don't have any group with 1 node", Toast.LENGTH_SHORT).show()
    }

    private fun training4() {
        //Grupos de minimo cuatro nodos (1-3-4)\n4 pasos
        val options = ScheduleOptions(
            listOf(
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.CIRCLE, ShapeParams.SQUARE), ColorParams.COLOR_BLUE, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(1, listOf(ShapeParams.CIRCLE, ShapeParams.SQUARE), ColorParams.COLOR_YELLOW, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(3, listOf(ShapeParams.CIRCLE, ShapeParams.SQUARE), ColorParams.COLOR_RED, PeripheralParams.LED_PERMANENT)
                    ),
                    3000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(1, listOf(ShapeParams.ARROW_DOWN), ColorParams.COLOR_GREEN, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(3, listOf(ShapeParams.ARROW_UP), ColorParams.COLOR_GREEN, PeripheralParams.LED_PERMANENT)
                    ),
                    1000, 100
                ),
                StepOption(
                    listOf(
                        StepNodeConfig(0, listOf(ShapeParams.ARROW_DOWN), ColorParams.COLOR_YELLOW, PeripheralParams.LED_PERMANENT),
                        StepNodeConfig(2, listOf(ShapeParams.ARROW_UP), ColorParams.COLOR_YELLOW, PeripheralParams.LED_PERMANENT)
                    ),
                    4000, 100
                )
            ),
            StarterMethod.COUNTDOWN,
            false, false, 1, 0, false
        )
        val groups = bleScheduleTraining.getGroups().value!!

        val validGroups = groups.filter {
            val nodes = bleScheduleTraining.getGroupNodes(it)
            nodes.size >= 4
        }

        if (validGroups.isNotEmpty()) bleScheduleTraining.set(validGroups, options, this)
        else Toast.makeText(this, "Don't have any group with 4 nodes", Toast.LENGTH_SHORT).show()
    }

    override fun onSettingStart() {

    }

    override fun onSettingComplete() {
        bleScheduleTraining.startTraining()
    }

    override fun onCountdown() {
    }

    override fun onAction(group: Group?, node: ProvisionedMeshNode?, nodeEventStatus: NodeEventStatus, event: EventType?, time: Long) {
        runOnUiThread {
            list.add(nodeEventStatus)
            adapter.setItems(list)
        }
    }

    override fun onStartTraining() {
        runOnUiThread {
            list.clear()
            adapter.setItems(list)
        }
    }

    override fun onStopTraining() {
        runOnUiThread {
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCompleteTraining() {
        runOnUiThread {
            Toast.makeText(this, "Complete", Toast.LENGTH_SHORT).show()
        }
    }
}
