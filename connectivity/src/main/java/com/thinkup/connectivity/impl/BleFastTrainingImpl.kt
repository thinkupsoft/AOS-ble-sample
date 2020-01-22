package com.thinkup.connectivity.impl

import android.content.Context
import android.util.Log
import androidx.lifecycle.Observer
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.BleFastTraining
import com.thinkup.connectivity.common.FastOptions
import com.thinkup.connectivity.common.TrainingCallback
import com.thinkup.connectivity.common.TrainingGroup
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.*
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class BleFastTrainingImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BleBaseTraining(context, setting, repository),
    BleFastTraining {

    private lateinit var options: FastOptions

    override fun getDimmerValue(): Int = options.dimmer
    override fun getDistanceValue(): Int = options.distance
    override fun getSoundValue(): Boolean = options.sound

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

    override fun set(groups: List<Group>?, options: FastOptions, callback: TrainingCallback) = executeService {
        this.options = options
        repository.getTrainingMessageLiveData().removeObserver(eventObserver)
        super.set(groups, callback, null)
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

    override fun stopTraining() {
        finish()
    }

    override fun start() = executeService {
        Log.d("TKUP-NEURAL::", "LedOff to Start")
        bulkMessaging(groups) { group ->
            sendMessage(
                group.group,
                NodeControlMessageUnacked(ControlParams.SET_LED_OFF, NO_CONFIG, appkey, model.modelId, model.companyIdentifier),
                true
            )
        }
        repository.flushTrainingMessageLiveData()
        repository.getTrainingMessageLiveData().observeForever(eventObserver)
        callback.onStartTraining()
        // delay despues de countdown, antes de primer paso
        delay(options.delay.toLong())
        step(groups)
    }

    private fun step(list: List<TrainingGroup>) = executeService {
        var ended = 0
        bulkMessaging(list) { group ->
            if (group.currentStep < options.touches) {
                group.currentStep++
                Log.d("TKUP-NEURAL::", " ${group.group} - Step ${group.currentStep}")
                Log.d("TKUP-NEURAL::", "Delay ${options.delay}")
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
                    // delay entre train y start
                    delay(options.delay.toLong())
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

    override fun finish() {
        Log.d("TKUP-NEURAL::", "Finish")
        repository.getTrainingMessageLiveData().removeObserver(eventObserver)
        if (groupsnitialized()) {
            bulkMessaging(groups) { group ->
                group.stopFallback()
                // delay antes de end light
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
        }
        repository.isSending = false
        callback.onCompleteTraining()
    }
}