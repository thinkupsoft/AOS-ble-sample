package com.thinkup.connectivity.impl

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.thinkup.connectivity.BleSession
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.config.NodeConfigMessageUnacked
import com.thinkup.connectivity.messges.control.NodeControlMessage
import com.thinkup.connectivity.messges.control.NodeControlMessageStatus
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.NodePrePeripheralMessageUnacked
import com.thinkup.connectivity.utils.CapabilitiesUtil
import com.thinkup.connectivity.utils.EventObserver
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class BleSessionImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository, private val capabilitiesUtil: CapabilitiesUtil) :
    BaseBleImpl(context, setting, repository), BleSession, EventObserver.Callback<NodeControlMessageStatus?> {

    private val state = MutableLiveData<ConnectionState>()
    private val handler = Handler()
    private val runnable = Runnable { keepAlive() }
    private var keepAliveProgressing = false
    val connected = mutableListOf<ProvisionedMeshNode>()

    override fun state(): LiveData<ConnectionState> = state

    init {
        state.postValue(if (capabilitiesUtil.isBleEnabled()) ConnectionState.IDLE else ConnectionState.BLE_OFF)
    }

    /**
     * Config a node when receive a HELLO event message
     * Config starter dimmer to be able to draw
     */
    private val configObserver = object : EventObserver.Callback<NodeEventStatus?> {
        override fun onPost(event: NodeEventStatus?) {
            if (event != null && event.eventType == EventType.HELLO && !repository.isSending) {
                Log.d("TKUP-NEURAL::HELLO-RE::", event.toString())
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
                                    NodeConfigMessageUnacked(it.nodeName.toInt(), key, model.modelId, model.companyIdentifier)
                                )
                                Handler(Looper.getMainLooper()).postDelayed({
                                    sendBroadcastMessage(
                                        NodePrePeripheralMessageUnacked(
                                            DimmerParams.LOW, NO_CONFIG, NO_CONFIG, NO_CONFIG,
                                            key, model.modelId, model.companyIdentifier, OpCodes.getUnicastMask(it.nodeName.toInt())
                                        )
                                    )
                                }, BULK_DELAY)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Observe connection state for changes
     */
    private val connectionObserver: Observer<Boolean> = Observer {
        if (it) {
            state.postValue(ConnectionState.CONECTED)
            // Listen for a HELLO event message
            repository.getHelloCallback().setObserver(configObserver)
            // on connect start to send keep alive
            keepAlive()
        } else {
            // when disconnect, update nodes to offline and search for a new connection proxy
            state.postValue(ConnectionState.DISCONECTED)
            repository.updateNodes(listOf())
            if (autoConnectCondition()) autoConnect()
        }
    }

    // Listen for BLE device setting change
    private val bluetoothStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val previousState = intent.getIntExtra(
                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                BluetoothAdapter.STATE_OFF
            )

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    if (autoConnectCondition()) autoConnect()
                }
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                    disconnect()
                    repository.updateNodes(listOf())
                }
            }
        }
    }

    override fun start(): Unit = executeService {
        // Register for bluetooth setting change and reconnect
        registerCallback()
        // Listen for connection changes
        repository.isConnectedToProxy().observeForever(connectionObserver)
        // Check if connect and re-connect message are enable
        if (autoConnectCondition() && capabilitiesUtil.isBleEnabled()) {
            autoConnect()
        } else {
            keepAlive()
        }
    }

    private fun autoConnectCondition(): Boolean = setting.enabledStartConfig() && !checkConnectivity()

    private fun autoConnect() {
        state.postValue(ConnectionState.CONNECTING)
        if (autoConnectCondition()) Handler().postDelayed({ autoConnect { } }, 400)
    }

    private fun registerCallback() {
        context.registerReceiver(
            bluetoothStateBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    // Keep-alive messages are sent to each node and wait for responses to mark node as online
    // finally the method is scheduled to repeat the routine
    private fun keepAlive(): Unit = executeService {
        Log.e("THKUP::", "Keep-alive ${setting.enabledKeepAlive()} && ${repository.isSending} && $keepAliveProgressing")

        if (isEnabledKeepAlive()) {
            val nodes = getNodes()
            if (nodes != null && nodes.isNotEmpty()) {
                keepAliveProgressing = true
                connected.clear()
                repository.getKeepMessageCallback().setObserver(this@BleSessionImpl)
                val ids = nodes.map { it.nodeName.toInt() }
                Log.d("TKUP-NEURAL::KA-SEND::", ids.toString())
                keepAliveMessage(ids, getElement(nodes[0]))

                Handler(Looper.getMainLooper()).postDelayed({
                    val diff = ids.subtract(connected.map { it.nodeName.toInt() })
                    keepAliveMessage(diff.toList(), getElement(nodes[0]))
                    Handler(Looper.getMainLooper()).postDelayed({
                        repository.getKeepMessageCallback().removeObserver()
                        repository.updateNodes(connected)
                        keepAliveProgressing = false
                    }, KEEP_ALIVE_WAIT)
                }, KEEP_ALIVE_WAIT)
            }
        }
        scheduleKeepAlive()
    }

    private fun scheduleKeepAlive() {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, KEEP_ALIVE)
    }

    private fun isEnabledKeepAlive() = setting.enabledKeepAlive() && !repository.isSending && !keepAliveProgressing


    override fun stop() {
        handler.removeCallbacks(runnable)
    }

    private fun checkNode(message: NodeControlMessageStatus): ProvisionedMeshNode? {
        return getNode(message.srcAddress)
    }

    private fun keepAliveMessage(ids: List<Int>, element: Element?) {
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    sendBroadcastMessage(
                        NodeControlMessage(
                            ControlParams.KEEP_ALIVE.toByte(), 0, appKey,
                            model.modelId, model.companyIdentifier, OpCodes.getGroupMask(ids)
                        )
                    )
                }
            }
        }
    }

    override fun onPost(e: NodeControlMessageStatus?) {
        synchronized(connected) {
            if (e is NodeControlMessageStatus) {
                Log.d("TKUP-NEURAL::KA-REC::", e.toString())
                checkNode(e)?.let { node ->
                    node.batteryLevel = e.batteryLevel
                    connected.add(node)
                }
            }
        }
    }

    enum class ConnectionState { BLE_OFF, CONECTED, CONNECTING, DISCONECTED, IDLE }
}