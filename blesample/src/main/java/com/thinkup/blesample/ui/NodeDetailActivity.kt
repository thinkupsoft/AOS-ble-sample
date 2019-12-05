package com.thinkup.blesample.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayout
import com.thinkup.blesample.R
import com.thinkup.connectivity.messges.ConfigParams
import com.thinkup.connectivity.messges.ControlParams
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.PeripheralParams
import com.thinkup.connectivity.nodes.NodesViewModel
import kotlinx.android.synthetic.main.activity_node_detail.*
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.viewmodel.ext.android.viewModel

class NodeDetailActivity : AppCompatActivity() {

    companion object {
        const val NODE = "node"
    }

    val nodesViewModel: NodesViewModel by viewModel()
    var node: ProvisionedMeshNode? = null

    private fun startConnection() {
        toolbar.title = title
        connectionStatus.text = if (nodesViewModel.isConnected()?.value == true) "Connected" else "Disconnected"
        connectionStatus.setOnClickListener {
            if (nodesViewModel.isConnected()?.value == true) nodesViewModel.disconnect()
            else nodesViewModel.autoConnect()
        }
        nodesViewModel.isConnected()?.observe(this, Observer {
            connectionStatus.text = if (nodesViewModel.isConnected()?.value == true) {
                "Connected"
            } else {
                nodesViewModel.autoConnect()
                "Disconnected"
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_node_detail)
        startConnection()

        node = intent.getParcelableExtra(NODE)

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {

            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {

            }

            override fun onTabSelected(p0: TabLayout.Tab?) {
                p0?.let {
                    when (it.position) {
                        0 -> {
                            control.bringToFront()
                        }
                        1 -> {
                            config.bringToFront()
                        }
                        2 -> {
                            periferal.bringToFront()
                        }
                        3 -> {
                            events.bringToFront()
                        }
                    }
                }
            }

        })
        startBase()
        startControl()
        startConfig()
        startPeripheral()
    }

    private fun startBase() {
        nodeStatus.setOnClickListener { nodesViewModel.getStatus(node!!) }
        nodeTtl.setOnClickListener { nodesViewModel.getTtl(node!!) }
    }

    private fun startControl() {
        controlStart.setOnClickListener { nodesViewModel.controlMessage(node!!, ControlParams.START) }
        controlStop.setOnClickListener { nodesViewModel.controlMessage(node!!, ControlParams.STOP) }
        controlPause.setOnClickListener { nodesViewModel.controlMessage(node!!, ControlParams.PAUSE) }
        controlLedOn.setOnClickListener { nodesViewModel.controlMessage(node!!, ControlParams.SET_LED_ON) }
        controlLedOff.setOnClickListener { nodesViewModel.controlMessage(node!!, ControlParams.SET_LED_OFF) }
        controlRecalibrar.setOnClickListener { nodesViewModel.controlMessage(node!!, ControlParams.RECALIBRAR) }
    }

    private fun startPeripheral() {
        periferalSet.setOnClickListener {
            nodesViewModel.setPeripheralMessage(
                node!!, PeripheralParams.CIRCLE, PeripheralParams.COLOR_BLUE, 0x40, PeripheralParams.LED_PERMANENT, PeripheralParams.FILL,
                PeripheralParams.HOVER, PeripheralParams.MIDDLE, PeripheralParams.INDOOR, NO_CONFIG, PeripheralParams.BIP_START_HIT
            )
        }
    }

    private fun startConfig() {
        configSet.setOnClickListener {
            nodesViewModel.configMessage(
                node!!,
                timeoutConfig = ConfigParams.TIMEOUT_CONFIG,
                timeout = 5,
                flow = ConfigParams.ON
            )
        }
    }
}
