package com.thinkup.connectivity.impl

import android.content.Context
import com.thinkup.connectivity.BleScheduleTraining
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.*
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.BASIC_MASK
import com.thinkup.connectivity.messges.OpCodes
import com.thinkup.connectivity.messges.PeripheralParams
import com.thinkup.connectivity.messges.setup.NodeTrainSetupMessage
import com.thinkup.connectivity.messges.setup.TrainSetup
import com.thinkup.connectivity.utils.EventObserver
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage

class BleScheduleImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BleBaseTraining(context, setting, repository),
    BleScheduleTraining, EventObserver.Callback<MeshMessage?> {

    private lateinit var options: ScheduleOptions
    private lateinit var trainingGroup: List<GroupSteps>
    private val setupMessages = mutableListOf<NodeTrainSetupMessage>()

    override fun getDimmerValue(): Int = options.dimmer
    override fun getDistanceValue(): Int = options.distance
    override fun getSoundValue(): Boolean = options.sound

    /**
     * set initial config
     */
    override fun set(groups: List<Group>?, options: ScheduleOptions, callback: TrainingCallback) {
        // clear setup messages queue
        setupMessages.clear()
        // attach this class like mesh messages observer, to count the setup ack received (re-send if doesn't arrive)
        repository.getMeshMessageCallback().setObserver(this)
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
                            options.dimmer, PeripheralParams.BOTH, options.distance, options.distance, step,
                            appkey, model.modelId, model.companyIdentifier, OpCodes.getUnicastMask(i)
                        )
                    )
                    // create (or modify if exist) a START message for this node for each step where it participate
                    addStart(tg, i, step, timeouts)
                }
            }
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

    override fun start() {

    }

    override fun startTraining() {

    }

    override fun stopTraining() {

    }

    @Synchronized
    override fun onPost(e: MeshMessage?) {

    }

    override fun finish() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}