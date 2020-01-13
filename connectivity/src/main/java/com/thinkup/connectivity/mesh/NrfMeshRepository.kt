package com.thinkup.connectivity.mesh

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thinkup.connectivity.messges.OpCodes
import com.thinkup.connectivity.messges.config.NodeConfigMessageStatus
import com.thinkup.connectivity.messges.control.NodeControlMessageStatus
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessageStatus
import com.thinkup.connectivity.messges.status.NodeGetMessageStatus
import com.thinkup.connectivity.provisioning.ProvisioningStatusLiveData
import com.thinkup.connectivity.utils.*
import no.nordicsemi.android.log.Logger
import no.nordicsemi.android.meshprovisioner.*
import no.nordicsemi.android.meshprovisioner.models.SigModelParser
import no.nordicsemi.android.meshprovisioner.provisionerstates.ProvisioningState.States
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils
import no.nordicsemi.android.support.v18.scanner.*
import java.io.File
import java.util.*

class NrfMeshRepository(
    private val meshManagerApi: MeshManagerApi,
    private val bleMeshManager: BleMeshManager
) :
    MeshProvisioningStatusCallbacks,
    MeshStatusCallbacks,
    MeshManagerCallbacks,
    BleMeshManagerCallbacks {

    private val TAG = NrfMeshRepository::class.java.simpleName
    private val ATTENTION_TIMER = 5

    init { //Initialize the mesh api
        meshManagerApi.setMeshManagerCallbacks(this)
        meshManagerApi.setProvisioningStatusCallbacks(this)
        meshManagerApi.setMeshStatusCallbacks(this)
        meshManagerApi.loadMeshNetwork()
        //Initialize the ble manager
        bleMeshManager.setGattCallbacks(this)
        // TODO remove for real impl
        changeBatteryLevel()
    }

    private fun changeBatteryLevel() {
        Handler().postDelayed({
            batteryAverage.postValue((0..100).shuffled().first())
            changeBatteryLevel()
        }, 5000)
    }

    // Provisioning callback
    var provisionCallback: ProvisionCallback? = null
    // Is sending single/bulking message(s)
    // is true when send a message or provision a node to avoid keep alive bulk messages
    var isSending: Boolean = false
    // Reset Callback
    var nodeCallback: NodeCallback? = null
    // Control vars
    private val batteryAverage: MutableLiveData<Int?> = MutableLiveData()

    val EXPORT_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + "NeuralTrainer" + File.separator + "nRF Mesh" + File.separator
    private val EXPORTED_PATH =
        "sdcard" + File.separator + "NeuralTrainer" + File.separator + "nRF Mesh" + File.separator
    // Connection States Connecting, Connected, Disconnecting, Disconnected etc.
    private val isConnectedToProxy = MutableLiveData<Boolean>()
    // Live data flag containing connected state.
    private var isConnected: MutableLiveData<Boolean> = MutableLiveData()
    // LiveData to notify when device is ready
    private val onDeviceReady = MutableLiveData<Unit>()
    // Flag to determine if a reconnection is in the progress when provisioning has completed
    private val isReconnecting: SingleLiveEvent<Boolean?> = SingleLiveEvent()
    private val unprovisionedMeshNodeLiveData = MutableLiveData<UnprovisionedMeshNode?>()
    private val provisionedMeshNodeLiveData = MutableLiveData<ProvisionedMeshNode?>()
    private val connectedProxyAddress: SingleLiveEvent<Int?> = SingleLiveEvent()
    private var isProvisioningComplete = false // Flag to determine if provisioning was completed
    // Holds the selected MeshNode to configure
    private val extendedMeshNode: MutableLiveData<ProvisionedMeshNode?>? = MutableLiveData()
    // Holds the selected Element to configure
    private val selectedElement = MutableLiveData<Element?>()
    // Holds the selected mesh model to configure
    private val selectedModel = MutableLiveData<MeshModel?>()
    // Holds the selected app key to configure
    private val selectedNetKey = MutableLiveData<NetworkKey>()
    // Holds the selected app key to configure
    private val selectedAppKey = MutableLiveData<ApplicationKey>()
    // Holds the selected provisioner when adding/editing
    private val selectedProvisioner = MutableLiveData<Provisioner>()
    private val selectedGroupLiveData = MutableLiveData<Group>()
    //Contains the MeshNetwork
    private val meshNetworkLiveData: MeshNetworkLiveData = MeshNetworkLiveData()
    private val networkImportState: SingleLiveEvent<String?> = SingleLiveEvent()
    private val meshMessageLiveData: MutableLiveData<MeshMessage?> = SingleLiveEvent()
    private val keepMessageLiveData: MutableLiveData<NodeControlMessageStatus?> = SingleLiveEvent()
    private val eventMessageLiveData: MutableLiveData<NodeEventStatus?> = MutableLiveData()
    private val trainingMessageLiveData: ComparableSingleEvent<NodeEventStatus?> = ComparableSingleEvent()
    // Comparator to delete duplicated events
    private val eventComparator: ComparableEvent<NodeEventStatus> = ComparableEvent()
    // Contains the provisioned nodes
    private val provisionedNodes = MutableLiveData<List<ProvisionedMeshNode>>()
    private val groups = MutableLiveData<List<Group>>()
    private val transactionStatus: MutableLiveData<TransactionStatus?> = SingleLiveEvent()
    private var handler: Handler = Handler()
    private var unprovisionedMeshNode: UnprovisionedMeshNode? = null
    private var provisionedMeshNode: ProvisionedMeshNode? = null
    private var isReconnectingFlag = false
    private var isScanning = false
    private var provisionedNode = false
    private lateinit var provisioningStateLiveData: ProvisioningStatusLiveData
    private var meshNetwork: MeshNetwork? = null
    private var isCompositionDataReceived = false
    private var isDefaultTtlReceived = false
    private var isAppKeyAddCompleted = false
    private var isNetworkRetransmitSetCompleted = false
    private val uri: Uri? = null
    private val reconnectRunnable = Runnable { startScan() }
    private fun autoconnectRunnable(context: Context) = Runnable {
        scanAutoCallback = getScanAutoCallback(getNetworkId(), context)
        startScan(scanAutoCallback, scannerAutoTimeout)
    }

    private val scannerTimeout = Runnable {
        stopScan()
        isReconnecting.postValue(false)
    }
    private val scannerAutoTimeout = Runnable {
        stopAutoScan()
        isReconnecting.postValue(false)
    }

    /**
     * Returns [SingleLiveEvent] containing the device ready state.
     */
    fun isDeviceReady(): LiveData<Unit> {
        return onDeviceReady
    }

    /**
     * Returns [SingleLiveEvent] containing the device ready state.
     */
    fun isConnected(): LiveData<Boolean> {
        return isConnected
    }

    /**
     * Returns [SingleLiveEvent] containing the device ready state.
     */
    fun isConnectedToProxy(): LiveData<Boolean> {
        return isConnectedToProxy
    }

    fun getMeshNetworkLiveData(): MeshNetworkLiveData {
        return meshNetworkLiveData
    }

    fun getNodes(): LiveData<List<ProvisionedMeshNode>> {
        return provisionedNodes
    }

    fun getGroups(): LiveData<List<Group>> {
        return groups
    }

    /**
     * Returns the mesh manager api
     *
     * @return [MeshManagerApi]
     */
    fun getMeshManagerApi(): MeshManagerApi {
        return meshManagerApi
    }

    /**
     * Returns the ble mesh manager
     *
     * @return [BleMeshManager]
     */
    fun getBleMeshManager(): BleMeshManager {
        return bleMeshManager
    }

    /**
     * Returns the [MeshMessageLiveData] live data object containing the mesh message
     */
    fun getMeshMessageLiveData(): LiveData<MeshMessage?> {
        return meshMessageLiveData
    }

    /**
     * Returns the [MeshMessageLiveData] live data object containing the mesh message
     */
    fun getKeepMessageLiveData(): LiveData<NodeControlMessageStatus?> {
        return keepMessageLiveData
    }

    fun flushKeepMessageLiveData() {
        keepMessageLiveData.postValue(null)
    }

    /**
     * Returns the [EventMessageLiveData] live data object containing the mesh message
     */
    fun getEventMessageLiveData(): LiveData<NodeEventStatus?> {
        return eventMessageLiveData
    }

    fun flushEventMessageLiveData() {
        eventMessageLiveData.postValue(null)
    }

    /**
     * Returns the [EventMessageLiveData] live data object containing the mesh message
     */
    fun getTrainingMessageLiveData(): LiveData<NodeEventStatus?> {
        return trainingMessageLiveData
    }

    fun flushTrainingMessageLiveData() {
        trainingMessageLiveData.postValue(null)
    }

    fun getSelectedGroup(): LiveData<Group>? {
        return selectedGroupLiveData
    }

    /**
     * Reset mesh network
     */
    fun resetMeshNetwork() {
        disconnect()
        meshManagerApi.resetMeshNetwork()
    }

    /**
     * Connect to peripheral
     *
     * @param context          Context
     * @param device           [ExtendedBluetoothDevice] device
     * @param connectToNetwork True if connecting to an unprovisioned node or proxy node
     */
    fun connect(
        context: Context?,
        device: ExtendedBluetoothDevice,
        connectToNetwork: Boolean
    ) {
        meshNetworkLiveData.setNodeName(device.name)
        isProvisioningComplete = false
        isCompositionDataReceived = false
        isDefaultTtlReceived = false
        isAppKeyAddCompleted = false
        isNetworkRetransmitSetCompleted = false
        //clearExtendedMeshNode();
        val logSession = Logger.newSession(
            context!!,
            null,
            device.getAddress(),
            device.name
        )
        bleMeshManager.logSession = logSession
        val bluetoothDevice: BluetoothDevice = device.device
        initIsConnectedLiveData(connectToNetwork)
        //Added a 1 second delay for connection, mostly to wait for a disconnection to complete before connecting.
        handler.postDelayed({
            bleMeshManager.connect(bluetoothDevice).retry(3, 200).enqueue()
        }, 1000)
    }

    /**
     * Connect to peripheral
     *
     * @param device bluetooth device
     */
    private fun connectToProxy(device: ExtendedBluetoothDevice) {
        initIsConnectedLiveData(true)
        bleMeshManager.connect(device.device).retry(3, 200).enqueue()
    }

    fun autoConnect(context: Context): LiveData<Unit> {
        handler.postDelayed(
            autoconnectRunnable(context),
            100
        )
        return onDeviceReady
    }

    private fun initIsConnectedLiveData(connectToNetwork: Boolean) {
        if (connectToNetwork) {
            isConnected = SingleLiveEvent()
        } else {
            isConnected = MutableLiveData()
        }
    }

    /**
     * Disconnects from peripheral
     */
    fun disconnect() {
        clearProvisioningLiveData()
        isProvisioningComplete = false
        bleMeshManager.disconnect().enqueue()
    }

    fun clearProvisioningLiveData() {
        stopScan()
        handler.removeCallbacks(reconnectRunnable)
        provisionedNode = false
        isReconnectingFlag = false
        unprovisionedMeshNodeLiveData.value = null
        provisionedMeshNodeLiveData.value = null
    }

    private fun removeCallbacks() {
        handler.removeCallbacksAndMessages(null)
    }

    fun identifyNode(device: ExtendedBluetoothDevice) {
        val beacon = device.beacon as UnprovisionedBeacon?
        if (beacon != null) {
            meshManagerApi.identifyNode(beacon.uuid, ATTENTION_TIMER)
        } else {
            val serviceData: ByteArray? =
                Utils.getServiceData(device.scanResult, Constants.MESH_PROVISIONING_UUID)
            if (serviceData != null) {
                val uuid = meshManagerApi.getDeviceUuid(serviceData)
                meshManagerApi.identifyNode(uuid, ATTENTION_TIMER)
            }
        }
    }

    private fun clearExtendedMeshNode() {
        extendedMeshNode?.postValue(null)
    }

    fun getUnprovisionedMeshNode(): LiveData<UnprovisionedMeshNode?> {
        return unprovisionedMeshNodeLiveData
    }

    fun getProvisionedMeshNode(): LiveData<ProvisionedMeshNode?> {
        return provisionedMeshNodeLiveData
    }

    fun getConnectedProxyAddress(): LiveData<Int?>? {
        return connectedProxyAddress
    }

    /**
     * Returns the selected mesh node
     */
    fun getSelectedMeshNode(): LiveData<ProvisionedMeshNode?>? {
        return extendedMeshNode
    }

    /**
     * Sets the mesh node to be configured
     *
     * @param node provisioned mesh node
     */
    fun setSelectedMeshNode(node: ProvisionedMeshNode?) {
        extendedMeshNode!!.postValue(node)
    }

    /**
     * Returns the selected element
     */
    fun getSelectedElement(): LiveData<Element?>? {
        return selectedElement
    }

    /**
     * Set the selected [Element] to be configured
     *
     * @param element element
     */
    fun setSelectedElement(element: Element?) {
        selectedElement.postValue(element)
    }

    /**
     * Set the selected model to be configured
     *
     * @param appKey mesh model
     */
    fun setSelectedAppKey(appKey: ApplicationKey) {
        selectedAppKey.postValue(appKey)
    }

    /**
     * Returns the selected mesh model
     */
    fun getSelectedAppKey(): LiveData<ApplicationKey>? {
        return selectedAppKey
    }

    /**
     * Selects provisioner for editing or adding
     *
     * @param provisioner [Provisioner]
     */
    fun setSelectedProvisioner(provisioner: Provisioner) {
        selectedProvisioner.postValue(provisioner)
    }

    /**
     * Returns the selected [Provisioner]
     */
    fun getSelectedProvisioner(): LiveData<Provisioner>? {
        return selectedProvisioner
    }

    /**
     * Returns the selected mesh model
     */
    fun getSelectedModel(): LiveData<MeshModel?>? {
        return selectedModel
    }

    /**
     * Set the selected model to be configured
     *
     * @param model mesh model
     */
    fun setSelectedModel(model: MeshModel?) {
        selectedModel.postValue(model)
    }

    fun getBatteryAverage(): LiveData<Int?> = batteryAverage

    override fun onDataReceived(
        bluetoothDevice: BluetoothDevice?,
        mtu: Int,
        pdu: ByteArray
    ) {
        meshManagerApi.handleNotifications(mtu, pdu)
    }

    override fun onDataSent(device: BluetoothDevice?, mtu: Int, pdu: ByteArray) {
        meshManagerApi.handleWriteCallbacks(mtu, pdu)
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        isConnected.postValue(true)
        isConnectedToProxy.postValue(true)
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        Log.v(TAG, "Disconnecting...")
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        Log.v(TAG, "Disconnected")
        if (isReconnectingFlag) {
            isReconnectingFlag = false
            isReconnecting.postValue(false)
            isConnected.postValue(false)
            isConnectedToProxy.postValue(false)
        } else {
            isConnected.postValue(false)
            isConnectedToProxy.postValue(false)
            if (connectedProxyAddress.value != null) {
                val network = meshManagerApi.meshNetwork
                network!!.proxyFilter = null
            }
            //clearExtendedMeshNode();
        }
        provisionedNode = false
        connectedProxyAddress.postValue(null)
    }

    override fun onLinkLossOccurred(device: BluetoothDevice) {
        Log.v(TAG, "Link loss occurred")
        isConnected.postValue(false)
    }

    override fun onServicesDiscovered(
        device: BluetoothDevice,
        optionalServicesFound: Boolean
    ) {
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        onDeviceReady.postValue(null)
        deviceReadyProvision(isSending)
        if (bleMeshManager.isProvisioningComplete()) {
            if (provisionedNode || isSending) {
                if (meshNetwork!!.selectedProvisioner.provisionerAddress != null) {
                    handler.postDelayed({
                        //Adding a slight delay here so we don't send anything before we receive the mesh beacon message
                        val node = provisionedMeshNodeLiveData.value
                        if (node != null) {
                            val compositionDataGet =
                                ConfigCompositionDataGet()
                            sendMessage(
                                node.unicastAddress,
                                compositionDataGet,
                                true
                            )
                        }
                    }, 2000)
                } else {
                    provisionedNode = false
                    provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.PROVISIONER_UNASSIGNED)
                    clearExtendedMeshNode()
                    deviceFailProvision(isSending)
                }
            }
            isConnectedToProxy.postValue(true)
        }
    }

    override fun onBondingRequired(device: BluetoothDevice) { // Empty.
    }

    override fun onBonded(device: BluetoothDevice) { // Empty.
    }

    override fun onBondingFailed(device: BluetoothDevice) { // Empty.
    }

    override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
        Log.e(
            TAG,
            "Error: " + message + " Error Code: " + errorCode + " Device: " + device.address
        )
    }

    override fun onDeviceNotSupported(device: BluetoothDevice) {}

    override fun onNetworkLoaded(meshNetwork: MeshNetwork) {
        loadNetwork(meshNetwork)
        loadGroups()
    }

    override fun onNetworkUpdated(meshNetwork: MeshNetwork) {
        loadNetwork(meshNetwork)
        loadGroups()
        updateSelectedGroup()
    }

    override fun onNetworkLoadFailed(error: String?) {
        networkImportState.postValue(error)
    }

    override fun onNetworkImported(meshNetwork: MeshNetwork) { //We can delete the old network after the import has been successful!
//But let's make sure we don't delete the same network in case someone imports the same network ;)
        val oldNet = this.meshNetwork
        if (oldNet!!.meshUUID != meshNetwork.meshUUID) {
            meshManagerApi.deleteMeshNetworkFromDb(oldNet)
        }
        loadNetwork(meshNetwork)
        loadGroups()
        networkImportState.postValue(
            meshNetwork.meshName + " has been successfully imported.\n" +
                    "In order to start sending messages to this network, please change the provisioner address. " +
                    "Using the same provisioner address will cause messages to be discarded due to the usage of incorrect sequence numbers " +
                    "for this address. However if the network does not contain any nodes you do not need to change the address"
        )
    }

    override fun onNetworkImportFailed(error: String?) {
        networkImportState.postValue(error)
    }

    override fun sendProvisioningPdu(
        meshNode: UnprovisionedMeshNode?,
        pdu: ByteArray?
    ) {
        bleMeshManager.sendPdu(pdu)
    }

    override fun onMeshPduCreated(pdu: ByteArray?) {
        bleMeshManager.sendPdu(pdu)
    }

    override fun getMtu(): Int {
        return bleMeshManager.getPublicMtu()
    }

    override fun onProvisioningStateChanged(
        meshNode: UnprovisionedMeshNode?,
        state: States,
        data: ByteArray?
    ) {
        unprovisionedMeshNode = meshNode
        unprovisionedMeshNodeLiveData.postValue(meshNode)
        when (state) {
            States.PROVISIONING_INVITE -> {
                identifyProvision(meshNode!!)
                provisioningStateLiveData = ProvisioningStatusLiveData()
            }
            States.PROVISIONING_CAPABILITIES -> {
                identifyProvision(meshNode!!)
            }
            States.PROVISIONING_FAILED -> {
                provisionFail()
                isProvisioningComplete = false
            }
        }
        provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.fromStatusCode(state.state))
    }

    override fun onProvisioningFailed(
        meshNode: UnprovisionedMeshNode?,
        state: States,
        data: ByteArray?
    ) {
        unprovisionedMeshNode = meshNode
        unprovisionedMeshNodeLiveData.postValue(meshNode)
        if (state == States.PROVISIONING_FAILED) {
            provisionFail()
            isProvisioningComplete = false
        }
        provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.fromStatusCode(state.state))
    }

    override fun onProvisioningCompleted(
        meshNode: ProvisionedMeshNode?,
        state: States,
        data: ByteArray?
    ) {
        provisionedMeshNode = meshNode
        unprovisionedMeshNodeLiveData.postValue(null)
        provisionedMeshNodeLiveData.postValue(meshNode)
        if (state == States.PROVISIONING_COMPLETE) {
            provisionComplete(meshNode!!)
            onProvisioningCompleted(meshNode)
        }
        provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.fromStatusCode(state.state))
    }

    private fun onProvisioningCompleted(node: ProvisionedMeshNode?) {
        isProvisioningComplete = true
        provisionedMeshNode = node
        isReconnecting.postValue(true)
        bleMeshManager.disconnect().enqueue()
        loadNodes()
        handler.postDelayed(
            reconnectRunnable,
            1000
        ) //Added a slight delay to disconnect and refresh the cache
    }

    /**
     * Here we load all nodes except the current provisioner. This may contain other provisioner nodes if available
     */
    fun loadNodes() {
        val nodes: MutableList<ProvisionedMeshNode> = ArrayList()
        for (node in meshNetwork!!.nodes) {
            if (!node.uuid.equals(meshNetwork!!.selectedProvisioner.provisionerUuid, ignoreCase = true)) {
                nodes.add(node)
            }
        }
        provisionedNodes.postValue(nodes)
    }

    fun updateNodes(connecteds: List<ProvisionedMeshNode>) {
        var hasReload = false
        for (node in meshNetwork?.nodes ?: listOf()) {
            hasReload = true
            connecteds.find { it.nodeName == node.nodeName }?.let {
                node.isOnline = true
                node.batteryLevel = it.batteryLevel
            } ?: run {
                node.isOnline = false
            }
        }
        if (hasReload) loadNodes()
    }

    override fun onTransactionFailed(dst: Int, hasIncompleteTimerExpired: Boolean) {
        provisionedMeshNode = meshNetwork!!.getNode(dst)
        transactionStatus.postValue(TransactionStatus(dst, hasIncompleteTimerExpired))
    }

    override fun onUnknownPduReceived(src: Int, accessPayload: ByteArray?) {
        val node = meshNetwork!!.getNode(src)
        if (node != null) {
            provisionedMeshNode = node
            updateNode(node)
        }
    }

    override fun onBlockAcknowledgementProcessed(dst: Int, message: ControlMessage) {
        val node = meshNetwork!!.getNode(dst)
        if (node != null) {
            provisionedMeshNode = node
            if (provisionedNode) {
                provisionedMeshNodeLiveData.postValue(provisionedMeshNode)
                provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.SENDING_BLOCK_ACKNOWLEDGEMENT)
            }
        }
    }

    override fun onBlockAcknowledgementReceived(src: Int, message: ControlMessage) {
        val node = meshNetwork!!.getNode(src)
        if (node != null) {
            provisionedMeshNode = node
            if (provisionedNode) {
                provisionedMeshNodeLiveData.postValue(node)
                provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.BLOCK_ACKNOWLEDGEMENT_RECEIVED)
            }
        }
    }

    override fun onMeshMessageProcessed(dst: Int, meshMessage: MeshMessage) {
        val node = meshNetwork!!.getNode(dst)
        if (node != null) {
            provisionedMeshNode = node
            if (meshMessage is ConfigCompositionDataGet) {
                if (provisionedNode) {
                    provisionedMeshNodeLiveData.postValue(node)
                    provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.COMPOSITION_DATA_GET_SENT)
                }
            } else if (meshMessage is ConfigDefaultTtlGet) {
                if (provisionedNode) {
                    provisionedMeshNodeLiveData.postValue(node)
                    provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.SENDING_DEFAULT_TTL_GET)
                }
            } else if (meshMessage is ConfigAppKeyAdd) {
                if (provisionedNode) {
                    provisionedMeshNodeLiveData.postValue(node)
                    provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.SENDING_APP_KEY_ADD)
                }
            } else if (meshMessage is ConfigNetworkTransmitSet) {
                if (provisionedNode) {
                    provisionedMeshNodeLiveData.postValue(node)
                    provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.SENDING_NETWORK_TRANSMIT_SET)
                }
            }
        }
    }

    override fun onMeshMessageReceived(src: Int, meshMessage: MeshMessage) {
        val node = meshNetwork!!.getNode(src)
        if (node != null) if (meshMessage is ProxyConfigFilterStatus) {
            provisionedMeshNode = node
            setSelectedMeshNode(node)
            val status = meshMessage
            val unicastAddress = status.src
            Log.v(
                TAG,
                "Proxy configuration source: " + MeshAddress.formatAddress(status.src, false)
            )
            connectedProxyAddress.postValue(unicastAddress)
            meshMessageLiveData.postValue(status)
        } else if (meshMessage is ConfigCompositionDataStatus) {
            val status = meshMessage
            isCompositionDataReceived = true
            provisionedMeshNodeLiveData.postValue(node)
            connectedProxyAddress.postValue(node.unicastAddress)
            provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.COMPOSITION_DATA_STATUS_RECEIVED)
            handler.postDelayed({
                val configDefaultTtlGet = ConfigDefaultTtlGet()
                sendMessage(node.unicastAddress, configDefaultTtlGet, true)
            }, 500)
        } else if (meshMessage is ConfigDefaultTtlStatus) {
            isDefaultTtlReceived = true
            provisionedMeshNodeLiveData.postValue(node)
            provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.DEFAULT_TTL_STATUS_RECEIVED)
            handler.postDelayed({
                val appKey: ApplicationKey? = meshNetworkLiveData.getSelectedAppKey()
                val index = node.addedNetKeys[0].index
                val networkKey = meshNetwork!!.netKeys[index]
                val configAppKeyAdd = ConfigAppKeyAdd(networkKey, appKey!!)
                sendMessage(node.unicastAddress, configAppKeyAdd, true)
            }, 1500)
        } else if (meshMessage is ConfigAppKeyStatus) {
            val status = meshMessage
            if (status.isSuccessful) {
                isAppKeyAddCompleted = true
                provisionedMeshNodeLiveData.postValue(node)
                provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.APP_KEY_STATUS_RECEIVED)
                handler.postDelayed({
                    val networkTransmitSet =
                        ConfigNetworkTransmitSet(2, 1)
                    sendMessage(node.unicastAddress, networkTransmitSet, true)
                }, 1500)
            }
        } else if (meshMessage is ConfigNetworkTransmitStatus) {
            provisionedNode = false
            isNetworkRetransmitSetCompleted = true
            provisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.NETWORK_TRANSMIT_STATUS_RECEIVED)
            if (isSending) bindNodeKeyComplete(node)
        } else if (meshMessage is ConfigModelAppStatus) {
            if (updateNode(node)) {
                val status = meshMessage
                val element =
                    node.elements[status.elementAddress]
                if (node.elements.containsKey(status.elementAddress)) {
                    selectedElement.postValue(element)
                    val model = element!!.meshModels[status.modelIdentifier]
                    selectedModel.postValue(model)

                }
                if (isSending) bindAppKeyComplete(node)
            }
        } else if (meshMessage is ConfigModelPublicationStatus) {
            if (updateNode(node)) {
                val status =
                    meshMessage
                if (node.elements.containsKey(status.elementAddress)) {
                    val element =
                        node.elements[status.elementAddress]
                    selectedElement.postValue(element)
                    val model = element!!.meshModels[status.modelIdentifier]
                    selectedModel.postValue(model)
                    if (isSending) setPublicationComplete(node)
                }
            }
        } else if (meshMessage is ConfigModelSubscriptionStatus) {
            if (updateNode(node)) {
                val status =
                    meshMessage
                if (node.elements.containsKey(status.elementAddress)) {
                    val element =
                        node.elements[status.elementAddress]
                    selectedElement.postValue(element)
                    val model = element!!.meshModels[status.modelIdentifier]
                    selectedModel.postValue(model)
                }
            }
        } else if (meshMessage is ConfigNodeResetStatus) {
            bleMeshManager.setClearCacheRequired()
            extendedMeshNode!!.postValue(null)
            loadNodes()
            meshMessageLiveData.postValue(meshMessage)
            resetNode()
        } else if (meshMessage is ConfigRelayStatus) {
            if (updateNode(node)) {
                meshMessageLiveData.postValue(meshMessage)
            }
        } else if (meshMessage is ConfigProxyStatus) {
            if (updateNode(node)) {
                meshMessageLiveData.postValue(meshMessage)
            }
        } else if (meshMessage is GenericOnOffStatus) {
            if (updateNode(node)) {
                val status = meshMessage
                if (node.elements.containsKey(status.srcAddress)) {
                    val element =
                        node.elements[status.srcAddress]
                    selectedElement.postValue(element)
                    val model =
                        element!!.meshModels[SigModelParser.GENERIC_ON_OFF_SERVER.toInt()]
                    selectedModel.postValue(model)
                }
            }
        } else if (meshMessage is GenericLevelStatus) {
            if (updateNode(node)) {
                val status = meshMessage
                if (node.elements.containsKey(status.srcAddress)) {
                    val element =
                        node.elements[status.srcAddress]
                    selectedElement.postValue(element)
                    val model =
                        element!!.meshModels[SigModelParser.GENERIC_LEVEL_SERVER.toInt()]
                    selectedModel.postValue(model)
                }
            }
        }
        if (meshMessageLiveData.hasActiveObservers()) {
            meshMessageLiveData.postValue(meshMessage)
        }
        //Refresh mesh network live data
        meshNetworkLiveData.refresh(meshManagerApi.meshNetwork!!)
    }


    /**
     * Received all vendor status and check types
     **/
    override fun onMeshMessageReceived(src: Int, original: MeshMessage?, accessMessage: AccessMessage) {
        val node = meshNetwork!!.getNode(src)
        if (node != null) {
            var status: GenericStatusMessage? = null
            val opCode = MeshParserUtils.unsignedByteToInt(accessMessage.accessPdu[0])
            if (opCode == OpCodes.NT_OPCODE_EVENT) {
                status = NodeEventStatus(accessMessage)
                trainingMessageLiveData.postValue(status)
                if (!eventComparator.compare(status)) {
                    eventMessageLiveData.postValue(status)
                }
            } else {
                var status: VendorModelMessageStatus? = null
                val modelIdentifier = if (original is VendorModelMessageAcked) original.modelIdentifier else 0
                when (opCode) {
                    OpCodes.NT_OPCODE_GENERAL_STATUS -> status = NodeGetMessageStatus(accessMessage, modelIdentifier)
                    OpCodes.NT_OPCODE_CONFIG_STATUS -> status = NodeConfigMessageStatus(accessMessage, modelIdentifier)
                    OpCodes.NT_OPCODE_SYSTEM_STATUS -> status = NodeControlMessageStatus(accessMessage, modelIdentifier)
                    OpCodes.NT_OPCODE_PERIFERAL_STATUS -> status = NodePeripheralMessageStatus(accessMessage, modelIdentifier)
                }
                update(node, status)
                if (meshMessageLiveData.hasActiveObservers()) {
                    meshMessageLiveData.postValue(status)
                }
                if (keepMessageLiveData.hasActiveObservers() && status is NodeControlMessageStatus) {
                    keepMessageLiveData.postValue(status)
                }
            }
        }
        // Refresh mesh network live data
        meshNetworkLiveData.refresh(meshManagerApi.meshNetwork!!)
    }

    private fun update(node: ProvisionedMeshNode, status: VendorModelMessageStatus?) {
        if (updateNode(node)) {
            if (status != null && node.elements.containsKey(status.srcAddress)) {
                val element = node.elements[status.srcAddress]
                selectedElement.postValue(element)
                val model = element!!.meshModels[status.modelIdentifier]
                selectedModel.postValue(model)
            }
        }
    }

    private fun defaultVendorMessage(src: Int, original: MeshMessage, accessMessage: AccessMessage): VendorModelMessageStatus? {
        if (original is VendorModelMessageAcked) {
            val vendorModelMessageAcked = original
            return VendorModelMessageStatus(accessMessage, vendorModelMessageAcked.modelIdentifier)
        } else if (original is VendorModelMessageUnacked) {
            val vendorModelMessageUnacked = original
            return VendorModelMessageStatus(accessMessage, vendorModelMessageUnacked.modelIdentifier)
        }
        return null
    }

    override fun onMessageDecryptionFailed(
        meshLayer: String,
        errorMessage: String
    ) {
        Log.e(TAG, "Decryption failed in $meshLayer : $errorMessage")
    }

    /**
     * Loads the network that was loaded from the db or imported from the mesh cdb
     *
     * @param meshNetwork mesh network that was loaded
     */
    private fun loadNetwork(meshNetwork: MeshNetwork) {
        this.meshNetwork = meshNetwork
        if (this.meshNetwork != null) {
            if (!this.meshNetwork!!.isProvisionerSelected) {
                val provisioner = meshNetwork.provisioners[0]
                provisioner.isLastSelected = true
                this.meshNetwork!!.selectProvisioner(provisioner)
            }
            //Load live data with mesh network
            meshNetworkLiveData.loadNetworkInformation(meshNetwork)
            //Load live data with provisioned nodes
            loadNodes()
            val node = getSelectedMeshNode()!!.value
            if (node != null) {
                extendedMeshNode!!.postValue(this.meshNetwork!!.getNode(node.uuid))
            }
        }
    }

    /**
     * We should only update the selected node, since sending messages to group address will notify with nodes that is not on the UI
     */
    private fun updateNode(node: ProvisionedMeshNode): Boolean {
        if (provisionedMeshNode?.unicastAddress == node.unicastAddress) {
            provisionedMeshNode = node
            extendedMeshNode?.postValue(node)
            return true
        }
        return false
    }

    /**
     * Starts reconnecting to the device
     */
    private fun startScan(scanCallback: ScanCallback = getDefaultScanCallback(), timeout: Runnable = scannerTimeout) {
        try {
            this.scanCallback = scanCallback
            if (isScanning) return
            isScanning = true
            // Scanning settings
            val settings =
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Refresh the devices list every second
                    .setReportDelay(0) // Hardware filtering has some issues on selected devices
                    .setUseHardwareFilteringIfSupported(false) // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
/*.setUseHardwareBatchingIfSupported(false)*/
                    .build()
            // Let's use the filter to scan only for Mesh devices
            val filters: MutableList<ScanFilter> =
                ArrayList()
            filters.add(
                ScanFilter.Builder().setServiceUuid(
                    ParcelUuid(Constants.MESH_PROXY_UUID)
                ).build()
            )
            val scanner = BluetoothLeScannerCompat.getScanner()
            scanner.startScan(filters, settings, scanCallback)
            Log.v(TAG, "Scan started")
            handler.postDelayed(timeout, 20000)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private fun stopScan() {
        handler.removeCallbacks(scannerTimeout)
        val scanner = BluetoothLeScannerCompat.getScanner()
        scanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun stopAutoScan() {
        handler.removeCallbacks(scannerAutoTimeout)
        val scanner = BluetoothLeScannerCompat.getScanner()
        scanner.stopScan(scanAutoCallback)
        isScanning = false
    }

    private var scanCallback: ScanCallback = getDefaultScanCallback()
    private var scanAutoCallback: ScanCallback = getDefaultScanCallback()

    private fun getDefaultScanCallback() =
        object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) { // In order to connectToProxy to the correct device, the hash advertised in the advertisement data should be matched.
                // This is to make sure we connectToProxy to the same device as device addresses could change after provisioning.
                val scanRecord =
                    result.scanRecord
                if (scanRecord != null) {
                    val serviceData: ByteArray? =
                        Utils.getServiceData(result, Constants.MESH_PROXY_UUID)
                    if (serviceData != null) {
                        if (meshManagerApi.isAdvertisedWithNodeIdentity(serviceData)) {
                            val node = provisionedMeshNode
                            if (meshManagerApi.nodeIdentityMatches(node!!, serviceData)) {
                                stopScan()
                                onProvisionedDeviceFound(node, ExtendedBluetoothDevice(result))
                            }
                        }
                    }
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>) { // Batch scan is disabled (report delay = 0)
            }

            override fun onScanFailed(errorCode: Int) {}
        }

    private fun getScanAutoCallback(networkId: String, context: Context): ScanCallback =
        object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) { // In order to connectToProxy to the correct device, the hash advertised in the advertisement data should be matched.
                // This is to make sure we connectToProxy to the same device as device addresses could change after provisioning.
                val scanRecord =
                    result.scanRecord
                if (scanRecord != null) {
                    val serviceData = Utils.getServiceData(result, Constants.MESH_PROXY_UUID)
                    if (meshManagerApi.isAdvertisingWithNetworkIdentity(serviceData)) {
                        if (meshManagerApi.networkIdMatches(networkId, serviceData)) {
                            stopAutoScan()
                            connect(context, ExtendedBluetoothDevice(result), true)
                        }
                    } else if (meshManagerApi.isAdvertisedWithNodeIdentity(serviceData)) {
                        if (checkIfNodeIdentityMatches(serviceData!!)) {
                            stopAutoScan()
                            connect(context, ExtendedBluetoothDevice(result), true)
                        }
                    }
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>) { // Batch scan is disabled (report delay = 0)
            }

            override fun onScanFailed(errorCode: Int) {}
        }

    private fun getNetworkId(): String {
        val network = meshManagerApi.meshNetwork
        if (network != null) {
            if (!network.netKeys.isEmpty()) {
                return meshManagerApi.generateNetworkId(network.netKeys[0].key)
            }
        }
        return ""
    }

    private fun checkIfNodeIdentityMatches(serviceData: ByteArray): Boolean {
        val network = meshManagerApi.meshNetwork
        if (network != null) {
            for (node in network.nodes) {
                if (meshManagerApi.nodeIdentityMatches(node, serviceData)) {
                    return true
                }
            }
        }
        return false
    }

    private fun onProvisionedDeviceFound(
        node: ProvisionedMeshNode?,
        device: ExtendedBluetoothDevice
    ) {
        provisionedNode = true
        provisionedMeshNode = node
        isReconnectingFlag = true
        //Added an extra delay to ensure reconnection
        handler.postDelayed({ connectToProxy(device) }, 2000)
    }

    /**
     * Generates the groups based on the addresses each models have subscribed to
     */
    private fun loadGroups() {
        groups.postValue(meshNetwork!!.groups)
    }

    private fun updateSelectedGroup() {
        val selectedGroup =
            selectedGroupLiveData.value
        if (selectedGroup != null) {
            selectedGroupLiveData.postValue(meshNetwork!!.getGroup(selectedGroup.address))
        }
    }

    /**
     * Sets the group that was selected from the GroupAdapter.
     */
    fun setSelectedGroup(address: Int) {
        val group = meshNetwork!!.getGroup(address)
        if (group != null) {
            selectedGroupLiveData.postValue(group)
        }
    }

    fun sendMessage(unicastAddress: Int, message: MeshMessage, isProvisioning: Boolean = false) {
        if (!isProvisioning) {
            isSending = true
            Handler().postDelayed({ isSending = false }, 100)
        }
        meshManagerApi.createMeshPdu(unicastAddress, message)
    }

}