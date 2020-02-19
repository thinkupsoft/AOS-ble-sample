package com.thinkup.connectivity.impl

import android.content.Context
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.common.TrainingCallback
import com.thinkup.connectivity.common.TrainingGroup
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import com.thinkup.connectivity.messges.peripheral.NodePrePeripheralMessageUnacked
import com.thinkup.connectivity.messges.peripheral.NodeStepPeripheralMessageUnacked
import kotlinx.coroutines.delay
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel

abstract class BleBaseTraining(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BaseBleImpl(context, setting, repository) {

    protected var callback: TrainingCallback? = null
    protected lateinit var appkey: ApplicationKey
    protected lateinit var model: VendorModel
    protected lateinit var groups: List<TrainingGroup>

    abstract fun start()
    abstract fun finish()
    abstract fun getDimmerValue(): Int
    abstract fun getDistanceValue(): Int
    abstract fun getSoundValue(): Boolean

    private fun getMessagesKeys() {
        bulkMessaging(groups) { group ->
            if (checkInitialized()) {
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
        bulkMessaging(groups) { group ->
            val dimmer = when (getDimmerValue()) {
                0 -> 0X05
                1 -> 0X32
                else -> 0X64
            }
            val distance = when (getDistanceValue()) {
                0 -> PeripheralParams.LOW
                1 -> PeripheralParams.MIDDLE
                else -> PeripheralParams.HIGH
            }
            sendMessage(
                group.group,
                NodePrePeripheralMessageUnacked(
                    dimmer, PeripheralParams.BOTH, distance,
                    if (getSoundValue()) PeripheralParams.BIP_START else PeripheralParams.NO_SOUND,
                    appkey, model.modelId, model.companyIdentifier
                ),
                true
            )
        }
    }

    protected fun set(groups: List<Group>?, callback: TrainingCallback, action: (() -> Unit)? = null) = executeService {
        this.groups = groups?.map {
            val nodes = getGroupNodes(it)
            TrainingGroup(it.address, it, nodes, nodes.map { n -> n.nodeName.toInt() }, 0, 0)
        } ?: getGroups().value!!.map {
            val nodes = getGroupNodes(it)
            TrainingGroup(it.address, it, nodes, nodes.map { n -> n.nodeName.toInt() }, 0, 0)
        }
        this.callback = callback
        repository.isSending = true
        callback.onSettingStart()
        action?.invoke()
        getMessagesKeys()
        starterConfig()
        // delay despues de pre config
        delay(200)
        repository.isSending = false
        callback.onSettingComplete()
    }

    protected suspend fun countdown() {
        for (i in 1..3) {
            bulkMessaging(groups) { group ->
                sendMessage(
                    group.group,
                    NodeStepPeripheralMessageUnacked(
                        ShapeParams.CIRCLE, getCountdownColor(i), PeripheralParams.LED_PERMANENT,
                        appkey, model.modelId, model.companyIdentifier
                    ), true
                )
                if (i == 1) {
                    // despues de circulo verde de countdown
                    delay(20)
                    sendMessage(
                        group.group,
                        NodeControlMessageUnacked(ControlParams.SET_LED_ON.toByte(), NO_CONFIG, appkey, model.modelId, model.companyIdentifier),
                        true
                    )
                }
            }
            // entre cada circulo de countdown
            delay(1000)
        }
        start()
    }

    protected fun getCountdownColor(index: Int): Int = when (index) {
        3 -> ColorParams.COLOR_GREEN
        2 -> ColorParams.COLOR_YELLOW
        else -> ColorParams.COLOR_RED
    }

    protected fun checkInitialized() = !::appkey.isInitialized || !::model.isInitialized

    protected fun groupsnitialized() = ::groups.isInitialized
}