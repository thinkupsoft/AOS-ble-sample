package com.thinkup.connectivity.impl

import android.content.Context
import android.util.Log
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
import com.thinkup.connectivity.utils.EventObserver
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class BleFastTrainingImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BleBaseTraining(context, setting, repository),
    BleFastTraining, EventObserver.Callback<NodeEventStatus?> {

    private lateinit var options: FastOptions

    override fun getDimmerValue(): Int = options.dimmer
    override fun getDistanceValue(): Int = options.distance
    override fun getSoundValue(): Boolean = options.sound

    override fun onPost(eventStatus: NodeEventStatus?) {
        var ended = 0
        if (eventStatus != null) {
            if (eventStatus.eventType == EventType.HIT || eventStatus.eventType == EventType.TIMEOUT) {
                groups.forEach { group ->
                    if (group.isFromThis(eventStatus.srcAddress) && group.currentStep > group.lastReceivedStep) {
                        group.lastReceivedStep++
                        group.stopFallback()
                        stepG(group)
                        callback?.onAction(
                            group.group,
                            getNode(eventStatus.srcAddress),
                            eventStatus,
                            eventStatus.eventType,
                            eventStatus.value.toLong()
                        )
                    }
                    if (group.lastReceivedStep == options.touches) ended++
                }
            }
        }
        if (ended == groups.size) finish()
    }

    override fun set(groups: List<Group>?, options: FastOptions, callback: TrainingCallback) = executeService {
        this.options = options
        repository.getTrainingMessageLiveData().setObserver(this)
        super.set(groups, callback, null)
    }

    override fun startTraining() = executeService {
        repository.isSending = true
        if (options.startWithCountdown) {
            callback?.onCountdown()
            countdown()
        } else {
            start()
        }
    }

    override fun stopTraining() {
        finish()
    }

    override fun start() = executeService {
        sendBroadcastMessage(
            NodeControlMessageUnacked(
                ControlParams.SET_LED_OFF.toByte(), NO_CONFIG,
                appkey, model.modelId, model.companyIdentifier, DYNAMIC_MASK
            ), true
        )
        callback?.onStartTraining()
        // delay after countdown, before first step
        delay(options.delay.toLong())
        bulkMessaging(groups) { group ->
            stepG(group)
        }
    }

    private fun stepG(group: TrainingGroup) = executeService {
        if (group.currentStep < options.touches) {
            group.currentStep++
            val node = group.nodeIds.random()
            val color = options.colors.random()
            val shape = options.shapes.random()
            sendBroadcastMessage(
                NodeStepPeripheralMessageUnacked(
                    shape, color, options.flashMode,
                    appkey, model.modelId, model.companyIdentifier,
                    OpCodes.getUnicastMask(node)
                ), true
            )
            // delay between train and start
            delay(options.delay.toLong() - 100)
            sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.START.toByte(), options.timeout, appkey, model.modelId,
                    model.companyIdentifier, OpCodes.getUnicastMask(node)
                )
            )
            scheduledFallback(group, node)
        }
    }

    private fun scheduledFallback(group: TrainingGroup, node: Int) {
        group.missedStepFallback(options.timeout) {
            stepG(group)
            callback?.onAction(
                group.group, group.nodes.find { it.nodeName == node.toString() }, NodeEventStatus(EventType.TIMEOUT, options.timeout),
                EventType.TIMEOUT, options.timeout.toLong()
            )
        }
    }

    override fun finish() {
        repository.getTrainingMessageLiveData().removeObserver()
        if (groupsnitialized()) {
            bulkMessaging(groups) { group ->
                group.stopFallback()
                // delay before end light
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
        callback?.onCompleteTraining()
    }
}