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

class BleFastTrainingImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BleBaseTraining(context, setting, repository),
    BleFastTraining, EventObserver.Callback<NodeEventStatus?> {

    private lateinit var options: FastOptions

    override fun getDimmerValue(): Int = options.dimmer
    override fun getDistanceValue(): Int = options.distance
    override fun getSoundValue(): Boolean = options.sound

    @Synchronized
    override fun onPost(eventStatus: NodeEventStatus?) {
        var ended = 0
        Log.d("TKUP::", eventStatus.toString())
        if (eventStatus != null) {
            if (eventStatus.eventType == EventType.HIT || eventStatus.eventType == EventType.TIMEOUT) {
                groups.forEach { group ->
                    if (group.isFromThis(eventStatus.srcAddress) && group.currentStep > group.lastReceivedStep) {
                        Log.d("TKUP::", "Group = ${group.address}")
                        group.lastReceivedStep++
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
        repository.getTrainingMessageCallback().setObserver(this)
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
        delay(DELTA_STEP_DELAY)
        firstStep(groups)
    }

    @Synchronized
    private fun firstStep(groups: List<TrainingGroup>) = executeService {
        val ids = mutableListOf<Int>()
        var shape = 0
        var color = 0
        groups.forEach { group ->
            group.currentStep++
            ids.add(group.nodeIds.random())
            color = options.colors.random()
            shape = options.shapes.random()
        }
        sendBroadcastMessage(
            NodeStepPeripheralMessageUnacked(
                shape, color, options.flashMode,
                appkey, model.modelId, model.companyIdentifier,
                OpCodes.getGroupMask(ids)
            ), true
        )
        // delay between train and start
        delay(options.delay.toLong() - DELTA_STEP_DELAY)

        val messsage = NodeControlMessageUnacked(
            ControlParams.START.toByte(), options.timeout, appkey, model.modelId,
            model.companyIdentifier, OpCodes.getGroupMask(ids)
        )

        // Replicate same message, because is unack
        sendBroadcastMessage(messsage, true)
        delay(REPLICATE_DELAY)
        sendBroadcastMessage(messsage, true)
    }

    @Synchronized
    private fun stepG(group: TrainingGroup) = executeService {
        Log.d("TKUP::", "CSteps=${group.currentStep} - TSteps=${options.touches}")
        if (group.currentStep < options.touches) {
            group.currentStep++
            val node = group.nodeIds.random()
            val color = options.colors.random()
            val shape = options.shapes.random()
            Log.d("TKUP::", "Node=${node} - Color=${color} - Shape=${shape}")
            sendBroadcastMessage(
                NodeStepPeripheralMessageUnacked(
                    shape, color, options.flashMode,
                    appkey, model.modelId, model.companyIdentifier,
                    OpCodes.getUnicastMask(node)
                ), true
            )
            // delay between train and start
            delay(options.delay.toLong() - DELTA_STEP_DELAY)
            val messsage = NodeControlMessageUnacked(
                ControlParams.START.toByte(), options.timeout, appkey, model.modelId,
                model.companyIdentifier, OpCodes.getUnicastMask(node)
            )
            // Replicate same message, because is unack
            sendBroadcastMessage(messsage, true)
            delay(REPLICATE_DELAY)
            sendBroadcastMessage(messsage, true)
        }
    }

    override fun finish() {
        repository.getTrainingMessageCallback().removeObserver()
        if (groupsnitialized()) {
            groups.forEach { group ->
                group.stopFallback()
            }
            if (options.endWithLight) {
                sendBroadcastMessage(
                    NodeStepPeripheralMessageUnacked(
                        ShapeParams.CIRCLE, ColorParams.COLOR_WITHE, PeripheralParams.LED_FAST_FLASH,
                        appkey, model.modelId, model.companyIdentifier, OpCodes.getGroupMask(allNodeIds.toList())
                    ), true
                )
                autoOffLedMessage(allNodeIds.toList(), appkey, model.modelId, model.companyIdentifier)
            }
        }
        repository.isSending = false
        callback?.onCompleteTraining()
    }
}