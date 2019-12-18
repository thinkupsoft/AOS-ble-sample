package com.thinkup.connectivity.impl

import android.content.Context
import android.os.Handler
import androidx.lifecycle.Observer
import com.thinkup.connectivity.BleSession
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.ControlParams
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.config.NodeConfigMessage
import com.thinkup.connectivity.messges.config.NodeConfigMessageStatus
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class BleSessionImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BaseBleImpl(context, setting, repository), BleSession {

    private val handler = Handler()
    private val runnable = Runnable { keepAlive() }

    override fun start(): Unit = executeService {
        if (setting.enabledStartConfig() && !checkConnectivity() && !repository.isSending) {
            Handler().postDelayed({ autoConnect { start() } }, 1000)
            repository.updateNodes(listOf())
        } else {
            keepAlive()
        }
    }

    override fun keepAlive(): Unit = executeService {
        if (setting.enabledKeepAlive() && !repository.isSending) {
            val connected = mutableListOf<ProvisionedMeshNode>()
            val nodes = getNodes()
            val observer = Observer<MeshMessage?> {
                synchronized(connected) {
                    if (it is NodeConfigMessageStatus) {
                        checkNode(it)?.let { node ->
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

    private fun checkNode(message: NodeConfigMessageStatus): ProvisionedMeshNode? {
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
                    val nodeId = try {
                        node.nodeName.toInt()
                    } catch (ex: Exception) {
                        0
                    }
                    handler.postDelayed({
                        sendMessage(
                            node,
                            NodeControlMessageUnacked(ControlParams.SET_LED_ON, appKey, model.modelId, model.companyIdentifier)
                        )
                        handler.postDelayed({
                            sendMessage(node, NodeConfigMessage(nodeId, NO_CONFIG, NO_CONFIG, appKey, model.modelId, model.companyIdentifier))
                        }, BULK_DELAY)
                    }, BULK_DELAY)

                }
            }
        }
    }
}