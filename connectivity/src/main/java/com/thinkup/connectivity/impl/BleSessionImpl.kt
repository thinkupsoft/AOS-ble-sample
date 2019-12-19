package com.thinkup.connectivity.impl

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import androidx.lifecycle.Observer
import com.thinkup.connectivity.BleSession
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.ControlParams
import com.thinkup.connectivity.messges.EventType
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.config.NodeConfigMessageUnacked
import com.thinkup.connectivity.messges.control.NodeControlMessage
import com.thinkup.connectivity.messges.control.NodeControlMessageStatus
import com.thinkup.connectivity.messges.event.NodeEventStatus
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class BleSessionImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BaseBleImpl(context, setting, repository), BleSession {

    private val handler = Handler()
    private val runnable = Runnable { keepAlive() }

    private val bluetoothStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val previousState = intent.getIntExtra(
                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                BluetoothAdapter.STATE_OFF
            )

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    autoConnect()
                }
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                    disconnect()
                    repository.updateNodes(listOf())
                }
            }
        }
    }

    override fun start(): Unit = executeService {
        // Listen for a HELLO event message
        repository.getEventMessageLiveData().observeForever { configMessage(it) }
        // Check if connect and re-connect message are enable
        if (setting.enabledStartConfig() && !checkConnectivity() && !repository.isSending) {
            // Register for bluetooth setting change and reconnect
            registerCallback()
            // Listen for connection changes
            repository.isConnectedToProxy().observeForever { if (it) keepAlive() else autoConnect() }
            autoConnect()
        } else {
            keepAlive()
        }
    }

    private fun autoConnect() {
        Handler().postDelayed({ autoConnect { start() } }, 1000)
    }

    private fun registerCallback() {
        context.registerReceiver(
            bluetoothStateBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    /**
     * Config a node when receive a HELLO event message
     */
    private fun configMessage(event: NodeEventStatus) {
        if (event.eventType == EventType.HELLO) {
            getNode(event.srcAddress)?.let {
                it.isOnline = true
                val element: Element? = getElement(it)
                if (element != null) {
                    val model = getModel<VendorModel>(element)
                    if (model != null) {
                        val appKey = getAppKey(model.boundAppKeyIndexes[0])
                        appKey?.let { key ->
                            sendMessage(
                                it,
                                NodeConfigMessageUnacked(it.nodeName.toInt(), NO_CONFIG, NO_CONFIG, key, model.modelId, model.companyIdentifier)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun keepAlive(): Unit = executeService {
        if (setting.enabledKeepAlive() && !repository.isSending) {
            val connected = mutableListOf<ProvisionedMeshNode>()
            val nodes = getNodes()
            val observer = Observer<MeshMessage?> {
                synchronized(connected) {
                    if (it is NodeControlMessageStatus) {
                        checkNode(it)?.let { node ->
                            node.batteryLevel = it.batteryLevel
                            connected.add(node)
                        }
                    }
                }
            }
            repository.getKeepMessageLiveData().observeForever(observer)
            bulkMessaging(nodes) { initialConfigMessage(it) }
            Handler().postDelayed({
                repository.getKeepMessageLiveData().removeObserver(observer)
                repository.flushKeepMessageLiveData()
                repository.updateNodes(connected)
            }, KEEP_ALIVE_WAIT)
        }
        handler.postDelayed(runnable, KEEP_ALIVE)
    }

    override fun stop() {
        handler.removeCallbacks(runnable)
    }

    private fun checkNode(message: NodeControlMessageStatus): ProvisionedMeshNode? {
        return getNode(message.srcAddress)
    }

    private fun initialConfigMessage(node: ProvisionedMeshNode) {
        val handler = Handler()
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    handler.postDelayed({
                        // TODO: KEEP_ALIVE instance of SET_LED_ON
                        sendMessage(node, NodeControlMessage(ControlParams.KEEP_ALIVE, appKey, model.modelId, model.companyIdentifier))
                    }, BULK_DELAY)
                }
            }
        }
    }
}