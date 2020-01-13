package com.thinkup.connectivity.common

import android.content.Context
import android.os.Handler
import androidx.lifecycle.LiveData
import com.thinkup.connectivity.BleConnection
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.mesh.NrfMeshRepository
import kotlinx.coroutines.*
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.*
import kotlin.coroutines.CoroutineContext

open class BaseBleImpl(protected val context: Context, protected val setting: BleSetting, protected val repository: NrfMeshRepository) :
    BleConnection {

    protected val ACTION_TIMEOUT = 3 * 1000L // 3 sec
    protected val STEP_TIMEOUT = 2 * 1000L // 2 sec
    protected val PROVISION_TIMEOUT = 60 * 1000L // 30 sec - Timeout to provision step
    protected val KEEP_ALIVE = 30 * 1000L // 40  sec - Time to send keep alive message
    protected val KEEP_ALIVE_RETRY = 5 * 1000L // 5  sec - Time to send keep alive message when is sending another command
    protected val KEEP_ALIVE_WAIT = 3 * 1000L // 3  sec - Time to recollect keep alive responses
    protected val BULK_DELAY = 100L
    protected val REPLICATE_DELAY = 250L

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
        val network = repository.getMeshNetworkLiveData().getMeshNetwork()
        return network?.getNodes(group) ?: listOf()
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

    override fun sendMessage(node: ProvisionedMeshNode, message: MeshMessage, isProvisioning: Boolean) =
        sendMessage(node.unicastAddress, message, isProvisioning)

    override fun sendMessage(group: Group, message: MeshMessage, isProvisioning: Boolean) = sendMessage(group.address, message, isProvisioning)

    protected fun sendReplicateMessage(group: Group, message: MeshMessage, isProvisioning: Boolean) {
        sendMessage(group.address, message, isProvisioning)
        Handler().postDelayed({ sendMessage(group.address, message, isProvisioning) }, REPLICATE_DELAY)
    }

    private fun sendMessage(unicastAddress: Int, message: MeshMessage, isProvisioning: Boolean = false) {
        try {
            if (!checkConnectivity()) autoConnect { sendMessage(unicastAddress, message, isProvisioning) }
            else {
                repository.sendMessage(unicastAddress, message, isProvisioning)
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun getMessages() = repository.getMeshMessageLiveData()

    override fun getEvents() = repository.getEventMessageLiveData()

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
}