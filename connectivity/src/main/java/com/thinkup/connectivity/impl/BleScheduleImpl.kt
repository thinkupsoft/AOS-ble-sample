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
    private lateinit var trainingGroup: List<GroupSteps>
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
        // clear setup messages queue
        setupMessages.clear()
        // attach this class like mesh messages observer, to count the setup ack received (re-send if doesn't arrive)
        repository.getMeshMessageCallback().setObserver(messageObserver)
        // prevent don't send any extra message config
        useStartConfigMessage = false
        // call super [BleBaseTraining] to create TrainingGroups and get appkeys to messages
        super.set(groups, callback) {
            // iterate groups to create setup messages
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
                    options.steps.forEachIndexed { indexSo, so ->
                        // find in the step for this node if exist
                        // if exist, add a [TrainSetup] and timeout
                        val option = so.nodes.find { it.nodeIndex == index }
                        if (option != null) {
                            step.add(TrainSetup(option.shapes.random(), option.color, option.led, indexSo))
                            timeouts.add(so.timeout)
                        }
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
        }
        startSetup()
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


    private fun startSetup() {
        callback?.onSettingStart()
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
            }else stopTimer()
        }
        handler.postDelayed(runnable!!, timeout )
    }

    private fun stopTimer(){
        handler.removeCallbacks(runnable)
        runnable = null
    }

    override fun startTraining() = executeService {
        repository.isSending = true
        when (options.starterMethod) {
            StarterMethod.INMEDIATELY -> start()
            StarterMethod.COUNTDOWN -> {
                callback?.onCountdown()
                countdown()
            }
            StarterMethod.DEACTIVATION -> deactivationMessage()
        }
    }

    override fun start() {
        this.groups.forEach {group->
            sendStart(group.starts, group)
        }
    }
    private fun sendStart(starts :Array<StartAction?>, tg: TrainingGroup) {
        val action = starts[currentStep]
        if (action is StartAction) {
            sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.START.toByte(),
                    (action.timeout * 1000),
                    this.appkey,
                    model.modelId,
                    model.companyIdentifier,
                    action.ids
                ))
            tg.missedStepFallback((action.timeout * 1000),{
                sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.START.toByte(),
                    (action.timeout * 1000),
                    this.appkey,
                    model.modelId,
                    model.companyIdentifier,
                    action.ids
                ))})
        }
    }
    //Return true iff all setup responses were received
    private fun allSetupResponsesReceived(): Boolean{
        val notReceivedMsg = setupMessages.find { !it.received }
        return notReceivedMsg == null
    }


    @Synchronized
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
                    if (tg.steps[group.currentStep].isCompleted()) {
                        Log.d("TKUP-NEURAL::EVE", "Completed:: ${group.currentStep}")
                        group.currentStep++
                        group.lastReceivedStep++
                        Log.d("TKUP-NEURAL::EVE", "Starting:: ${group.currentStep}")
                        group.stopFallback()
                        sendStart(tg.group.starts, group)

                    }
                    if (group.currentStep == tg.steps.size) ended++
                    Log.d("TKUP-NEURAL::EVE", "Ended:: $ended")
                }
            }
        }
        if (ended == groups.size) finish()    }

    override fun stopTraining() {
        finish()
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
        callback?.onCompleteTraining()    }
}