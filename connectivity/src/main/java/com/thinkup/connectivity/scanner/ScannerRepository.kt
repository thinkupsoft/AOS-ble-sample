package com.thinkup.connectivity.scanner

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.ParcelUuid
import com.thinkup.connectivity.utils.CapabilitiesUtil
import com.thinkup.connectivity.utils.Constants
import com.thinkup.connectivity.utils.Utils
import no.nordicsemi.android.meshprovisioner.MeshManagerApi
import no.nordicsemi.android.support.v18.scanner.*
import java.util.*

class ScannerRepository(
    private val context: Context,
    private val meshManagerApi: MeshManagerApi,
    private val capabilitiesUtil: CapabilitiesUtil
) {

    private var networkId: String? = null
    private var filterUuid: UUID? = null

    /**
     * MutableLiveData containing the scanner state to notify app.
     */
    private var scannerLiveData: ScannerLiveData =
        ScannerLiveData(
            capabilitiesUtil.isBleEnabled(),
            capabilitiesUtil.isLocationEnabled(context)
        )

    private val scanCallbacks = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                if (filterUuid == Constants.MESH_PROVISIONING_UUID) {
                    if (!scannerLiveData.isStopScanRequested()) {
                        updateScannerLiveData(result)
                    }
                } else if (filterUuid == Constants.MESH_PROXY_UUID) {
                    val serviceData = Utils.getServiceData(result, Constants.MESH_PROXY_UUID)
                    if (meshManagerApi.isAdvertisingWithNetworkIdentity(serviceData)) {
                        if (meshManagerApi.networkIdMatches(networkId!!, serviceData)) {
                            updateScannerLiveData(result)
                        }
                    } else if (meshManagerApi.isAdvertisedWithNodeIdentity(serviceData)) {
                        if (checkIfNodeIdentityMatches(serviceData)) {
                            updateScannerLiveData(result)
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // Batch scan is disabled (report delay = 0)
        }

        override fun onScanFailed(errorCode: Int) {
            try {
                scannerLiveData.scanningStopped()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the location provider
     */
    private val locationProviderChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val enabled = capabilitiesUtil.isLocationEnabled(context)
            scannerLiveData.setLocationEnabled(enabled)
        }
    }
    /**
     * Broadcast receiver to monitor the changes in the bluetooth adapter
     */
    private val bluetoothStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val previousState = intent.getIntExtra(
                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                BluetoothAdapter.STATE_OFF
            )

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    scannerLiveData.bluetoothEnabled()
                    scannerLiveData.startScanning()
                }
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                    stopScan()
                    scannerLiveData.bluetoothDisabled()
                }
            }
        }
    }

    fun getScannerState(): ScannerLiveData {
        return scannerLiveData
    }

    private fun updateScannerLiveData(result: ScanResult) {
        val scanRecord = result.scanRecord
        if (scanRecord != null && scanRecord.bytes != null) {
            meshManagerApi.getMeshBeaconData(scanRecord.bytes!!)?.let {
                scannerLiveData.deviceDiscovered(
                    result,
                    meshManagerApi.getMeshBeacon(it)!!
                )
            } ?: run {
                scannerLiveData.deviceDiscovered(result)
            }
        }
    }

    /**
     * Register for required broadcast receivers.
     */
    fun registerBroadcastReceivers() {
        context.registerReceiver(
            bluetoothStateBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        if (Utils.isMarshmallowOrAbove()) {
            context.registerReceiver(
                locationProviderChangedReceiver,
                IntentFilter(LocationManager.MODE_CHANGED_ACTION)
            )
        }
    }

    /**
     * Unregister for required broadcast receivers.
     */
    fun unregisterBroadcastReceivers() {
        context.unregisterReceiver(bluetoothStateBroadcastReceiver)
        if (Utils.isMarshmallowOrAbove()) {
            context.unregisterReceiver(locationProviderChangedReceiver)
        }
    }

    /**
     * Start scanning for Bluetooth devices.
     *
     * @param filterUuid UUID to filter scan results with
     */
    fun startScan(filterUuid: UUID) {
        try {
            this.filterUuid = filterUuid

            if (scannerLiveData.isScanRequested()) {
                if (scannerLiveData.isScanning()) {
                    return
                }
            }

            if (scannerLiveData.isStopScanRequested()) {
                return
            }

            if (this.filterUuid == Constants.MESH_PROXY_UUID) {
                val network = meshManagerApi.meshNetwork
                if (network != null) {
                    if (network.netKeys.isNotEmpty()) {
                        networkId = meshManagerApi.generateNetworkId(network.netKeys[0].key)
                    }
                }
            }

            scannerLiveData.scanningStarted()
            //Scanning settings
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Refresh the devices list every second
                .setReportDelay(0)
                // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(false)
                // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
                /*.setUseHardwareBatchingIfSupported(false)*/
                .build()

            // Let's use the filter to scan only for unprovisioned mesh nodes.
            val filters: MutableList<ScanFilter> = mutableListOf()
            filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(filterUuid)).build())

            val scanner = BluetoothLeScannerCompat.getScanner()
            scanner.startScan(filters, settings, scanCallbacks)
        } catch (ex: java.lang.Exception) {
            BluetoothLeScannerCompat.getScanner().stopScan(scanCallbacks)
            startScan(filterUuid)
        }
    }

    /**
     * stop scanning for bluetooth devices.
     */
    fun stopScan() {
        scannerLiveData.stopScanning()
        val scanner = BluetoothLeScannerCompat.getScanner()
        scanner.stopScan(scanCallbacks)
        scannerLiveData.scanningStopped()
    }

    /**
     * Check if node identity matches
     *
     * @param serviceData service data received from the advertising data
     * @return true if the node identity matches or false otherwise
     */
    private fun checkIfNodeIdentityMatches(serviceData: ByteArray?): Boolean {
        val network = meshManagerApi.meshNetwork
        if (network != null && serviceData != null) {
            for (node in network.nodes) {
                if (meshManagerApi.nodeIdentityMatches(node, serviceData)) {
                    return true
                }
            }
        }
        return false

    }
}