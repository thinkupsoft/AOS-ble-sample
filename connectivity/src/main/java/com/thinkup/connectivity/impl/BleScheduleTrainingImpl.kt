package com.thinkup.connectivity.impl

import android.content.Context
import android.util.Log
import com.thinkup.connectivity.BleScheduleTraining
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.*
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.NodeStepPeripheralMessageUnacked
import com.thinkup.connectivity.utils.EventObserver
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import java.util.*
import kotlin.random.Random

class BleScheduleTrainingImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BleBaseTraining(context, setting, repository),
    BleScheduleTraining, EventObserver.Callback<NodeEventStatus?> {

    private lateinit var options: ScheduleOptions
    private lateinit var trainingGroup: List<GroupSteps>
    private var waitingDeactivation = false
    private var ended = 0

    override fun getDimmerValue(): Int = options.dimmer
    override fun getDistanceValue(): Int = options.distance
    override fun getSoundValue(): Boolean = options.sound

    override fun onPost(e: NodeEventStatus?) {
        if (waitingDeactivation && e?.eventType == EventType.HIT) {
            waitingDeactivation = false
            stopDeactivation()
            start()
        } else if (e is NodeEventStatus && (e.eventType == EventType.HIT || e.eventType == EventType.TIMEOUT)) {
            Log.d("TKUP-NEURAL::EVE", "$e")
            trainingGroup.forEach { tg ->
                val group = tg.group
                if (group.isFromThis(e.srcAddress)) {
                    Log.d("TKUP-NEURAL::EVE", "Group :: ${group.group.name}")
                    tg.steps[group.currentStep].actual++
                    val node = getNode(e.srcAddress)
                    callback?.onAction(group.group, node, e, e.eventType, e.value.toLong())
                    Log.d("TKUP-NEURAL::EVE", "Config node:: ${node?.nodeName}")
                    configNextStep(tg, node)
                    if (tg.steps[group.currentStep].isCompleted()) {
                        Log.d("TKUP-NEURAL::EVE", "Completed:: ${group.currentStep}")
                        group.currentStep++
                        group.lastReceivedStep++
                        if (group.currentStep < tg.steps.size) {
                            Log.d("TKUP-NEURAL::EVE", "Starting:: ${group.currentStep}")
                            startNextStep(tg)
                        }
                    }
                    if (group.currentStep == tg.steps.size) ended++
                    Log.d("TKUP-NEURAL::EVE", "Ended:: $ended")
                }
            }
        }
        if (ended == groups.size) finish()
    }

    override fun start() = executeService {
        Log.d("TKUP-NEURAL::", "Start")
        bulkMessaging(groups) { group ->
            sendMessage(
                group.group,
                NodeControlMessageUnacked(ControlParams.SET_LED_OFF.toByte(), NO_CONFIG, appkey, model.modelId, model.companyIdentifier),
                true
            )
        }
        repository.getTrainingMessageCallback().setObserver(this)
        ended = 0
        callback?.onStartTraining()
        delay(trainingGroup[0].actions[0].steps[0].delay - DELTA_STEP_DELAY)
        stepFirst()
    }

    private fun configNextStep(trainingGroup: GroupSteps, node: ProvisionedMeshNode?) = executeService {
        node?.let {
            val index = trainingGroup.group.nodes.indexOfFirst { it.unicastAddress == node.unicastAddress }
            trainingGroup.actions.find { it.nodeIndex == index }?.steps?.firstOrNull { s -> !s.sended }?.let { snc ->
                val shape = snc.shapes.random()
                Log.d("TKUP-NEURAL::EVE", "Configured ${shape}, ${snc.color}, ${snc.led}")
                sendBroadcastMessage(
                    NodeStepPeripheralMessageUnacked(
                        shape, snc.color, snc.led, appkey, model.modelId, model.companyIdentifier, OpCodes.getUnicastMask(node.nodeName.toInt())
                    )
                )
                snc.sended = true
            }
        }
    }

    private fun startNextStep(step: GroupSteps) = executeService {
        println("Thinkup: Next Step")

        val currentStep = step.group.currentStep
        val stepActions = mutableListOf<StepNodeConfig>()
        step.actions.forEach { action ->
            action.steps.firstOrNull { s -> s.stepIndex == currentStep }?.let { snc ->
                stepActions.add(snc)
            }
        }
        delay(stepActions[0].delay - DELTA_STEP_DELAY)
        bulkMessaging(stepActions) { snc ->
            val node = step.group.nodes[snc.nodeIndex]
            Log.d("TKUP-NEURAL::EVE", "Started ${snc.timeout}")
            sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.START.toByte(), snc.timeout, appkey, model.modelId, model.companyIdentifier, OpCodes.getUnicastMask(node.nodeName.toInt())
                )
            )
        }
    }

    private fun stepFirst() = executeService {
        bulkMessaging(trainingGroup) {
            println("Thinkup: First Step")
            val currentStep = it.group.currentStep
            val stepActions = mutableListOf<StepNodeConfig>()
            Log.d("TKUP-NEURAL::", it.group.toString())
            it.actions.forEach { action ->
                action.steps.firstOrNull { s -> s.stepIndex == currentStep }?.let { snc ->
                    stepActions.add(snc)
                }
                executeService {
                    val node = it.group.nodes[action.nodeIndex]
                    Log.d("TKUP-NEURAL::", "node ${node.nodeName}")
                    action.steps.firstOrNull { s -> !s.sended }?.let { snc ->
                        val shape = snc.shapes.random()
                        sendBroadcastMessage(
                            NodeStepPeripheralMessageUnacked(
                                shape, snc.color, snc.led, appkey, model.modelId, model.companyIdentifier, OpCodes.getUnicastMask(node.nodeName.toInt())
                            )
                        )
                        snc.sended = true
                    }
                }
            }
            bulkMessaging(stepActions) { snc ->
                val node = it.group.nodes[snc.nodeIndex]
                sendBroadcastMessage(
                    NodeControlMessageUnacked(
                        ControlParams.START.toByte(), snc.timeout, appkey, model.modelId, model.companyIdentifier, OpCodes.getUnicastMask(node.nodeName.toInt())
                    )
                )
            }
        }
    }

    private fun generateSteps() {
        val indexes = mutableSetOf<Int>()
        val simples = mutableListOf<SimpleStep>()
        options.steps.forEach {
            it.nodes.map { step -> indexes.add(step.nodeIndex) }
            simples.add(SimpleStep(it.nodes.size))
        }


        val actions = mutableListOf<Action>()
        indexes.forEach {
            val steps = mutableListOf<StepNodeConfig>()
            options.steps.forEachIndexed { index, s ->
                s.nodes.find { n -> n.nodeIndex == it }?.let { snc ->
                    snc.stepIndex = index
                    snc.sended = false
                    snc.timeout = s.timeout
                    snc.delay = s.delay
                    steps.add(snc)
                }
            }
            actions.add(Action(it, steps.toList()))
            Unit
        }

        trainingGroup =
            groups.map { GroupSteps(it, actions.map { a -> Action(a.nodeIndex, a.steps.map { s -> s.copy() }) }, simples.map { s -> s.copy() }) }
    }

    override fun finish() {
        Log.d("TKUP-NEURAL::", "Finish")
        repository.getTrainingMessageCallback().removeObserver()
        repository.getMeshMessageCallback().removeObserver()
        if (groupsnitialized()) {
            bulkMessaging(groups) { group ->
                group.stopFallback()
                delay(REPLICATE_DELAY)
                if (options.endWithLight) {
                    autoOffLedMessage(
                        group.group,
                        NodeStepPeripheralMessageUnacked(
                            ShapeParams.CIRCLE, ColorParams.COLOR_WITHE, PeripheralParams.LED_FAST_FLASH,
                            appkey, model.modelId, model.companyIdentifier, OpCodes.getGroupMask(group.group.ids)
                        )
                    )
                }
            }
        }
        repository.isSending = false
        ended = 0
        callback?.onCompleteTraining()
    }

    private fun deactivationMessage() {
        repository.getTrainingMessageCallback().setObserver(this)
        waitingDeactivation = true
        bulkMessaging(trainingGroup) {
            sendBroadcastMessage(
                NodeStepPeripheralMessageUnacked(
                    ShapeParams.CIRCLE, ColorParams.COLOR_WITHE, PeripheralParams.LED_PERMANENT,
                    appkey, model.modelId, model.companyIdentifier, OpCodes.getGroupMask(it.group.group.ids)
                )
            )
            delay(DELTA_STEP_DELAY)
            sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.START.toByte(), 60000, appkey, model.modelId, model.companyIdentifier, OpCodes.getGroupMask(it.group.group.ids)
                )
            )
        }
    }

    private fun stopDeactivation() {
        bulkMessaging(trainingGroup) {
            sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.STOP.toByte(), 0, appkey, model.modelId, model.companyIdentifier, OpCodes.getGroupMask(it.group.group.ids)
                )
            )
        }
    }

    override fun set(groups: List<Group>?, options: ScheduleOptions, callback: TrainingCallback) = executeService {
        this.options = options
        if (options.randomFirstStep) Collections.rotate(this.options.steps, Random.nextInt(0, this.options.steps.size))
        ended = 0
        super.set(groups, callback) { generateSteps() }
    }

    override fun startTraining() = executeService {
        repository.isSending = true
        when (options.starterMethod) {
            StarterMethod.INMEDIATELY -> start()
            StarterMethod.COUNTDOWN -> {
                callback?.onCountdown()
                countdown()}
            StarterMethod.DEACTIVATION -> deactivationMessage()
        }
    }

    override fun stopTraining() {
        finish()
    }
}