package com.thinkup.blesample.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.blesample.Utils
import com.thinkup.blesample.renderers.SetupRenderer
import com.thinkup.connectivity.BleNode
import com.thinkup.connectivity.messges.ColorParams
import com.thinkup.connectivity.messges.PeripheralParams
import com.thinkup.connectivity.messges.ShapeParams
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.setup.TrainSetup
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_group_trianing.*
import kotlinx.android.synthetic.main.activity_setup_trianing.*
import kotlinx.android.synthetic.main.activity_setup_trianing.colors
import kotlinx.android.synthetic.main.activity_setup_trianing.delay
import kotlinx.android.synthetic.main.activity_setup_trianing.dimmer
import kotlinx.android.synthetic.main.activity_setup_trianing.distance
import kotlinx.android.synthetic.main.activity_setup_trianing.executeTraining
import kotlinx.android.synthetic.main.activity_setup_trianing.groups
import kotlinx.android.synthetic.main.activity_setup_trianing.ledmode
import kotlinx.android.synthetic.main.activity_setup_trianing.shapes
import kotlinx.android.synthetic.main.activity_setup_trianing.sound
import kotlinx.android.synthetic.main.activity_setup_trianing.timeout
import kotlinx.android.synthetic.main.activity_setup_trianing.trainingList
import org.koin.android.ext.android.inject

class SetupTrainingActivity : BaseActivity() {

    private val bleNode: BleNode by inject()
    private val adapter = RendererAdapter()
    private val list = mutableListOf<NodeEventStatus>()

    private val timeoutValues = getRange(0.5, 5.0, 0.5)
    private val delayValues = getRange(0.1, 5.0, 0.1)
    private val threeValues = arrayOf("Low", "Medium", "High")
    private val touchesValues = (1..20 step 1).map { it.toString() }.toTypedArray()
    private val steps = mutableListOf<TrainSetup>()

    override fun title(): String = "Fast Training"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_trianing)

        timeout.displayedValues = timeoutValues
        timeout.maxValue = timeoutValues.size - 1
        timeout.minValue = 0
        timeout.wrapSelectorWheel = false
        delay.displayedValues = delayValues
        delay.maxValue = delayValues.size - 1
        delay.minValue = 0
        delay.wrapSelectorWheel = false
        dimmer.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, threeValues)
        distance.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, threeValues)
        val shapesArray = Utils.getAttrs(ShapeParams.javaClass).filter { it != "INSTANCE" }
        val colorsArray = Utils.getAttrs(ColorParams.javaClass).filter { it != "INSTANCE" && it != "COLOR_CUSTOM" }
        shapes.setItems(shapesArray)
        colors.setItems(colorsArray)
        groups.setItems((1..8).toList())

        executeTraining.setOnClickListener {
            execute()
        }
        messages()
    }

    private fun messages() {
        adapter.addRenderer(SetupRenderer())
        trainingList.adapter = adapter
        trainingList.layoutManager = LinearLayoutManager(this)
        trainingList.isNestedScrollingEnabled = false
        addTraining.setOnClickListener {
            val shape = Utils.getValues(ShapeParams.javaClass, shapes.getSelecteds() as List<String>).map { it as Int }.get(0)
            val color = Utils.getValues(ColorParams.javaClass, colors.getSelecteds() as List<String>).map { it as Int }.get(0)
            steps.add(
                TrainSetup(
                    shape = shape, color = color,
                    led = if (ledmode.isChecked) PeripheralParams.LED_FAST_FLASH else PeripheralParams.LED_PERMANENT
                )
            )
            adapter.setItems(steps)
        }
    }

    private fun execute() {
        val steps = adapter.getItems() as List<TrainSetup>
        bleNode.setupTrainMessage(
            groups.getSelecteds()[0] as Int, dimmer.selectedItemPosition, PeripheralParams.BOTH, distance.selectedItemPosition,
            if (sound.isChecked) PeripheralParams.BIP_START else PeripheralParams.NO_SOUND, steps
        )
    }

    private fun getRange(min: Double, max: Double, step: Double): Array<String> {
        val minInt = (min * 10).toInt()
        val maxInt = (max * 10).toInt()
        val stepInt = (step * 10).toInt()
        val range = (minInt..maxInt step stepInt)
        return range.map { (it / 10.0).toString() }.toTypedArray()
    }
}