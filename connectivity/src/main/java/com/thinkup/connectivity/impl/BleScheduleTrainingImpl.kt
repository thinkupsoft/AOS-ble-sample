package com.thinkup.connectivity.impl

import android.content.Context
import android.util.Log
import androidx.lifecycle.Observer
import com.thinkup.connectivity.BleScheduleTraining
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.*
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.NodeStepPeripheralMessageUnacked
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.Group
import java.util.*

class BleScheduleTrainingImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BleBaseTraining(context, setting, repository),
    BleScheduleTraining {

    private lateinit var options: ScheduleOptions
    private lateinit var trainingGroup: List<GroupSteps>

    override fun getDimmerValue(): Int = options.dimmer
    override fun getDistanceValue(): Int = options.distance
    override fun getSoundValue(): Boolean = options.sound

    private val eventObserver = Observer<NodeEventStatus?> {
        Log.d("TKUP-NEURAL::", "Unprocess event:: $it")
        var ended = 0
        if (it is NodeEventStatus && (it.eventType == EventType.HIT || it.eventType == EventType.TIMEOUT)) {

        }
        if (ended == groups.size) finish()
    }

    override fun start() {
        Log.d("TKUP-NEURAL::", "Start")
        repository.flushTrainingMessageLiveData()
        repository.getTrainingMessageLiveData().observeForever(eventObserver)
        callback.onStartTraining()
        stepFirst()
    }

    private fun stepFirst() = executeService {
        bulkMessaging(trainingGroup) {
            Log.d("TKUP-NEURAL::", it.group.toString())
            it.actions.forEach { action ->
                executeService {
                    val node = it.group.nodes[action.nodeIndex]
                    Log.d("TKUP-NEURAL::", "node ${node.nodeName}")
                    action.steps.firstOrNull { s -> !s.sended }?.let {

                    }
                }
            }
        }
    }

    private fun generateSteps() {
        val indexes = mutableSetOf<Int>()
        options.steps.forEach { it.nodes.map { step -> indexes.add(step.nodeIndex) } }


        val actions = mutableListOf<Action>()
        indexes.forEach {
            val steps = mutableListOf<StepNodeConfig>()
            options.steps.forEachIndexed { index, s ->
                s.nodes.find { n -> n.nodeIndex == it }?.let { snc ->
                    snc.stepIndex = index
                    snc.sended = false
                    steps.add(snc)
                }
            }
            actions.add(Action(it, steps.toList()))
            Unit
        }

        trainingGroup = groups.map { GroupSteps(it, actions) }

        trainingGroup[0].actions[0].steps[0].sended = true
    }

    override fun finish() {
        Log.d("TKUP-NEURAL::", "Finish")
        repository.getTrainingMessageLiveData().removeObserver(eventObserver)
        if (groupsnitialized()) {
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
        }
        repository.isSending = false
        callback.onCompleteTraining()
    }

    override fun set(groups: List<Group>?, options: ScheduleOptions, callback: TrainingCallback) = executeService {
        this.options = options
        repository.getTrainingMessageLiveData().removeObserver(eventObserver)
        if (options.randomFirstStep) Collections.rotate(options.steps, options.steps.size)
        super.set(groups, callback) { generateSteps() }
    }

    override fun startTraining() = executeService {
        repository.isSending = true
        when (options.starterMethod) {
            StarterMethod.INMEDIATELY -> start()
            StarterMethod.COUNTDOWN -> countdown()
            StarterMethod.DEACTIVATION -> {
                // TODO
            }
        }
    }

    override fun stopTraining() {
        finish()
    }
}