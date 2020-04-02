package com.thinkup.connectivity.impl

import android.content.Context
import android.os.Looper
import com.thinkup.connectivity.BleScheduleTraining
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.*
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.NodeStepPeripheralMessageUnacked
import com.thinkup.connectivity.messges.setup.NodeTrainSetupMessage
import com.thinkup.connectivity.messges.setup.TrainSetup
import com.thinkup.connectivity.utils.EventObserver
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import android.os.Handler
import android.util.Log
import com.thinkup.connectivity.messges.control.NodeControlMessage

class BleScheduleImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BleBaseTraining(context, setting, repository),
    BleScheduleTraining, EventObserver.Callback<NodeEventStatus?> {

    private lateinit var options: ScheduleOptions
//    private lateinit var trainingGroup: List<GroupSteps>
    private val setupMessages = mutableListOf<NodeTrainSetupMessage>()
    private var waitingDeactivation = false
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var currentStep = 0
    private var ended = 0

    companion object {
        const val SETUP_TIMEOUT = 400L
    }

    override fun getDimmerValue(): Int = options.dimmer
    override fun getDistanceValue(): Int = options.distance
    override fun getSoundValue(): Boolean = options.sound

    private val messageObserver = object : EventObserver.Callback<MeshMessage?> {
        override fun onPost(e: MeshMessage?) {
            Log.d("TKUP-NEURAL::EVE", "$e")
            if (e is MeshMessage) {
                //Gets the message that was send to the node with the same srcAddress by checking in the mask of the message
                val msg = setupMessages.find { it.destination?.get(e.src - 1)?.toInt() == 1 }
                msg?.received = true
                if (allSetupResponsesReceived()){
                    callback?.onSettingComplete()
                    stopTimer()
                    startTraining()
                }
            }
        }
    }

    /**
     * set initial config
     */
    override fun set(groups: List<Group>?, options: ScheduleOptions, callback: TrainingCallback) {
        this.options = options
        // clear setup messages queue
        setupMessages.clear()
        callback?.onSettingStart()
        // attach this class like mesh messages observer, to count the setup ack received (re-send if doesn't arrive)
        repository.getMeshMessageCallback().setObserver(messageObserver)
        // prevent don't send any extra message config
        useStartConfigMessage = false
        // call super [BleBaseTraining] to create TrainingGroups and get appkeys to messages
        super.set(groups, callback) {
            this.options = options
            // iterate groups to create setup messages
            println("Thinkup: groups size is ${this.groups.size}")

            this.groups.forEach { tg ->
                // init a sized array to allocate setup messages
                tg.starts = arrayOfNulls(options.steps.size)
                val group = tg.group
                // iterate group nodes by index
                group.ids.forEachIndexed { index, i ->
                    // init steps and timeout's step lists
                    val step = mutableListOf<TrainSetup>()
                    val timeouts = mutableListOf<Int>()
                    // iterate steps by index
                    var stepIndex = 0
                    when (options.starterMethod){
                        StarterMethod.INMEDIATELY -> {}
                        StarterMethod.COUNTDOWN -> {
                            //add traffic lights steps to the steps list and timeouts
                            countdown(step, timeouts)
                            stepIndex = 3
                        }
                        //add white circle to deactivate
                        StarterMethod.DEACTIVATION -> {
                            deactivationMessage(step, timeouts)
                            stepIndex = 1
                        }
                    }
                    options.steps.forEachIndexed { indexSo, so ->
                        // find in the step for this node if exist
                        // if exist, add a [TrainSetup] and timeout
                        val option = so.nodes.find { it.nodeIndex == index }
                        if (option != null) {
                            step.add(TrainSetup(option.shapes.random(), option.color, option.led, indexSo+ stepIndex))
                            timeouts.add(so.timeout)
                        }
                    }
                    if (options.endWithLight){
                        //Add last white circle light to show when training is finished
                        step.add(TrainSetup(ShapeParams.CIRCLE, ColorParams.COLOR_WITHE, PeripheralParams.LED_FAST_FLASH, options.steps.size + stepIndex))
                        timeouts.add(AUTO_OFF_LEDS.toInt())
                    }
                    // create a setup message fot this node using the steps info and global config(dimmer,distance,etc)
                    setupMessages.add(
                        NodeTrainSetupMessage(
                            options.dimmer, PeripheralParams.BOTH, options.distance, if (options.sound) PeripheralParams.BIP_START_HIT else PeripheralParams.NO_SOUND,
                            options.nodesRequired,step,
                            appkey, model.modelId, model.companyIdentifier, OpCodes.getUnicastMask(i)
                        )
                    )
                    // create (or modify if exist) a START message for this node for each step where it participate
                    addStart(tg, i, step, timeouts)
                }
            }
            startSetup()
        }
    }

    /**
     * Create START message [NodeControlMessageUnacked] for each step
     * that message is associated to a group
     */
    private fun addStart(group: TrainingGroup, nodeId: Int, steps: List<TrainSetup>, timeouts: List<Int>) {
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
                group.starts[trainSetup.stepIndex] = StartAction(OpCodes.getMask(nodeId, BASIC_MASK), timeouts[index])
            }
        }
    }

    private fun deactivationMessage(steps: MutableList<TrainSetup>, timeouts: MutableList<Int>) {
        repository.getTrainingMessageCallback().setObserver(this)
        waitingDeactivation = true
            steps.add(TrainSetup(ShapeParams.CIRCLE, ColorParams.COLOR_WITHE, PeripheralParams.LED_PERMANENT, 0))
            timeouts.add(60000)

    }

    private fun stopDeactivation() {
        bulkMessaging(this.groups) {
            sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.STOP.toByte(), 0, appkey, model.modelId, model.companyIdentifier, OpCodes.getGroupMask(it.group.ids)
                )
            )
        }
    }


    private fun startSetup() {
        setupMessages.forEach {
            sendBroadcastMessage(it)
        }
        startTimer(SETUP_TIMEOUT)
    }
    //Starts the timer to check the timeout of receiving all the messages from the setupMesage
    private fun startTimer(timeout: Long){
        runnable = Runnable {
            if (!allSetupResponsesReceived()){
                setupMessages.forEach {
                    if (!it.received){
                        sendBroadcastMessage(it)
                    }
                }
            }else {
                println("Thinkup: setup messages timeout")
                stopTimer()
            }
        }
        handler.postDelayed(runnable!!, timeout )
    }

    private fun stopTimer(){
        handler.removeCallbacks(runnable)
        runnable = null
    }

    override fun startTraining() = executeService {
        println("Thinkup: Training starting")
        repository.isSending = true
        when (options.starterMethod) {
            StarterMethod.INMEDIATELY -> start()
            StarterMethod.COUNTDOWN -> {
                callback?.onCountdown()
                start()
            }
            StarterMethod.DEACTIVATION -> start()
        }
    }

    //add traffic lights steps to the steps list and timeouts
    private fun countdown(steps: MutableList<TrainSetup>, timeouts: MutableList<Int>) {
        for (i in 1..3) {
            steps.add(TrainSetup(ShapeParams.CIRCLE, getCountdownColor(i), PeripheralParams.LED_PERMANENT, i-1))
            timeouts.add(1000)
        }
    }

    override fun start() {
        println("Thinkup: Training started")
        println("Thinkup: groups size is ${this.groups.size}")
        this.groups.forEach {group->
            println("Thinkup: sending start")
            sendStart(group.starts, group)
        }
        callback?.onStartTraining()
    }
    private fun sendStart(starts :Array<StartAction?>, tg: TrainingGroup) {
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
                ))
            tg.missedStepFallback(action.timeout){
                println("Thinkup: hit response timeout ${action.timeout}")
                onPost(NodeEventStatus(EventType.TIMEOUT, action.timeout, tg.group.address))
            }
        }
    }
    //Return true iff all setup responses were received
    private fun allSetupResponsesReceived(): Boolean{
        val notReceivedMsg = setupMessages.find { !it.received }
        return notReceivedMsg == null
    }


    @Synchronized
    override fun onPost(e: NodeEventStatus?) {
     if (e is NodeEventStatus && (e.eventType == EventType.HIT || e.eventType == EventType.TIMEOUT)) {
            Log.d("TKUP-NEURAL::EVE", "$e")
            println("Thinkup: HIT or TIMEOUT EVENT")
            this.groups.forEach { group ->
                if ((group.isFromThis(e.srcAddress))  || (group.address == e.address) ){

                    Log.d("TKUP-NEURAL::EVE", "Group :: ${group.group.name}")
                    val node = getNode(e.srcAddress)
                    val isInCountDown = (options.starterMethod == StarterMethod.COUNTDOWN) && (group.currentStep < 3)
                    val isDeactivation = ((options.starterMethod == StarterMethod.DEACTIVATION) && (group.currentStep < 1))
                    if ((!isInCountDown) && (!isDeactivation)){
                        callback?.onAction(group.group, node, e, e.eventType, e.value.toLong())
                    }
                    if (isDeactivation){
                        waitingDeactivation = false
                        stopDeactivation()
                    }
                    group.starts[group.currentStep]!!.eventsReceived++
                    if (group.starts[group.currentStep]?.count == group.starts[group.currentStep]?.eventsReceived ){
                        Log.d("TKUP-NEURAL::EVE", "Completed:: ${group.currentStep}")
                        group.currentStep++
                        group.lastReceivedStep++
                        Log.d("TKUP-NEURAL::EVE", "Starting:: ${group.currentStep}")
                        group.stopFallback()
                        if (group.currentStep < group.starts.size)
                            sendStart(group.starts, group)
                    }
                    if ((group.currentStep == group.starts.size - 1) || ((options.endWithLight) && (group.currentStep == group.starts.size - 2))) ended++
                    Log.d("TKUP-NEURAL::EVE", "Ended:: $ended")
                }
            }
        }
        if (ended == groups.size) finish()
    }

    override fun stopTraining() {
        finish()
    }


    override fun finish() {
        Log.d("TKUP-NEURAL::", "Finish")
        repository.getTrainingMessageCallback().removeObserver()
        repository.getMeshMessageCallback().removeObserver()
        if (groupsnitialized()) {
            groups.forEach { group ->
                group.stopFallback()
                if (options.endWithLight) {
                    group.currentStep = group.starts.size - 1
                    sendStart(group.starts, group)
                }
            }
        }
        repository.isSending = false
        ended = 0
        callback?.onCompleteTraining()
    }
}