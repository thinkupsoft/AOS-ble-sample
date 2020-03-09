package com.thinkup.connectivity.common

import android.content.Context
import android.os.Handler
import androidx.lifecycle.LiveData
import com.thinkup.connectivity.BleConnection
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.BROADCAST
import com.thinkup.connectivity.messges.ControlParams
import com.thinkup.connectivity.messges.OpCodes
import com.thinkup.connectivity.messges.control.NodeControlMessageUnacked
import com.thinkup.connectivity.utils.TimeoutLiveData
import kotlinx.coroutines.*
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import kotlin.coroutines.CoroutineContext

open class BaseBleImpl(protected val context: Context, protected val setting: BleSetting, protected val repository: NrfMeshRepository) :
    BleConnection {

    protected val ACTION_TIMEOUT = 3 * 1000L // 3 sec
    protected val STEP_TIMEOUT = 2 * 1000L // 2 sec
    protected val PROVISION_TIMEOUT = 60 * 1000L // 30 sec - Timeout to provision step
    protected val KEEP_ALIVE = 10 * 1000L // 15  sec - Time to send keep alive message
    protected val KEEP_ALIVE_WAIT = 3 * 1000L // 3  sec - Time to recollect keep alive responses
    protected val BULK_DELAY = 20L
    protected val REPLICATE_DELAY = 20L
    protected val DELTA_STEP_DELAY = 75L
    protected val AUTO_OFF_LEDS = 2500L // Auto-off leds after the info has been shown

    override fun settings(): BleSetting = setting

    override fun isConnected() = repository.isConnectedToProxy()

    override fun getBatteryAverage(): LiveData<Int?> = repository.getBatteryAverage()

    override fun getProvisionedNodes(): LiveData<List<ProvisionedMeshNode>> {
        return repository.getNodes()
    }

    override fun getNodes() = getProvisionedNodes().value

    protected fun getNode(address: Int) = repository.getMeshNetworkLiveData().getMeshNetwork()?.getNode(address)

    override fun getGroups(): LiveData<List<Group>> {
        return repository.getGroups()
    }

    override fun getGroupNodes(group: Group): List<ProvisionedMeshNode> {
        val listNodes = mutableListOf<ProvisionedMeshNode>()
        getProvisionedNodes().value?.forEach {
             if (!listNodes.contains(it) && group.ids.contains(it.nodeName.toInt()))
                 listNodes.add(it)
         }
        return listNodes
    }

    override fun disconnect() {
        repository.disconnect()
    }

    override fun autoConnect(onConnect: (() -> Unit)?) {
        repository.autoConnect(context).observeForever {
            if (repository.getBleMeshManager().isDeviceReady()) onConnect?.invoke()
        }
    }

    protected fun getAppKey(index: Int) = repository.getMeshNetworkLiveData().getMeshNetwork()?.getAppKey(index)

    override fun sendMessage(node: ProvisionedMeshNode, message: MeshMessage, isBlocking: Boolean) =
        sendMessage(node.unicastAddress, message, isBlocking)

    override fun sendMessage(group: Group, message: MeshMessage, isBlocking: Boolean) = sendMessage(group.address, message, isBlocking)

    protected fun sendReplicateMessage(group: Group, message: MeshMessage, isBlocking: Boolean) {
        sendMessage(group.address, message, isBlocking)
        Handler().postDelayed({ sendMessage(group.address, message, isBlocking) }, REPLICATE_DELAY)
    }

    protected fun sendBroadcastMessage(message: MeshMessage, isBlocking: Boolean = false) {
        sendMessage(BROADCAST, message, isBlocking)
    }

    private fun sendMessage(unicastAddress: Int, message: MeshMessage, isBlocking: Boolean = false) {
        try {
            if (!checkConnectivity()) autoConnect { sendMessage(unicastAddress, message, isBlocking) }
            else {
                repository.sendMessage(unicastAddress, message, isBlocking)
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun getMessages() = repository.getMeshMessageCallback()

    override fun getEvents() = repository.getTrainingMessageCallback()

    override fun checkConnectivity(): Boolean {
        return repository.isConnectedToProxy().value ?: false
    }

    override fun forceDelete(unicastAddress: Int) {
        val configNodeReset = ConfigNodeReset()
        sendMessage(unicastAddress, configNodeReset)
    }

    protected fun getElement(meshNode: ProvisionedMeshNode): Element? {
        return if (meshNode.elements.values.isNotEmpty()) meshNode.elements.values.elementAt(0) else null
    }

    protected inline fun <reified T : MeshModel> getModel(element: Element): T? {
        return element.meshModels.map { it.value }.filterIsInstance<T>()[0]
    }

    protected suspend fun <X> backgroundAsync(context: CoroutineContext = GlobalScope.coroutineContext, block: suspend () -> X): Deferred<X> {
        return CoroutineScope(context).async(context) { block.invoke() }
    }

    protected fun <T> bulkMessaging(items: List<T>?, delay: Long = BULK_DELAY, action: suspend (T) -> Unit) {
        executeService {
            items?.forEach {
                action(it)
                delay(delay)
            }
        }
    }

    fun executeService(service: suspend () -> Unit) {
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                try {
                    service()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    fun autoOffLedMessage(node: ProvisionedMeshNode, message: MeshMessage, timeout: Long = AUTO_OFF_LEDS) {
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    autoOffLedMessageBroadcast(node.nodeName.toInt(), message, appKey, model.modelId, model.companyIdentifier, timeout)
                }
            }
        }
    }

    fun autoOffLedMessage(group: Group, message: MeshMessage, timeout: Long = AUTO_OFF_LEDS) {
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        val models = network?.getModels(group)
        if (models?.isNotEmpty() == true) {
            val model = models[0] as VendorModel
            val appKey = getAppKey(model.boundAppKeyIndexes[0])
            appKey?.let {
                sendBroadcastMessage(message)
                autoOffLedMessage(group.ids, appKey, model.modelId, model.companyIdentifier, timeout)
            }
        }
    }

    private fun autoOffLedMessage(
        unicastAddress: Int,
        message: MeshMessage,
        appkey: ApplicationKey,
        modelId: Int,
        companyId: Int,
        timeout: Long = AUTO_OFF_LEDS
    ) {
        executeService {
            sendMessage(unicastAddress, message)
            delay(REPLICATE_DELAY)
            sendMessage(unicastAddress, NodeControlMessageUnacked(ControlParams.SET_LED_ON.toByte(), 0, appkey, modelId, companyId))
            TimeoutLiveData<Any?>(timeout, null)
            {
                sendMessage(unicastAddress, NodeControlMessageUnacked(ControlParams.SET_LED_OFF.toByte(), 0, appkey, modelId, companyId))
            }
        }
    }

    fun autoOffLedMessage(
        ids: List<Int>,
        appkey: ApplicationKey,
        modelId: Int,
        companyId: Int,
        timeout: Long = AUTO_OFF_LEDS
    ) {
        executeService {
            delay(REPLICATE_DELAY)
            sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.SET_LED_ON.toByte(),
                    0,
                    appkey,
                    modelId,
                    companyId,
                    OpCodes.getGroupMask(ids)
                )
            )
            TimeoutLiveData<Any?>(timeout, null)
            {
                sendBroadcastMessage(
                    NodeControlMessageUnacked(
                        ControlParams.SET_LED_OFF.toByte(),
                        0,
                        appkey,
                        modelId,
                        companyId,
                        OpCodes.getGroupMask(ids)
                    )
                )
            }
        }
    }

    private fun autoOffLedMessageBroadcast(
        id: Int,
        message: MeshMessage,
        appkey: ApplicationKey,
        modelId: Int,
        companyId: Int,
        timeout: Long = AUTO_OFF_LEDS
    ) {
        executeService {
            sendBroadcastMessage(message)
            delay(REPLICATE_DELAY)

            sendBroadcastMessage(
                NodeControlMessageUnacked(
                    ControlParams.SET_LED_ON.toByte(),
                    0,
                    appkey,
                    modelId,
                    companyId,
                    OpCodes.getUnicastMask(id)
                )
            )

            TimeoutLiveData<Any?>(timeout, null)
            {
                sendBroadcastMessage(
                    NodeControlMessageUnacked(
                        ControlParams.SET_LED_OFF.toByte(),
                        0,
                        appkey,
                        modelId,
                        companyId,
                        OpCodes.getUnicastMask(id)
                    )
                )
            }
        }
    }
}