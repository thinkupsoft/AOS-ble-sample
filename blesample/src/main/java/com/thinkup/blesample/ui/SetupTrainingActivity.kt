package com.thinkup.blesample.ui

import android.os.Bundle
import android.os.Handler
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.blesample.Utils
import com.thinkup.blesample.renderers.SetupRenderer
import com.thinkup.blesample.renderers.StartRenderer
import com.thinkup.connectivity.BleNode
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.setup.TrainSetup
import com.thinkup.connectivity.utils.EventObserver
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_setup_trianing.*
import org.koin.android.ext.android.inject

/**
 * Funcionalidades que necesitamos del lado de la APP para seguir testeando:
Poder seleccionar los destinatarios de cada paso. Luego la APP debería interpretar esto, y mandarle a cada nodo su secuencia a ejecutar.
Tener un botón de START que mande los N start secuenciados
 */
class SetupTrainingActivity : BaseActivity(), EventObserver.Callback<NodeEventStatus?> {

    data class StartAction(var ids: String = BASIC_MASK, val timeout: Double, var count: Int = 1)
    private val activeNodes: Int by lazy { bleNode.getNodes()?.filter { i -> i.isOnline }?.size ?: 0 }
    private val bleNode: BleNode by inject()
    private val adapter = RendererAdapter()
    private val adapterFinal = RendererAdapter()
    private val events = mutableListOf<NodeEventStatus>()
    private var currentStep = 0

    private val timeoutValues = getRange(0.5, 5.0, 0.5)
    private val delayValues = getRange(0.1, 5.0, 0.1)
    private val threeValues = arrayOf("Low", "Medium", "High")
    private val touchesValues = (1..20 step 1).map { it.toString() }.toTypedArray()
    private val nodeIds = (1..8 step 1).map { it.toString() }.toTypedArray()
    private val steps = mutableListOf<TrainSetup>()
    private val starts = mutableListOf<StartAction>()
    private val runnable = Runnable {
        checkStep()
    }
    private val handler = Handler()

    override fun title(): String = "Fast Training"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_trianing)

        timeout.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, timeoutValues)
        delay.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, delayValues)
        dimmer.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, threeValues)
        distance.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, threeValues)
        val shapesArray = Utils.getAttrs(ShapeParams.javaClass).filter { it != "INSTANCE" }
        val colorsArray = Utils.getAttrs(ColorParams.javaClass).filter { it != "INSTANCE" && it != "COLOR_CUSTOM" }
        shapes.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, shapesArray)
        colors.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, colorsArray)
        nodeAddress.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, nodeIds)

        executeTraining.setOnClickListener {
            execute()
        }
        startTraining.setOnClickListener {
            bleNode.getEvents().setObserver(this)
            sendStart()
        }
        clearTraining.setOnClickListener {
            clear()
        }
        messages()
    }

    private fun messages() {
        adapter.addRenderer(SetupRenderer())
        trainingList.adapter = adapter
        trainingList.layoutManager = LinearLayoutManager(this)
        trainingList.isNestedScrollingEnabled = false

        adapterFinal.addRenderer(StartRenderer())
        stepsList.adapter = adapterFinal
        stepsList.layoutManager = LinearLayoutManager(this)
        stepsList.isNestedScrollingEnabled = false

        addTraining.setOnClickListener {
            try {
                val shape = Utils.getValues(ShapeParams.javaClass, listOf(shapes.selectedItem.toString())).map { it as Int }.get(0)
                val color = Utils.getValues(ColorParams.javaClass, listOf(colors.selectedItem.toString())).map { it as Int }.get(0)
                steps.add(
                    TrainSetup(
                        shape = shape, color = color,
                        led = if (ledmode.isChecked) PeripheralParams.LED_FAST_FLASH else PeripheralParams.LED_PERMANENT,
                        stepIndex = steps.size
                    )
                )
                adapter.setItems(steps)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun execute() {
        try {
            val steps = adapter.getItems() as List<TrainSetup>
            bleNode.setupTrainMessage(
                nodeAddress.selectedItem.toString().toInt() + 1, dimmer.selectedItemPosition, PeripheralParams.BOTH, distance.selectedItemPosition,
                if (sound.isChecked) PeripheralParams.BIP_START else PeripheralParams.NO_SOUND, steps, activeNodes
            )
            addStart(nodeAddress.selectedItem.toString().toInt(), steps)

            this.steps.clear()
            adapter.setItems(this.steps)

            adapterFinal.setItems(starts)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun addStart(nodeAddress: Int, steps: List<TrainSetup>) {
        steps.forEachIndexed { index, trainSetup ->
            val action = starts.elementAtOrNull(index)
            action?.let {
                it.ids = OpCodes.getMask(nodeAddress, it.ids)
                it.count = it.count + 1
            } ?: run {
                starts.add(index, StartAction(OpCodes.getMask(nodeAddress, BASIC_MASK), 1.0))
            }
        }
    }

    private fun sendStart() {
        val action = starts[currentStep]
        bleNode.sendBroadcast(action.ids, (action.timeout * 1000).toLong())
        handler .postDelayed(runnable, (action.timeout * 1000).toLong())
    }

    private fun clear() {
        starts.clear()
        adapterFinal.setItems(starts)
        steps.clear()
        adapter.setItems(steps)
        events.clear()
        currentStep = 0
    }

    private fun getRange(min: Double, max: Double, step: Double): Array<String> {
        val minInt = (min * 10).toInt()
        val maxInt = (max * 10).toInt()
        val stepInt = (step * 10).toInt()
        val range = (minInt..maxInt step stepInt)
        return range.map { (it / 10.0).toString() }.toTypedArray()
    }

    @Synchronized
    override fun onPost(e: NodeEventStatus?) {
        e?.let {
            events.add(e)
            if (events.size == starts[currentStep].count) {
                checkStep()
            }
        }
    }

    private fun checkStep() {
        handler.removeCallbacks(runnable)
        events.clear()
        currentStep++
        if (steps.size == currentStep) {
            clear()
            Toast.makeText(this, "Completed", Toast.LENGTH_SHORT).show()
        } else sendStart()
    }
}