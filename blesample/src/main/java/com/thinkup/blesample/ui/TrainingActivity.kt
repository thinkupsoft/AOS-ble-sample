package com.thinkup.blesample.ui

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.blesample.renderers.EventRenderer
import com.thinkup.connectivity.BleTraining
import com.thinkup.connectivity.common.FastOptions
import com.thinkup.connectivity.messges.ColorParams
import com.thinkup.connectivity.messges.PeripheralParams
import com.thinkup.connectivity.messges.ShapeParams
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_group_trianing.*
import kotlinx.android.synthetic.main.activity_group_trianing.sound
import kotlinx.android.synthetic.main.activity_group_trianing.timeout
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.ext.android.inject

class TrainingActivity : BaseActivity(), BleTraining.TrainingCallback {

    private val bleTraining: BleTraining by inject()
    private val adapter = RendererAdapter()
    private val list = mutableListOf<NodeEventStatus>()

    private val timeoutValues = getRange(0.0, 5.0, 0.5)
    private val delayValues = getRange(0.1, 5.0, 0.1)
    private val touchesValues = (1..20 step 1).map { it.toString() }.toTypedArray()

    override fun title(): String = "Training"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_trianing)

        timeout.displayedValues = timeoutValues
        timeout.maxValue = timeoutValues.size - 1
        timeout.minValue = 0
        timeout.wrapSelectorWheel = false
        delay.displayedValues = delayValues
        delay.maxValue = delayValues.size - 1
        delay.minValue = 0
        delay.wrapSelectorWheel = false
        touches.displayedValues = touchesValues
        touches.maxValue = touchesValues.size - 1
        touches.minValue = 0
        touches.wrapSelectorWheel = false

        executeTraining.setOnClickListener { execute() }
        messages()
    }

    private fun messages() {
        adapter.addRenderer(EventRenderer())
        trainingList.adapter = adapter
        trainingList.layoutManager = LinearLayoutManager(this)
        trainingList.isNestedScrollingEnabled = false
    }

    private fun execute() {
        val opTouches = touchesValues[touches.value].toInt()
        val opTimeout = timeoutValues[timeout.value].toDouble() * 1000
        val opDelay = delayValues[delay.value].toDouble() * 1000
        val options = FastOptions(
            opTouches, opTimeout.toInt(), opDelay.toInt(),
            listOf(ShapeParams.CIRCLE), listOf(ColorParams.COLOR_RED),
            countdown.isChecked, if (ledmode.isChecked) PeripheralParams.LED_FLASH else PeripheralParams.LED_PERMANENT,
            sound.isChecked, endlight.isChecked
        )
        actualConfig.text = "ACTUAL CONFIG :: $options"

        val groups = listOf<Group>(bleTraining.getGroups().value!![1])
        bleTraining.set(
            groups = groups,
            options = options,
            callback = this
        )
    }

    private fun getRange(min: Double, max: Double, step: Double): Array<String> {
        val minInt = (min * 10).toInt()
        val maxInt = (max * 10).toInt()
        val stepInt = (step * 10).toInt()
        val range = (minInt..maxInt step stepInt)
        return range.map { (it / 10.0).toString() }.toTypedArray()
    }

    override fun onSettingStart() {

    }

    override fun onSettingComplete() {
        bleTraining.startTraining()
    }

    override fun onCountdown() {
    }

    override fun onAction(group: Group?, node: ProvisionedMeshNode?, event: NodeEventStatus) {
        list.add(event)
        adapter.setItems(list)
    }

    override fun onStartTraining() {
        list.clear()
        adapter.setItems(list)
    }

    override fun onStopTraining() {
    }

    override fun onCompleteTraining() {
        Toast.makeText(this, "Complete", Toast.LENGTH_SHORT).show()
    }
}