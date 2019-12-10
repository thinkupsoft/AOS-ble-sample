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
import com.thinkup.connectivity.common.Status
import com.thinkup.connectivity.messges.config.NodeConfigMessage
import com.thinkup.connectivity.messges.config.NodeConfigMessageStatus
import com.thinkup.connectivity.messges.control.NodeControlMessage
import com.thinkup.connectivity.messges.control.NodeControlMessageStatus
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessage
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessageStatus
import com.thinkup.connectivity.messges.status.NodeGetMessage
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
import no.nordicsemi.android.support.v18.scanner.*
import java.io.File
import java.util.*

class NrfMeshRepository(
    private val mMeshManagerApi: MeshManagerApi,
    private val mBleMeshManager: BleMeshManager
) :
    MeshProvisioningStatusCallbacks,
    MeshStatusCallbacks,
    MeshManagerCallbacks,
    BleMeshManagerCallbacks {

    init { //Initialize the mesh api
        mMeshManagerApi.setMeshManagerCallbacks(this)
        mMeshManagerApi.setProvisioningStatusCallbacks(this)
        mMeshManagerApi.setMeshStatusCallbacks(this)
        mMeshManagerApi.loadMeshNetwork()
        //Initialize the ble manager
        mBleMeshManager.setGattCallbacks(this)
    }

    private val TAG = NrfMeshRepository::class.java.simpleName
    private val ATTENTION_TIMER = 5
    val EXPORT_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + "Nordic Semiconductor" + File.separator + "nRF Mesh" + File.separator
    private val EXPORTED_PATH =
        "sdcard" + File.separator + "Nordic Semiconductor" + File.separator + "nRF Mesh" + File.separator
    // Connection States Connecting, Connected, Disconnecting, Disconnected etc.
    private val mIsConnectedToProxy = MutableLiveData<Boolean>()
    // Live data flag containing connected state.
    private var mIsConnected: MutableLiveData<Boolean> = MutableLiveData()
    // LiveData to notify when device is ready
    private val mOnDeviceReady = MutableLiveData<Unit>()
    // Updates the connection state while connecting to a peripheral
    private val mConnectionState = MutableLiveData<Status>()
    // Flag to determine if a reconnection is in the progress when provisioning has completed
    private val mIsReconnecting: SingleLiveEvent<Boolean?> = SingleLiveEvent()
    private val mUnprovisionedMeshNodeLiveData = MutableLiveData<UnprovisionedMeshNode?>()
    private val mProvisionedMeshNodeLiveData = MutableLiveData<ProvisionedMeshNode?>()
    private val mConnectedProxyAddress: SingleLiveEvent<Int?> = SingleLiveEvent()
    private var mIsProvisioningComplete = false // Flag to determine if provisioning was completed
    // Holds the selected MeshNode to configure
    private val mExtendedMeshNode: MutableLiveData<ProvisionedMeshNode?>? = MutableLiveData()
    // Holds the selected Element to configure
    private val mSelectedElement = MutableLiveData<Element?>()
    // Holds the selected mesh model to configure
    private val mSelectedModel = MutableLiveData<MeshModel?>()
    // Holds the selected app key to configure
    private val mSelectedNetKey = MutableLiveData<NetworkKey>()
    // Holds the selected app key to configure
    private val mSelectedAppKey = MutableLiveData<ApplicationKey>()
    // Holds the selected provisioner when adding/editing
    private val mSelectedProvisioner = MutableLiveData<Provisioner>()
    private val mSelectedGroupLiveData = MutableLiveData<Group>()
    // Composition data status
    val mCompositionDataStatus: SingleLiveEvent<ConfigCompositionDataStatus> = SingleLiveEvent()
    // App key add status
    val mAppKeyStatus: SingleLiveEvent<ConfigAppKeyStatus> = SingleLiveEvent()
    //Contains the MeshNetwork
    private val mMeshNetworkLiveData: MeshNetworkLiveData = MeshNetworkLiveData()
    private val mNetworkImportState: SingleLiveEvent<String?> = SingleLiveEvent()
    private val mMeshMessageLiveData: SingleLiveEvent<MeshMessage?> = SingleLiveEvent()
    // Contains the provisioned nodes
    private val mProvisionedNodes = MutableLiveData<List<ProvisionedMeshNode>>()
    private val mGroups = MutableLiveData<List<Group>>()
    private val mTransactionStatus: MutableLiveData<TransactionStatus?> = SingleLiveEvent()
    private var mHandler: Handler = Handler()
    private var mUnprovisionedMeshNode: UnprovisionedMeshNode? = null
    private var mProvisionedMeshNode: ProvisionedMeshNode? = null
    private var mIsReconnectingFlag = false
    private var mIsScanning = false
    private var mSetupProvisionedNode = false
    private lateinit var mProvisioningStateLiveData: ProvisioningStatusLiveData
    private var mMeshNetwork: MeshNetwork? = null
    private var mIsCompositionDataReceived = false
    private var mIsDefaultTtlReceived = false
    private var mIsAppKeyAddCompleted = false
    private var mIsNetworkRetransmitSetCompleted = false
    private val uri: Uri? = null
    private val mReconnectRunnable = Runnable { startScan() }
    private fun autoconnectRunnable(context: Context) = Runnable { startScan(getScanAutoCallback(getNetworkId(), context)) }
    private val mScannerTimeout = Runnable {
        stopScan()
        mIsReconnecting.postValue(false)
    }

    /**
     * Returns [SingleLiveEvent] containing the device ready state.
     */
    fun isDeviceReady(): LiveData<Unit> {
        return mOnDeviceReady
    }

    /**
     * Returns [SingleLiveEvent] containing the device ready state.
     */
    fun getConnectionState(): MutableLiveData<Status> {
        return mConnectionState
    }

    /**
     * Returns [SingleLiveEvent] containing the device ready state.
     */
    fun isConnected(): LiveData<Boolean> {
        return mIsConnected
    }

    /**
     * Returns [SingleLiveEvent] containing the device ready state.
     */
    fun isConnectedToProxy(): LiveData<Boolean>? {
        return mIsConnectedToProxy
    }

    fun isReconnecting(): LiveData<Boolean?>? {
        return mIsReconnecting
    }

    fun isProvisioningComplete(): Boolean {
        return mIsProvisioningComplete
    }

    fun isCompositionDataStatusReceived(): Boolean {
        return mIsCompositionDataReceived
    }

    fun isDefaultTtlReceived(): Boolean {
        return mIsDefaultTtlReceived
    }

    fun isAppKeyAddCompleted(): Boolean {
        return mIsAppKeyAddCompleted
    }

    fun isNetworkRetransmitSetCompleted(): Boolean {
        return mIsNetworkRetransmitSetCompleted
    }

    fun getMeshNetworkLiveData(): MeshNetworkLiveData {
        return mMeshNetworkLiveData
    }

    fun getNodes(): LiveData<List<ProvisionedMeshNode>> {
        return mProvisionedNodes
    }

    fun getGroups(): LiveData<List<Group>> {
        return mGroups
    }

    fun getNetworkLoadState(): LiveData<String?>? {
        return mNetworkImportState
    }

    fun getProvisioningState(): ProvisioningStatusLiveData {
        return mProvisioningStateLiveData
    }

    fun getTransactionStatus(): LiveData<TransactionStatus?>? {
        return mTransactionStatus
    }

    /**
     * Clears the transaction status
     */
    fun clearTransactionStatus() {
        if (mTransactionStatus.getValue() != null) {
            mTransactionStatus.postValue(null)
        }
    }

    /**
     * Returns the mesh manager api
     *
     * @return [MeshManagerApi]
     */
    fun getMeshManagerApi(): MeshManagerApi {
        return mMeshManagerApi
    }

    /**
     * Returns the ble mesh manager
     *
     * @return [BleMeshManager]
     */
    fun getBleMeshManager(): BleMeshManager {
        return mBleMeshManager
    }

    /**
     * Returns the [MeshMessageLiveData] live data object containing the mesh message
     */
    fun getMeshMessageLiveData(): LiveData<MeshMessage?> {
        return mMeshMessageLiveData
    }

    fun getSelectedGroup(): LiveData<Group>? {
        return mSelectedGroupLiveData
    }

    /**
     * Reset mesh network
     */
    fun resetMeshNetwork() {
        disconnect()
        mMeshManagerApi!!.resetMeshNetwork()
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
        mMeshNetworkLiveData.setNodeName(device.name)
        mIsProvisioningComplete = false
        mIsCompositionDataReceived = false
        mIsDefaultTtlReceived = false
        mIsAppKeyAddCompleted = false
        mIsNetworkRetransmitSetCompleted = false
        //clearExtendedMeshNode();
        val logSession = Logger.newSession(
            context!!,
            null,
            device.getAddress(),
            device.name
        )
        mBleMeshManager.logSession = logSession
        val bluetoothDevice: BluetoothDevice = device.device
        initIsConnectedLiveData(connectToNetwork)
        mConnectionState.postValue(Status.CONNECTING)
        //Added a 1 second delay for connection, mostly to wait for a disconnection to complete before connecting.
        mHandler!!.postDelayed({
            mBleMeshManager.connect(bluetoothDevice).retry(3, 200).enqueue()
        }, 1000)
    }

    /**
     * Connect to peripheral
     *
     * @param device bluetooth device
     */
    private fun connectToProxy(device: ExtendedBluetoothDevice) {
        initIsConnectedLiveData(true)
        mConnectionState.postValue(Status.CONNECTING)
        mBleMeshManager.connect(device.device).retry(3, 200).enqueue()
    }

    fun autoConnect(context: Context): LiveData<Unit> {
        mHandler.postDelayed(
            autoconnectRunnable(context),
            1000
        )
        return mOnDeviceReady
    }

    private fun initIsConnectedLiveData(connectToNetwork: Boolean) {
        if (connectToNetwork) {
            mIsConnected = SingleLiveEvent()
        } else {
            mIsConnected = MutableLiveData()
        }
    }

    /**
     * Disconnects from peripheral
     */
    fun disconnect() {
        clearProvisioningLiveData()
        mIsProvisioningComplete = false
        mBleMeshManager.disconnect().enqueue()
    }

    fun clearProvisioningLiveData() {
        stopScan()
        mHandler!!.removeCallbacks(mReconnectRunnable)
        mSetupProvisionedNode = false
        mIsReconnectingFlag = false
        mUnprovisionedMeshNodeLiveData.value = null
        mProvisionedMeshNodeLiveData.value = null
    }

    private fun removeCallbacks() {
        mHandler!!.removeCallbacksAndMessages(null)
    }

    fun identifyNode(device: ExtendedBluetoothDevice) {
        val beacon = device.beacon as UnprovisionedBeacon?
        if (beacon != null) {
            mMeshManagerApi!!.identifyNode(beacon.uuid, ATTENTION_TIMER)
        } else {
            val serviceData: ByteArray? =
                Utils.getServiceData(device.scanResult, Constants.MESH_PROVISIONING_UUID)
            if (serviceData != null) {
                val uuid = mMeshManagerApi!!.getDeviceUuid(serviceData)
                mMeshManagerApi!!.identifyNode(uuid, ATTENTION_TIMER)
            }
        }
    }

    private fun clearExtendedMeshNode() {
        mExtendedMeshNode?.postValue(null)
    }

    fun getUnprovisionedMeshNode(): LiveData<UnprovisionedMeshNode?> {
        return mUnprovisionedMeshNodeLiveData
    }

    fun getProvisionedMeshNode(): LiveData<ProvisionedMeshNode?> {
        return mProvisionedMeshNodeLiveData
    }

    fun getConnectedProxyAddress(): LiveData<Int?>? {
        return mConnectedProxyAddress
    }

    /**
     * Returns the selected mesh node
     */
    fun getSelectedMeshNode(): LiveData<ProvisionedMeshNode?>? {
        return mExtendedMeshNode
    }

    /**
     * Sets the mesh node to be configured
     *
     * @param node provisioned mesh node
     */
    fun setSelectedMeshNode(node: ProvisionedMeshNode?) {
        mExtendedMeshNode!!.postValue(node)
    }

    /**
     * Returns the selected element
     */
    fun getSelectedElement(): LiveData<Element?>? {
        return mSelectedElement
    }

    /**
     * Set the selected [Element] to be configured
     *
     * @param element element
     */
    fun setSelectedElement(element: Element?) {
        mSelectedElement.postValue(element)
    }

    /**
     * Set the selected model to be configured
     *
     * @param appKey mesh model
     */
    fun setSelectedAppKey(appKey: ApplicationKey) {
        mSelectedAppKey.postValue(appKey)
    }

    /**
     * Returns the selected mesh model
     */
    fun getSelectedAppKey(): LiveData<ApplicationKey>? {
        return mSelectedAppKey
    }

    /**
     * Selects provisioner for editing or adding
     *
     * @param provisioner [Provisioner]
     */
    fun setSelectedProvisioner(provisioner: Provisioner) {
        mSelectedProvisioner.postValue(provisioner)
    }

    /**
     * Returns the selected [Provisioner]
     */
    fun getSelectedProvisioner(): LiveData<Provisioner>? {
        return mSelectedProvisioner
    }

    /**
     * Returns the selected mesh model
     */
    fun getSelectedModel(): LiveData<MeshModel?>? {
        return mSelectedModel
    }

    /**
     * Set the selected model to be configured
     *
     * @param model mesh model
     */
    fun setSelectedModel(model: MeshModel?) {
        mSelectedModel.postValue(model)
    }

    override fun onDataReceived(
        bluetoothDevice: BluetoothDevice?,
        mtu: Int,
        pdu: ByteArray
    ) {
        mMeshManagerApi.handleNotifications(mtu, pdu)
    }

    override fun onDataSent(device: BluetoothDevice?, mtu: Int, pdu: ByteArray) {
        mMeshManagerApi.handleWriteCallbacks(mtu, pdu)
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
        mConnectionState.postValue(Status.CONNECTING)
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        mIsConnected!!.postValue(true)
        mConnectionState.postValue(Status.DISCOVERING)
        mIsConnectedToProxy.postValue(true)
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        Log.v(TAG, "Disconnecting...")
        if (mIsReconnectingFlag) {
            mConnectionState.postValue(Status.RECONNECTING)
        } else {
            mConnectionState.postValue(Status.DISCONNECTING)
        }
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        Log.v(TAG, "Disconnected")
        mConnectionState.postValue(Status.IDLE)
        if (mIsReconnectingFlag) {
            mIsReconnectingFlag = false
            mIsReconnecting.postValue(false)
            mIsConnected!!.postValue(false)
            mIsConnectedToProxy.postValue(false)
        } else {
            mIsConnected!!.postValue(false)
            mIsConnectedToProxy.postValue(false)
            if (mConnectedProxyAddress.getValue() != null) {
                val network = mMeshManagerApi!!.meshNetwork
                network!!.proxyFilter = null
            }
            //clearExtendedMeshNode();
        }
        mSetupProvisionedNode = false
        mConnectedProxyAddress.postValue(null)
    }

    override fun onLinkLossOccurred(device: BluetoothDevice) {
        Log.v(TAG, "Link loss occurred")
        mIsConnected!!.postValue(false)
    }

    override fun onServicesDiscovered(
        device: BluetoothDevice,
        optionalServicesFound: Boolean
    ) {
        mConnectionState.postValue(Status.INITIALIZING)
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        mOnDeviceReady.postValue(null)
        if (mBleMeshManager.isProvisioningComplete()) {
            if (mSetupProvisionedNode) {
                if (mMeshNetwork!!.selectedProvisioner.provisionerAddress != null) {
                    mHandler!!.postDelayed({
                        //Adding a slight delay here so we don't send anything before we receive the mesh beacon message
                        val node = mProvisionedMeshNodeLiveData.value
                        if (node != null) {
                            val compositionDataGet =
                                ConfigCompositionDataGet()
                            mMeshManagerApi!!.createMeshPdu(
                                node.unicastAddress,
                                compositionDataGet
                            )
                        }
                    }, 2000)
                } else {
                    mSetupProvisionedNode = false
                    mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.PROVISIONER_UNASSIGNED)
                    clearExtendedMeshNode()
                }
            }
            mIsConnectedToProxy.postValue(true)
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
        mConnectionState.postValue(Status.getError(message))
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
        mNetworkImportState.postValue(error)
    }

    override fun onNetworkImported(meshNetwork: MeshNetwork) { //We can delete the old network after the import has been successful!
//But let's make sure we don't delete the same network in case someone imports the same network ;)
        val oldNet = mMeshNetwork
        if (oldNet!!.meshUUID != meshNetwork.meshUUID) {
            mMeshManagerApi!!.deleteMeshNetworkFromDb(oldNet)
        }
        loadNetwork(meshNetwork)
        loadGroups()
        mNetworkImportState.postValue(
            meshNetwork.meshName + " has been successfully imported.\n" +
                    "In order to start sending messages to this network, please change the provisioner address. " +
                    "Using the same provisioner address will cause messages to be discarded due to the usage of incorrect sequence numbers " +
                    "for this address. However if the network does not contain any nodes you do not need to change the address"
        )
    }

    override fun onNetworkImportFailed(error: String?) {
        mNetworkImportState.postValue(error)
    }

    override fun sendProvisioningPdu(
        meshNode: UnprovisionedMeshNode?,
        pdu: ByteArray?
    ) {
        mBleMeshManager.sendPdu(pdu)
    }

    override fun onMeshPduCreated(pdu: ByteArray?) {
        mBleMeshManager.sendPdu(pdu)
    }

    override fun getMtu(): Int {
        return mBleMeshManager.getPublicMtu()
    }

    override fun onProvisioningStateChanged(
        meshNode: UnprovisionedMeshNode?,
        state: States,
        data: ByteArray?
    ) {
        mUnprovisionedMeshNode = meshNode
        mUnprovisionedMeshNodeLiveData.postValue(meshNode)
        when (state) {
            States.PROVISIONING_INVITE -> mProvisioningStateLiveData = ProvisioningStatusLiveData()
            States.PROVISIONING_FAILED -> mIsProvisioningComplete = false
        }
        mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.fromStatusCode(state.state))
    }

    override fun onProvisioningFailed(
        meshNode: UnprovisionedMeshNode?,
        state: States,
        data: ByteArray?
    ) {
        mUnprovisionedMeshNode = meshNode
        mUnprovisionedMeshNodeLiveData.postValue(meshNode)
        if (state == States.PROVISIONING_FAILED) {
            mIsProvisioningComplete = false
        }
        mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.fromStatusCode(state.state))
    }

    override fun onProvisioningCompleted(
        meshNode: ProvisionedMeshNode?,
        state: States,
        data: ByteArray?
    ) {
        mProvisionedMeshNode = meshNode
        mUnprovisionedMeshNodeLiveData.postValue(null)
        mProvisionedMeshNodeLiveData.postValue(meshNode)
        if (state == States.PROVISIONING_COMPLETE) {
            onProvisioningCompleted(meshNode)
        }
        mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.fromStatusCode(state.state))
    }

    private fun onProvisioningCompleted(node: ProvisionedMeshNode?) {
        mIsProvisioningComplete = true
        mProvisionedMeshNode = node
        mIsReconnecting.postValue(true)
        mBleMeshManager.disconnect().enqueue()
        loadNodes()
        mHandler.post { mConnectionState.postValue(Status.SCANNING) }
        mHandler.postDelayed(
            mReconnectRunnable,
            1000
        ) //Added a slight delay to disconnect and refresh the cache
    }

    /**
     * Here we load all nodes except the current provisioner. This may contain other provisioner nodes if available
     */
    fun loadNodes() {
        val nodes: MutableList<ProvisionedMeshNode> = ArrayList()
        for (node in mMeshNetwork!!.nodes) {
            if (!node.uuid.equals(mMeshNetwork!!.selectedProvisioner.provisionerUuid, ignoreCase = true)) {
                nodes.add(node)
            }
        }
        mProvisionedNodes.postValue(nodes)
    }

    override fun onTransactionFailed(dst: Int, hasIncompleteTimerExpired: Boolean) {
        mProvisionedMeshNode = mMeshNetwork!!.getNode(dst)
        mTransactionStatus.postValue(TransactionStatus(dst, hasIncompleteTimerExpired))
    }

    override fun onUnknownPduReceived(src: Int, accessPayload: ByteArray?) {
        val node = mMeshNetwork!!.getNode(src)
        if (node != null) {
            mProvisionedMeshNode = node
            updateNode(node)
        }
    }

    override fun onBlockAcknowledgementProcessed(dst: Int, message: ControlMessage) {
        val node = mMeshNetwork!!.getNode(dst)
        if (node != null) {
            mProvisionedMeshNode = node
            if (mSetupProvisionedNode) {
                mProvisionedMeshNodeLiveData.postValue(mProvisionedMeshNode)
                mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.SENDING_BLOCK_ACKNOWLEDGEMENT)
            }
        }
    }

    override fun onBlockAcknowledgementReceived(src: Int, message: ControlMessage) {
        val node = mMeshNetwork!!.getNode(src)
        if (node != null) {
            mProvisionedMeshNode = node
            if (mSetupProvisionedNode) {
                mProvisionedMeshNodeLiveData.postValue(node)
                mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.BLOCK_ACKNOWLEDGEMENT_RECEIVED)
            }
        }
    }

    override fun onMeshMessageProcessed(dst: Int, meshMessage: MeshMessage) {
        val node = mMeshNetwork!!.getNode(dst)
        if (node != null) {
            mProvisionedMeshNode = node
            if (meshMessage is ConfigCompositionDataGet) {
                if (mSetupProvisionedNode) {
                    mProvisionedMeshNodeLiveData.postValue(node)
                    mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.COMPOSITION_DATA_GET_SENT)
                }
            } else if (meshMessage is ConfigDefaultTtlGet) {
                if (mSetupProvisionedNode) {
                    mProvisionedMeshNodeLiveData.postValue(node)
                    mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.SENDING_DEFAULT_TTL_GET)
                }
            } else if (meshMessage is ConfigAppKeyAdd) {
                if (mSetupProvisionedNode) {
                    mProvisionedMeshNodeLiveData.postValue(node)
                    mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.SENDING_APP_KEY_ADD)
                }
            } else if (meshMessage is ConfigNetworkTransmitSet) {
                if (mSetupProvisionedNode) {
                    mProvisionedMeshNodeLiveData.postValue(node)
                    mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.SENDING_NETWORK_TRANSMIT_SET)
                }
            }
        }
    }

    override fun onMeshMessageReceived(src: Int, meshMessage: MeshMessage) {
        val node = mMeshNetwork!!.getNode(src)
        if (node != null) if (meshMessage is ProxyConfigFilterStatus) {
            mProvisionedMeshNode = node
            setSelectedMeshNode(node)
            val status = meshMessage
            val unicastAddress = status.src
            Log.v(
                TAG,
                "Proxy configuration source: " + MeshAddress.formatAddress(status.src, false)
            )
            mConnectedProxyAddress.postValue(unicastAddress)
            mMeshMessageLiveData.postValue(status)
        } else if (meshMessage is ConfigCompositionDataStatus) {
            val status = meshMessage
            if (mSetupProvisionedNode) {
                mIsCompositionDataReceived = true
                mProvisionedMeshNodeLiveData.postValue(node)
                mConnectedProxyAddress.postValue(node.unicastAddress)
                mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.COMPOSITION_DATA_STATUS_RECEIVED)
                mHandler!!.postDelayed({
                    val configDefaultTtlGet = ConfigDefaultTtlGet()
                    mMeshManagerApi!!.createMeshPdu(node.unicastAddress, configDefaultTtlGet)
                }, 500)
            } else {
                updateNode(node)
            }
        } else if (meshMessage is ConfigDefaultTtlStatus) {
            if (mSetupProvisionedNode) {
                mIsDefaultTtlReceived = true
                mProvisionedMeshNodeLiveData.postValue(node)
                mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.DEFAULT_TTL_STATUS_RECEIVED)
                mHandler!!.postDelayed({
                    val appKey: ApplicationKey? = mMeshNetworkLiveData.getSelectedAppKey()
                    val index = node.addedNetKeys[0].index
                    val networkKey = mMeshNetwork!!.netKeys[index]
                    val configAppKeyAdd = ConfigAppKeyAdd(networkKey, appKey!!)
                    mMeshManagerApi!!.createMeshPdu(node.unicastAddress, configAppKeyAdd)
                }, 1500)
            } else {
                updateNode(node)
                mMeshMessageLiveData.postValue(meshMessage)
            }
        } else if (meshMessage is ConfigAppKeyStatus) {
            val status = meshMessage
            if (mSetupProvisionedNode) {
                if (status.isSuccessful) {
                    mIsAppKeyAddCompleted = true
                    mProvisionedMeshNodeLiveData.postValue(node)
                    mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.APP_KEY_STATUS_RECEIVED)
                    mHandler!!.postDelayed({
                        val networkTransmitSet =
                            ConfigNetworkTransmitSet(2, 1)
                        mMeshManagerApi!!.createMeshPdu(node.unicastAddress, networkTransmitSet)
                    }, 1500)
                }
            } else {
                updateNode(node)
                mMeshMessageLiveData.postValue(status)
            }
        } else if (meshMessage is ConfigNetworkTransmitStatus) {
            if (mSetupProvisionedNode) {
                mSetupProvisionedNode = false
                mIsNetworkRetransmitSetCompleted = true
                mProvisioningStateLiveData.onMeshNodeStateUpdated(ProvisionerStates.NETWORK_TRANSMIT_STATUS_RECEIVED)
            } else {
                updateNode(node)
                mMeshMessageLiveData.postValue(meshMessage)
            }
        } else if (meshMessage is ConfigModelAppStatus) {
            if (updateNode(node)) {
                val status = meshMessage
                val element =
                    node.elements[status.elementAddress]
                if (node.elements.containsKey(status.elementAddress)) {
                    mSelectedElement.postValue(element)
                    val model = element!!.meshModels[status.modelIdentifier]
                    mSelectedModel.postValue(model)
                }
            }
        } else if (meshMessage is ConfigModelPublicationStatus) {
            if (updateNode(node)) {
                val status =
                    meshMessage
                if (node.elements.containsKey(status.elementAddress)) {
                    val element =
                        node.elements[status.elementAddress]
                    mSelectedElement.postValue(element)
                    val model = element!!.meshModels[status.modelIdentifier]
                    mSelectedModel.postValue(model)
                }
            }
        } else if (meshMessage is ConfigModelSubscriptionStatus) {
            if (updateNode(node)) {
                val status =
                    meshMessage
                if (node.elements.containsKey(status.elementAddress)) {
                    val element =
                        node.elements[status.elementAddress]
                    mSelectedElement.postValue(element)
                    val model = element!!.meshModels[status.modelIdentifier]
                    mSelectedModel.postValue(model)
                }
            }
        } else if (meshMessage is ConfigNodeResetStatus) {
            mBleMeshManager.setClearCacheRequired()
            mExtendedMeshNode!!.postValue(null)
            loadNodes()
            mMeshMessageLiveData.postValue(meshMessage)
        } else if (meshMessage is ConfigRelayStatus) {
            if (updateNode(node)) {
                mMeshMessageLiveData.postValue(meshMessage)
            }
        } else if (meshMessage is ConfigProxyStatus) {
            if (updateNode(node)) {
                mMeshMessageLiveData.postValue(meshMessage)
            }
        } else if (meshMessage is GenericOnOffStatus) {
            if (updateNode(node)) {
                val status = meshMessage
                if (node.elements.containsKey(status.srcAddress)) {
                    val element =
                        node.elements[status.srcAddress]
                    mSelectedElement.postValue(element)
                    val model =
                        element!!.meshModels[SigModelParser.GENERIC_ON_OFF_SERVER.toInt()]
                    mSelectedModel.postValue(model)
                }
            }
        } else if (meshMessage is GenericLevelStatus) {
            if (updateNode(node)) {
                val status = meshMessage
                if (node.elements.containsKey(status.srcAddress)) {
                    val element =
                        node.elements[status.srcAddress]
                    mSelectedElement.postValue(element)
                    val model =
                        element!!.meshModels[SigModelParser.GENERIC_LEVEL_SERVER.toInt()]
                    mSelectedModel.postValue(model)
                }
            }
        }
        if (mMeshMessageLiveData.hasActiveObservers()) {
            mMeshMessageLiveData.postValue(meshMessage)
        }
        //Refresh mesh network live data
        mMeshNetworkLiveData.refresh(mMeshManagerApi.meshNetwork!!)
    }


    /**
     * Received all vendor status and check types
     **/
    override fun onMeshMessageReceived(src: Int, original: MeshMessage, accessMessage: AccessMessage) {
        val node = mMeshNetwork!!.getNode(src)
        var status: VendorModelMessageStatus? = null
        if (node != null) {
            when (original) {
                is NodeGetMessage -> { // Status message
                    status = NodeGetMessageStatus(accessMessage, original.modelIdentifier)
                }
                is NodeConfigMessage -> { // Configuration node message
                    status = NodeConfigMessageStatus(accessMessage, original.modelIdentifier)
                }
                is NodeControlMessage -> { // Control message
                    status = NodeControlMessageStatus(accessMessage, original.modelIdentifier)
                }
                is NodePeripheralMessage -> { // set peripheral message
                    status = NodePeripheralMessageStatus(accessMessage, original.modelIdentifier)
                }
                is VendorModelMessageStatus -> { // Standard or unknown message
                    status = defaultVendorMessage(src, original, accessMessage)
                }
            }
            update(node, status)
        }
        if (mMeshMessageLiveData.hasActiveObservers()) {
            mMeshMessageLiveData.postValue(status)
        }
        // Refresh mesh network live data
        mMeshNetworkLiveData.refresh(mMeshManagerApi.meshNetwork!!)
    }

    private fun update(node: ProvisionedMeshNode, status: VendorModelMessageStatus?) {
        if (updateNode(node)) {
            if (status != null && node.elements.containsKey(status.srcAddress)) {
                val element = node.elements[status.srcAddress]
                mSelectedElement.postValue(element)
                val model = element!!.meshModels[status.modelIdentifier]
                mSelectedModel.postValue(model)
            }
        }
    }

    private fun defaultVendorMessage(src: Int, original: MeshMessage, accessMessage: AccessMessage): VendorModelMessageStatus? {
        if (original is VendorModelMessageAcked) {
            val vendorModelMessageAcked = original as VendorModelMessageAcked
            return VendorModelMessageStatus(accessMessage, vendorModelMessageAcked.modelIdentifier)
        } else if (original is VendorModelMessageUnacked) {
            val vendorModelMessageUnacked = original as VendorModelMessageUnacked
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
        mMeshNetwork = meshNetwork
        if (mMeshNetwork != null) {
            if (!mMeshNetwork!!.isProvisionerSelected) {
                val provisioner = meshNetwork.provisioners[0]
                provisioner.isLastSelected = true
                mMeshNetwork!!.selectProvisioner(provisioner)
            }
            //Load live data with mesh network
            mMeshNetworkLiveData.loadNetworkInformation(meshNetwork)
            //Load live data with provisioned nodes
            loadNodes()
            val node = getSelectedMeshNode()!!.value
            if (node != null) {
                mExtendedMeshNode!!.postValue(mMeshNetwork!!.getNode(node.uuid))
            }
        }
    }

    /**
     * We should only update the selected node, since sending messages to group address will notify with nodes that is not on the UI
     */
    private fun updateNode(node: ProvisionedMeshNode): Boolean {
        if (mProvisionedMeshNode!!.unicastAddress == node.unicastAddress) {
            mProvisionedMeshNode = node
            mExtendedMeshNode!!.postValue(node)
            return true
        }
        return false
    }

    /**
     * Starts reconnecting to the device
     */
    private fun startScan(scanCallback: ScanCallback = getDefaultScanCallback()) {
        this.scanCallback = scanCallback
        if (mIsScanning) return
        mIsScanning = true
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
        mHandler!!.postDelayed(mScannerTimeout, 20000)
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private fun stopScan() {
        mHandler!!.removeCallbacks(mScannerTimeout)
        val scanner = BluetoothLeScannerCompat.getScanner()
        scanner.stopScan(scanCallback)
        mIsScanning = false
    }

    private var scanCallback: ScanCallback = getDefaultScanCallback()

    private fun getDefaultScanCallback() =
        object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) { //In order to connectToProxy to the correct device, the hash advertised in the advertisement data should be matched.
//This is to make sure we connectToProxy to the same device as device addresses could change after provisioning.
                val scanRecord =
                    result.scanRecord
                if (scanRecord != null) {
                    val serviceData: ByteArray? =
                        Utils.getServiceData(result, Constants.MESH_PROXY_UUID)
                    if (serviceData != null) {
                        if (mMeshManagerApi!!.isAdvertisedWithNodeIdentity(serviceData)) {
                            val node = mProvisionedMeshNode
                            if (mMeshManagerApi!!.nodeIdentityMatches(node!!, serviceData)) {
                                stopScan()
                                mConnectionState.postValue(Status.NODE_FOUND)
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
            ) { //In order to connectToProxy to the correct device, the hash advertised in the advertisement data should be matched.
//This is to make sure we connectToProxy to the same device as device addresses could change after provisioning.
                val scanRecord =
                    result.scanRecord
                if (scanRecord != null) {
                    val serviceData = Utils.getServiceData(result, Constants.MESH_PROXY_UUID)
                    if (mMeshManagerApi.isAdvertisingWithNetworkIdentity(serviceData)) {
                        if (mMeshManagerApi.networkIdMatches(networkId, serviceData)) {
                            stopScan()
                            mConnectionState.postValue(Status.NODE_FOUND)
                            connect(context, ExtendedBluetoothDevice(result), true)
                        }
                    } else if (mMeshManagerApi.isAdvertisedWithNodeIdentity(serviceData)) {
                        if (checkIfNodeIdentityMatches(serviceData!!)) {
                            stopScan()
                            mConnectionState.postValue(Status.NODE_FOUND)
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
        val network = mMeshManagerApi.meshNetwork
        if (network != null) {
            if (!network.netKeys.isEmpty()) {
                return mMeshManagerApi.generateNetworkId(network.netKeys[0].key)
            }
        }
        return ""
    }

    private fun checkIfNodeIdentityMatches(serviceData: ByteArray): Boolean {
        val network = mMeshManagerApi.meshNetwork
        if (network != null) {
            for (node in network.nodes) {
                if (mMeshManagerApi.nodeIdentityMatches(node, serviceData)) {
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
        mSetupProvisionedNode = true
        mProvisionedMeshNode = node
        mIsReconnectingFlag = true
        //Added an extra delay to ensure reconnection
        mHandler!!.postDelayed({ connectToProxy(device) }, 2000)
    }

    /**
     * Generates the groups based on the addresses each models have subscribed to
     */
    private fun loadGroups() {
        mGroups.postValue(mMeshNetwork!!.groups)
    }

    private fun updateSelectedGroup() {
        val selectedGroup =
            mSelectedGroupLiveData.value
        if (selectedGroup != null) {
            mSelectedGroupLiveData.postValue(mMeshNetwork!!.getGroup(selectedGroup.address))
        }
    }

    /**
     * Sets the group that was selected from the GroupAdapter.
     */
    fun setSelectedGroup(address: Int) {
        val group = mMeshNetwork!!.getGroup(address)
        if (group != null) {
            mSelectedGroupLiveData.postValue(group)
        }
    }

}