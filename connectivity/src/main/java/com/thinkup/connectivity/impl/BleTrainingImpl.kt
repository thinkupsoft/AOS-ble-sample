package com.thinkup.connectivity.impl

import android.content.Context
import android.util.Log
import androidx.lifecycle.Observer
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.BleTraining
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.common.FastOptions
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.config.NodeConfigMessageUnacked
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.NodePrePeripheralMessage
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessageStatus
import com.thinkup.connectivity.messges.peripheral.NodePrePeripheralMessageUnacked
import com.thinkup.connectivity.messges.peripheral.NodeStepPeripheralMessageUnacked
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class BleTrainingImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BaseBleImpl(context, setting, repository), BleTraining {

    private lateinit var callback: BleTraining.TrainingCallback
    private lateinit var options: FastOptions
    private lateinit var groups: MutableList<Group>
    private val nodes = HashMap<Int, List<ProvisionedMeshNode>>()

    private lateinit var appkey: ApplicationKey
    private lateinit var model: VendorModel
    private var currentStep = 0
    private var lastReceivedStep = 0

    private val eventObserver = Observer<NodeEventStatus?> {
        Log.d("TKUP-NEURAL::", "Unprocess event:: $it")
        if (it is NodeEventStatus && currentStep > lastReceivedStep && (it.eventType == EventType.HIT || it.eventType == EventType.TIMEOUT)) {
            Log.d("TKUP-NEURAL::", "Event:: $it")
            lastReceivedStep++
            step()
            val group = getGroup(it.srcAddress)
            callback.onAction(group, getNode(it.srcAddress), it)
        }
    }

    private fun getGroup(srcAddress: Int): Group? {
        nodes.forEach {
            it.value.find { node -> node.unicastAddress == srcAddress }?.let { unwrappednode ->
                groups.find { group -> group.id == it.key }?.let { group ->
                    return group
                }
            }
        }
        return null
    }

    override fun set(groups: List<Group>?, options: FastOptions, callback: BleTraining.TrainingCallback) = executeService {
        this.groups = groups?.toMutableList() ?: getGroups().value!!.toMutableList()
        this.options = options
        this.callback = callback
        currentStep = 0
        lastReceivedStep = 0
        repository.getTrainingMessageLiveData().removeObserver(eventObserver)
        repository.getMeshMessageLiveData().removeObserver(messagesObserver)
        repository.isSending = true
        callback.onSettingStart()
        setNodes()
        starterConfig()
        delay(1000)
        repository.isSending = false
        callback.onSettingComplete()
    }

    override fun startTraining() = executeService {
        repository.isSending = true
        if (options.startWithCountdown) {
            callback.onCountdown()
            countdown()
        } else {
            start()
        }
    }

    private suspend fun countdown() {
        bulkMessaging(groups) { group ->
            sendMessage(
                group,
                NodeControlMessageUnacked(ControlParams.SET_LED_ON, NO_CONFIG, appkey, model.modelId, model.companyIdentifier),
                true
            )
            delay(1000)
        }
        for (i in 1..3) {
            bulkMessaging(groups) { group ->
                sendMessage(
                    group, NodeStepPeripheralMessageUnacked(
                        ShapeParams.CIRCLE, getCountdownColor(i), PeripheralParams.LED_PERMANENT,
                        appkey, model.modelId, model.companyIdentifier
                    ), true
                )
            }
            delay(1200)
        }
        start()
    }

    private fun getCountdownColor(index: Int): Int = when (index) {
        3 -> ColorParams.COLOR_GREEN
        2 -> ColorParams.COLOR_YELLOW
        else -> ColorParams.COLOR_RED
    }

    private fun setNodes() {
        bulkMessaging(groups) { group ->
            if (!::appkey.isInitialized || !::model.isInitialized) {
                val network = repository.getMeshNetworkLiveData().getMeshNetwork()
                val models = network?.getModels(group)
                if (models?.isNotEmpty() == true) {
                    model = models[0] as VendorModel
                    val appKey = getAppKey(model.boundAppKeyIndexes[0])
                    appKey?.let { this.appkey = it }
                }
            }
            val groupNodes = getGroupNodes(group)
            if (groupNodes.isNotEmpty()) {
                nodes[group.id] = groupNodes
            } else groups.remove(group)
        }
    }

    /**
     * Before start a fast training set the common an in-mutable options to the groups
     * timeout (light time), sound, led
     */
    private fun starterConfig() {
        Log.d("TKUP-NEURAL::", "StarConfig")
        bulkMessaging(groups) { group ->
            sendMessage(
                group,
                NodeConfigMessageUnacked(NO_CONFIG, appkey, model.modelId, model.companyIdentifier),
                true
            )
        }
    }

    private fun start() = executeService {
        Log.d("TKUP-NEURAL::", "LedOff to Start")
        bulkMessaging(groups) { group ->
            sendMessage(
                group,
                NodeControlMessageUnacked(ControlParams.SET_LED_OFF, options.timeout, appkey, model.modelId, model.companyIdentifier),
                true
            )
        }
        repository.flushTrainingMessageLiveData()
        repository.getTrainingMessageLiveData().observeForever(eventObserver)
        repository.getMeshMessageLiveData().observeForever(messagesObserver)
        callback.onStartTraining()
        delay(500)
        step()
    }

    private fun step() = executeService {
        if (currentStep < options.touches) {
            currentStep++
            Log.d("TKUP-NEURAL::", "Step $currentStep")
            Log.d("TKUP-NEURAL::", "Delay ${options.delay}")
            delay(options.delay.toLong())
            bulkMessaging(groups) { group ->
                val node = nodes[group.id]?.random()
                val color = options.colors.random()
                val shape = options.shapes.random()
                node?.let {
                    currentNode = node
                    Log.d("TKUP-NEURAL::", "Setting peripheral options - Node = ${node.nodeName}")
                    sendMessage(
                        node,
                        NodeStepPeripheralMessageUnacked(
                            shape, color, options.flashMode,
                            appkey, model.modelId, model.companyIdentifier
                        ), true
                    )
                }
            }
        } else finish()
    }

    private fun finish() {
        currentStep = 0
        lastReceivedStep = 0
        Log.d("TKUP-NEURAL::", "Finish")
        repository.getTrainingMessageLiveData().removeObserver(eventObserver)
        repository.getMeshMessageLiveData().removeObserver(messagesObserver)
        bulkMessaging(groups) { group ->
            sendReplicateMessage(
                group,
                NodeControlMessageUnacked(ControlParams.STOP, options.timeout, appkey, model.modelId, model.companyIdentifier),
                false
            )
        }
        repository.isSending = false
        callback.onCompleteTraining()
    }

    private lateinit var currentNode: ProvisionedMeshNode
    private val messagesObserver: Observer<MeshMessage?> = Observer {
        when {
            it is NodePeripheralMessageStatus && ::currentNode.isInitialized -> {
                Log.d("TKUP-NEURAL::", "Starting peripheral")
                sendMessage(
                    currentNode,
                    NodeControlMessageUnacked(ControlParams.START, options.timeout, appkey, model.modelId, model.companyIdentifier),
                    true
                )
            }
        }
    }
}