package com.thinkup.connectivity.impl

import android.content.Context
import android.util.Log
import androidx.lifecycle.Observer
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.BleFastTraining
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.common.FastOptions
import com.thinkup.connectivity.common.TrainingGroup
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.*
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class BleFastTrainingImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BaseBleImpl(context, setting, repository),
    BleFastTraining {

    private lateinit var callback: BleFastTraining.TrainingCallback
    private lateinit var options: FastOptions
    private lateinit var groups: List<TrainingGroup>

    private lateinit var appkey: ApplicationKey
    private lateinit var model: VendorModel

    private val eventObserver = Observer<NodeEventStatus?> {
        Log.d("TKUP-NEURAL::", "Unprocess event:: $it")
        var ended = 0
        if (it is NodeEventStatus && (it.eventType == EventType.HIT || it.eventType == EventType.TIMEOUT)) {
            groups.forEach { group ->
                if (group.isFromThis(it.srcAddress) && group.currentStep > group.lastReceivedStep) {
                    group.stopFallback()
                    Log.d("TKUP-NEURAL::", " ${group.group} - Event:: $it")
                    group.lastReceivedStep++
                    callback.onAction(group.group, getNode(it.srcAddress), it, it.eventType, it.value.toLong())
                    step(listOf(group))
                }
                if (group.lastReceivedStep == options.touches) ended++
            }
        }
        if (ended == groups.size) finish()
    }

    override fun set(groups: List<Group>?, options: FastOptions, callback: BleFastTraining.TrainingCallback) = executeService {
        this.groups = groups?.map { TrainingGroup(it.address, it, getGroupNodes(it), 0, 0) }
            ?: getGroups().value!!.map { TrainingGroup(it.address, it, getGroupNodes(it), 0, 0) }
        this.options = options
        this.callback = callback
        repository.getTrainingMessageLiveData().removeObserver(eventObserver)
        repository.isSending = true
        callback.onSettingStart()
        setNodes()
        starterConfig()
        delay(300)
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
                group.group,
                NodeControlMessageUnacked(ControlParams.SET_LED_ON, NO_CONFIG, appkey, model.modelId, model.companyIdentifier),
                true
            )
        }
        for (i in 1..3) {
            bulkMessaging(groups) { group ->
                sendMessage(
                    group.group,
                    NodeStepPeripheralMessageUnacked(
                        ShapeParams.CIRCLE, getCountdownColor(i), PeripheralParams.LED_PERMANENT,
                        appkey, model.modelId, model.companyIdentifier
                    ), true
                )
            }
            delay(1000)
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
                val models = network?.getModels(group.group)
                if (models?.isNotEmpty() == true) {
                    model = models[0] as VendorModel
                    val appKey = getAppKey(model.boundAppKeyIndexes[0])
                    appKey?.let { this.appkey = it }
                }
            }
        }
    }

    /**
     * Before start a fast training set the common an in-mutable options to the groups
     * timeout (light time), sound, led
     */
    private fun starterConfig() {
        Log.d("TKUP-NEURAL::", "StarConfig")
        bulkMessaging(groups) { group ->
            val dimmer = when (options.dimmer) {
                0 -> 0X05
                1 -> 0X32
                else -> 0X64
            }
            val distance = when (options.distance) {
                0 -> PeripheralParams.LOW
                1 -> PeripheralParams.MIDDLE
                else -> PeripheralParams.HIGH
            }
            sendMessage(
                group.group,
                NodePrePeripheralMessageUnacked(
                    dimmer, PeripheralParams.BOTH, distance,
                    if (options.sound) PeripheralParams.BIP_START_HIT else PeripheralParams.NO_SOUND,
                    appkey, model.modelId, model.companyIdentifier
                ),
                true
            )
        }
    }

    private fun start() = executeService {
        Log.d("TKUP-NEURAL::", "LedOff to Start")
        bulkMessaging(groups) { group ->
            sendMessage(
                group.group,
                NodeControlMessageUnacked(ControlParams.SET_LED_OFF, options.timeout, appkey, model.modelId, model.companyIdentifier),
                true
            )
        }
        repository.flushTrainingMessageLiveData()
        repository.getTrainingMessageLiveData().observeForever(eventObserver)
        callback.onStartTraining()
        delay(200)
        step(groups)
    }

    private fun step(list: List<TrainingGroup>) = executeService {
        var ended = 0
        bulkMessaging(list) { group ->
            if (group.currentStep < options.touches) {
                group.currentStep++
                Log.d("TKUP-NEURAL::", " ${group.group} - Step ${group.currentStep}")
                Log.d("TKUP-NEURAL::", "Delay ${options.delay}")
                delay(options.delay.toLong())
                val node = group.nodes.random()
                val color = options.colors.random()
                val shape = options.shapes.random()
                node.let {
                    Log.d("TKUP-NEURAL::", "Setting peripheral options - Node = ${node.nodeName}")
                    sendMessage(
                        node,
                        NodeStepPeripheralMessageUnacked(
                            shape, color, options.flashMode,
                            appkey, model.modelId, model.companyIdentifier
                        ), true
                    )
                    sendMessage(
                        node,
                        NodeControlMessageUnacked(ControlParams.START, options.timeout, appkey, model.modelId, model.companyIdentifier)
                    )
                }
                scheduledFallback(group, node)
            }
            if (group.lastReceivedStep == options.touches) ended++
            if (ended == groups.size) finish()
        }
    }

    private fun scheduledFallback(group: TrainingGroup, node: ProvisionedMeshNode) {
        group.missedStepFallback(options.timeout) {
            step(listOf(group))
            callback.onAction(
                group.group, node, NodeEventStatus(EventType.TIMEOUT, options.timeout),
                EventType.TIMEOUT, options.timeout.toLong()
            )
        }
    }

    private fun finish() {
        Log.d("TKUP-NEURAL::", "Finish")
        repository.getTrainingMessageLiveData().removeObserver(eventObserver)
        bulkMessaging(groups) { group ->
            group.stopFallback()
            delay(REPLICATE_DELAY)
            if (options.endWithLight) {
                autoOffLedMessage(
                    group.group,
                    NodeStepPeripheralMessageUnacked(
                        ShapeParams.CIRCLE, ColorParams.COLOR_WITHE, PeripheralParams.LED_PERMANENT,
                        appkey, model.modelId, model.companyIdentifier
                    )
                )
            }
        }
        repository.isSending = false
        callback.onCompleteTraining()
    }
}