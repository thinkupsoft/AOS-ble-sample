package com.thinkup.connectivity.impl

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.thinkup.connectivity.BleFastTraining
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.*
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessageStatus
import com.thinkup.connectivity.messges.setup.NodeTrainSetupMessage
import com.thinkup.connectivity.messges.setup.TrainSetup
import com.thinkup.connectivity.utils.EventObserver
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage

class BleFastTrainingImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BleBaseTraining(context, setting, repository),
    BleFastTraining, EventObserver.Callback<NodeEventStatus?> {

    companion object {
        const val SETUP_TIMEOUT = 250L
    }

    private lateinit var options: FastOptions
    private var lastLightSent = false
    private var stepTimeoutAchieved = false
    private var ended = 0
    private val setupMessages = mutableListOf<NodeTrainSetupMessage>()
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = Runnable {}

    override fun getDimmerValue(): Int = options.dimmer
    override fun getDistanceValue(): Int = options.distance
    override fun getSoundValue(): Boolean = options.sound

    private val messageObserver = object : EventObserver.Callback<MeshMessage?> {
        override fun onPost(e: MeshMessage?) {
            Log.d("TKUP-NEURAL::EVE", "$e")
            if ((e is MeshMessage) && (e is NodePeripheralMessageStatus)) {
                stopTimer()
                //Gets the message that was send to the node with the same srcAddress by checking in the mask of the message
                val msg = setupMessages.find { it.destination?.get(e.src - 1)!!.toString().toInt() == 1 }
                msg?.received = true
                val next = setupMessages.firstOrNull { !it.received }

                next?.let {
                    sendSetup(it)
                } ?: run {
                    callback?.onSettingComplete()
                }
            }
        }
    }

    @Synchronized
    override fun onPost(e: NodeEventStatus?) {
        if (e is NodeEventStatus && (e.eventType == EventType.HIT || e.eventType == EventType.TIMEOUT)) {
            Log.d("TKUP-NEURAL::EVE", "$e")
            println("Thinkup: HIT or TIMEOUT EVENT")
            this.groups.forEach { group ->
                if ((group.isFromThis(e.srcAddress)) || (group.address == e.address)) {
                    Log.d("TKUP-NEURAL::EVE", "Group :: ${group.group.name}")
                    val node = getNode(e.srcAddress)
                    val isInCountDown = (options.startWithCountdown) && (group.currentStep < 3)
                    if (!isInCountDown) {
                        if (stepTimeoutAchieved && (group.currentStep < group.starts.size)) {
                            val eventsRemaining = ((group.starts[group.currentStep]!!.count) - (group.starts[group.currentStep]!!.eventsReceived))
                            for (i in 1..eventsRemaining) {
                                callback?.onAction(group.group, node, e, e.eventType, e.value.toLong())
                                group.starts[group.currentStep]!!.eventsReceived++
                            }
                            stepTimeoutAchieved = false
                        } else {
                            callback?.onAction(group.group, node, e, e.eventType, e.value.toLong())
                        }
                    } else {
                        if (group.currentStep == 2) {
                            //Countdown finished
                            callback?.onStartTraining()
                        }
                    }

                    if ((group.currentStep < group.starts.size)) {
                        group.starts[group.currentStep]!!.eventsReceived++
                        if (group.starts[group.currentStep]!!.count <= group.starts[group.currentStep]!!.eventsReceived) {
                            group.stopFallback()
                            stepCompleted(group)
                        }
                    }
                    if ((group.currentStep == group.starts.size) || ((options.endWithLight) && (group.currentStep == group.starts.size - 1))) ended++
                    Log.d("TKUP-NEURAL::EVE", "Ended:: $ended")
                }
            }
        }
        if (ended == groups.size) finish()
    }

    private fun sendSetup(msg: NodeTrainSetupMessage) {
        sendBroadcastMessage(msg)
        startTimer(msg, SETUP_TIMEOUT)
    }

    //Starts the timer to check the timeout of receiving all the messages from the setupMessage
    private fun startTimer(msg: NodeTrainSetupMessage, timeout: Long) {
        runnable = Runnable { sendSetup(msg) }
        handler.postDelayed(runnable, timeout)
    }

    private fun stopTimer() {
        handler.removeCallbacks(runnable)
    }

    override fun set(groups: List<Group>?, options: FastOptions, callback: TrainingCallback) = executeService {
        this.options = options
        // clear setup messages queue
        setupMessages.clear()
        callback.onSettingStart()
        // attach this class like mesh messages observer, to count the setup ack received (re-send if doesn't arrive)
        repository.getMeshMessageCallback().setObserver(messageObserver)
        //attach this class like events observer, to receive hit or timeout of each step
        repository.getTrainingMessageCallback().setObserver(this)
        // prevent don't send any extra message config
        useStartConfigMessage = false
        super.set(groups, callback) {
            this.groups.forEach { tg ->
                // init a sized array to allocate setup messages
                var arraySize = options.touches
                //Starter methods are consider as steps in TrainSetup
                if (options.startWithCountdown) {
                    arraySize += 3
                }
                if (options.endWithLight) {
                    arraySize += 1
                }
                tg.starts = arrayOfNulls(arraySize)
                val group = tg.group
                // init steps and timeout's step lists
                val step = mutableListOf<TrainSetup>()
                val timeouts = mutableListOf<Int>()
                // iterate steps by index
                var stepIndex = 0
                if (options.startWithCountdown) {
                    //add traffic lights steps to the steps list and timeouts
                    countdown(step, timeouts)
                    stepIndex = 3
                }
                for (index in 0 until options.touches) {
                    step.add(TrainSetup(options.shapes.random(), options.colors.random(), options.flashMode, index + stepIndex))
                    timeouts.add(options.timeout)
                }
                val nodeSteps:  Array<MutableList<TrainSetup>> = Array(group.ids.size){ mutableListOf<TrainSetup>()}
                if (options.startWithCountdown) {
                    group.ids.forEachIndexed { index, i ->
                        countdown(nodeSteps[index], timeouts)
                    }
                }
                // random id node for each step
               step.forEach {
                   nodeSteps[group.ids.indexOf(group.ids.random())].add(it)
               }
                nodeSteps.forEachIndexed { index, nodeStep ->
                    if (options.endWithLight) {
                        //Add last white circle light to show when training is finished
                        nodeStep.add(
                            TrainSetup(
                                ShapeParams.CIRCLE,
                                ColorParams.COLOR_WITHE,
                                PeripheralParams.LED_FAST_FLASH,
                                options.touches + stepIndex
                            )
                        )
                        timeouts.add(AUTO_OFF_LEDS.toInt())
                    }
                    // create a setup message for this node using the steps info and global config(dimmer,distance,etc)
                    setupMessages.add(
                        NodeTrainSetupMessage(
                            options.dimmer,
                            PeripheralParams.BOTH,
                            options.distance,
                            if (options.sound) PeripheralParams.BIP_START_HIT else PeripheralParams.NO_SOUND,
                            options.nodesRequired,
                            nodeStep,
                            appkey,
                            model.modelId,
                            model.companyIdentifier,
                            OpCodes.getUnicastMask(group.ids[index])
                        )
                    )
                    // create (or modify if exist) a START message for this node for each step where it participate
                    addStart(tg, group.ids[index], step)
                }
            }
            startSetup()
        }
    }

    /**
     * Create START message [NodeControlMessageUnacked] for each step
     * that message is associated to a group
     */
    private fun addStart(group: TrainingGroup, nodeId: Int, steps: List<TrainSetup>) {
        // fora each step, look for a START created or create a new message if not exist
        steps.forEachIndexed { index, trainSetup ->
            // get a step start
            val action = group.starts.elementAtOrNull(trainSetup.stepIndex)
            action?.let {
                // update destination mask and HIT count expected
                it.ids = OpCodes.getMask(nodeId, it.ids)
                it.count = it.count + 1
            } ?: run {
                // create a new StartAction
                group.starts[trainSetup.stepIndex] = StartAction(OpCodes.getMask(nodeId, BASIC_MASK),
                    if (options.startWithCountdown && trainSetup.stepIndex < 3) COUNTDOWN_TIMEOUT else
                        if (options.endWithLight && trainSetup.stepIndex == group.starts.size -1) AUTO_OFF_LEDS.toInt()
                        else options.timeout)
            }
        }
    }

    private fun startSetup() = executeService {
        val msg = setupMessages.first { !it.received }
        sendSetup(msg)
    }

    override fun startTraining() = executeService {
        repository.isSending = true
        stopTimer()
        if (options.startWithCountdown)
            callback?.onCountdown()
        start()
    }

    override fun stopTraining() {
        finish()
    }

    override fun start() = executeService {
        this.groups.forEach { group ->
            sendStart(group.starts, group)
        }
        if (options.startWithCountdown)
            callback?.onStartTraining()
    }

    private fun sendStart(starts: Array<StartAction?>, tg: TrainingGroup) {
        println("Thinkup: start was sent")
        val action = starts[tg.currentStep]
        if (action is StartAction) {
            sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.START.toByte(),
                    action.timeout,
                    this.appkey,
                    model.modelId,
                    model.companyIdentifier,
                    action.ids
                )
            )
            tg.missedStepFallback(action.timeout) {
                println("Thinkup: hit response timeout ${action.timeout}")
                stepTimeoutAchieved = true
                onPost(NodeEventStatus(EventType.TIMEOUT, action.timeout, tg.group.address))
            }
        }
    }

    private fun stepCompleted(group: TrainingGroup) {
        Log.d("TKUP-NEURAL::EVE", "Completed:: ${group.currentStep}")
        group.currentStep++
        group.lastReceivedStep++
        Log.d("TKUP-NEURAL::EVE", "Starting:: ${group.currentStep}")
        if (group.currentStep < group.starts.size) {
            if ((options.endWithLight) && (group.currentStep == group.starts.size - 1)) {
                lastLightSent = true
                sendLedOnMsg(group.starts, group)
            } else {
                sendStart(group.starts, group)
            }
        }
    }

    //Set LED ON with last light to indicate the training was finished
    private fun sendLedOnMsg(starts: Array<StartAction?>, tg: TrainingGroup) {
        println("Thinkup: start was sent")
        val action = starts[tg.currentStep]
        if (action is StartAction) {
            autoOffLedMessage(action.ids,this.appkey, model.modelId, model.companyIdentifier, action.timeout.toLong())
        }
    }

    override fun finish() {
        repository.getTrainingMessageCallback().removeObserver()
        repository.getMeshMessageCallback().removeObserver()
        if (groupsInitialized()) {
            groups.forEach { group ->
                group.stopFallback()
                if (options.endWithLight && !lastLightSent) {
                    group.currentStep = group.starts.size - 2
                    stepCompleted(group)
                }
            }
        }
        repository.isSending = false
        ended = 0
        callback?.onCompleteTraining()
    }
}