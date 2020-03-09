package com.thinkup.blesample.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.thinkup.blesample.R
import com.thinkup.blesample.Utils
import com.thinkup.blesample.renderers.EventRenderer
import com.thinkup.connectivity.BleGroup
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.utils.EventObserver
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_group_detail.*
import kotlinx.android.synthetic.main.activity_group_detail.color
import kotlinx.android.synthetic.main.activity_group_detail.config
import kotlinx.android.synthetic.main.activity_group_detail.configSet
import kotlinx.android.synthetic.main.activity_group_detail.control
import kotlinx.android.synthetic.main.activity_group_detail.controlLedOff
import kotlinx.android.synthetic.main.activity_group_detail.controlLedOn
import kotlinx.android.synthetic.main.activity_group_detail.controlPause
import kotlinx.android.synthetic.main.activity_group_detail.controlRecalibrar
import kotlinx.android.synthetic.main.activity_group_detail.controlStart
import kotlinx.android.synthetic.main.activity_group_detail.controlStop
import kotlinx.android.synthetic.main.activity_group_detail.dimmer
import kotlinx.android.synthetic.main.activity_group_detail.distanceGroup
import kotlinx.android.synthetic.main.activity_group_detail.events
import kotlinx.android.synthetic.main.activity_group_detail.eventsList
import kotlinx.android.synthetic.main.activity_group_detail.gestureGroup
import kotlinx.android.synthetic.main.activity_group_detail.led
import kotlinx.android.synthetic.main.activity_group_detail.nodeIdText
import kotlinx.android.synthetic.main.activity_group_detail.periferal
import kotlinx.android.synthetic.main.activity_group_detail.periferalSetPre
import kotlinx.android.synthetic.main.activity_group_detail.shape
import kotlinx.android.synthetic.main.activity_group_detail.sound
import kotlinx.android.synthetic.main.activity_group_detail.tabs
import kotlinx.android.synthetic.main.activity_group_detail.timeout
import kotlinx.android.synthetic.main.toolbar.*
import no.nordicsemi.android.meshprovisioner.Group
import org.koin.android.ext.android.inject

class GroupDetailActivity : BaseActivity() {

    companion object {
        const val GROUP = "group"
        const val GROUP_INTENT = 5
    }

    val bleGroup: BleGroup by inject()
    var group: Group? = null
    val adapter = RendererAdapter()
    val list = mutableListOf<NodeEventStatus>()

    override fun title(): String = group?.name ?: "Group"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)
        group = intent?.getParcelableExtra(GROUP)
        identify.setOnClickListener { bleGroup.identify(group!!) }
        startUI()

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {
                p0?.let {
                    when (it.position) {
                        0 -> {
                            control.visibility = View.GONE
                        }
                        1 -> {
                            config.visibility = View.GONE
                        }
                        2 -> {
                            periferal.visibility = View.GONE
                        }
                        3 -> {
                            events.visibility = View.GONE
                        }
                    }
                }
            }

            override fun onTabSelected(p0: TabLayout.Tab?) {
                p0?.let {
                    when (it.position) {
                        0 -> {
                            control.visibility = View.VISIBLE
                        }
                        1 -> {
                            config.visibility = View.VISIBLE
                        }
                        2 -> {
                            periferal.visibility = View.VISIBLE
                        }
                        3 -> {
                            events.visibility = View.VISIBLE
                        }
                    }
                }
            }

        })
        startEvent()
        startControl()
        startConfig()
        startPrePeripheral()
        startStepPeripheral()
    }


    private fun startUI() {
        // Events
        adapter.addRenderer(EventRenderer())
        eventsList.adapter = adapter
        eventsList.layoutManager = LinearLayoutManager(this)
        eventsList.isNestedScrollingEnabled = false

        // Config
        timeout.minValue = 0
        timeout.maxValue = 10
        timeout.wrapSelectorWheel = true

        // Peripheral
        val shapesArray = Utils.getAttrs(ShapeParams.javaClass).filter { it != "INSTANCE" }
        shape.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, shapesArray)
        val colorsArray = Utils.getAttrs(ColorParams.javaClass).filter { it != "INSTANCE" && it != "COLOR_CUSTOM" }
        color.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, colorsArray)
        val dimmerValues = (0x00..0x64).toList().map { it.toString() }
        dimmer.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dimmerValues)
        sound.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayOf("NO_SOUND", "BIP_START", "BIP_HIT", "BIP_START_HIT"))
        led.adapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayOf("LED_PERMANENT", "LED_FLICKER", "LED_FLASH", "LED_FAST_FLASH"))
    }

    private fun startEvent() {
        bleGroup.getEvents().setObserver(object : EventObserver.Callback<NodeEventStatus?> {
            override fun onPost(e: NodeEventStatus?) {
                runOnUiThread {
                    e?.let {
                        list.add(e)
                        adapter.setItems(list)
                    }
                }
            }
        })
    }

    private fun startControl() {
        controlStart.setOnClickListener { bleGroup.controlMessage(group!!, ControlParams.START, timeout = timeout.value * 1000) }
        controlStop.setOnClickListener { bleGroup.controlMessage(group!!, ControlParams.STOP, timeout = timeout.value * 1000) }
        controlPause.setOnClickListener { bleGroup.controlMessage(group!!, ControlParams.PAUSE, timeout = timeout.value * 1000) }
        controlLedOn.setOnClickListener { bleGroup.controlMessage(group!!, ControlParams.SET_LED_ON, timeout = timeout.value * 1000) }
        controlLedOff.setOnClickListener { bleGroup.controlMessage(group!!, ControlParams.SET_LED_OFF, timeout = timeout.value * 1000) }
        controlRecalibrar.setOnClickListener { bleGroup.controlMessage(group!!, ControlParams.RECALIBRAR, timeout = timeout.value * 1000) }
    }

    private fun startPrePeripheral() {
        periferalSetPre.setOnClickListener {
            val selSound = Utils.getValue(PeripheralParams.javaClass, sound.selectedItem.toString())?.toString()?.toInt() ?: NO_CONFIG
            val gesture: Int = when (gestureGroup.checkedRadioButtonId) {
                R.id.hover -> PeripheralParams.HOVER
                R.id.touch -> PeripheralParams.TOUCH
                else -> PeripheralParams.BOTH
            }
            val distance: Int = when (distanceGroup.checkedRadioButtonId) {
                R.id.high -> PeripheralParams.HIGH
                R.id.medium -> PeripheralParams.MIDDLE
                else -> PeripheralParams.LOW
            }
            bleGroup.setPrePeripheralMessage(
                group!!, dimmer.selectedItem.toString().toInt(), gesture, distance, selSound
            )
        }
    }

    private fun startStepPeripheral() {
        periferalSetStep.setOnClickListener {
            val selShape = Utils.getValue(ShapeParams.javaClass, shape.selectedItem.toString())?.toString()?.toInt() ?: NO_CONFIG
            val selColor = Utils.getValue(ColorParams.javaClass, color.selectedItem.toString())?.toString()?.toInt() ?: NO_CONFIG
            val selLed = Utils.getValue(PeripheralParams.javaClass, led.selectedItem.toString())?.toString()?.toInt() ?: NO_CONFIG
            bleGroup.setStepPeripheralMessage(
                group!!, selShape, selColor, selLed
            )
        }
    }

    private fun startConfig() {
        configSet.setOnClickListener {
            bleGroup.configMessage(
                group!!,
                id = if (nodeIdText.text.toString().isNullOrEmpty()) NO_CONFIG else nodeIdText.text.toString().toInt()
            )
        }
    }
}
