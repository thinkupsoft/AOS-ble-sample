package com.thinkup.connectivity.scanner

import androidx.lifecycle.LiveData
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import no.nordicsemi.android.meshprovisioner.MeshBeacon
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.ArrayList

class ScannerLiveData(private var bluetoothEnabled: Boolean, private var locationEnabled: Boolean) :
    LiveData<ScannerLiveData>() {

    private val devices = ArrayList<ExtendedBluetoothDevice>()
    private var updatedDeviceIndex: Int? = null
    private var scanningStarted: Boolean = false
    private var startScanning: Boolean = false
    private var stopScanning: Boolean = false

    init {
        postValue(this)
    }

    fun refresh() {
        postValue(this)
    }

    /**
     * Updates the flag to notify scanner live data that a stop scan was requested.
     */
    fun startScanning() {
        devices.clear() //Clear the devices on resuming the scan
        stopScanning = false
        startScanning = true
        setValue(this)
    }

    fun isScanRequested(): Boolean {
        return startScanning
    }

    /**
     * Updates the flag to notify scanner live data that a stop scan was requested.
     */
    fun stopScanning() {
        stopScanning = true
        startScanning = false
        setValue(this)
    }

    internal fun isStopScanRequested(): Boolean {
        return stopScanning
    }

    internal fun scanningStarted() {
        scanningStarted = true
        setValue(this)
    }

    internal fun scanningStopped() {
        scanningStarted = false
        setValue(this)
    }

    internal fun bluetoothEnabled() {
        bluetoothEnabled = true
        postValue(this)
    }

    internal fun bluetoothDisabled() {
        bluetoothEnabled = false
        updatedDeviceIndex = null
        devices.clear()
        postValue(this)
    }

    internal fun deviceDiscovered(result: ScanResult) {
        val device: ExtendedBluetoothDevice

        val index = indexOf(result)
        if (index == -1) {
            device = ExtendedBluetoothDevice(result)
            devices.add(device)
            updatedDeviceIndex = null
        } else {
            device = devices[index]
            updatedDeviceIndex = index
        }
        // Update RSSI and name
        device.rssi = result.rssi
        device.name = result.scanRecord!!.deviceName!!

        postValue(this)
    }

    internal fun deviceDiscovered(result: ScanResult, beacon: MeshBeacon) {
        val device: ExtendedBluetoothDevice

        val index = indexOf(result)
        if (index == -1) {
            device = ExtendedBluetoothDevice(result, beacon)
            devices.add(device)
            updatedDeviceIndex = null
        } else {
            device = devices[index]
            updatedDeviceIndex = index
        }
        // Update RSSI and name
        device.rssi = result.rssi
        device.name = result.scanRecord!!.deviceName!!

        postValue(this)
    }

    /**
     * Returns the list of devices.
     *
     * @return current list of devices discovered
     */
    fun getDevices(): List<ExtendedBluetoothDevice> {
        return devices
    }

    /**
     * Returns null if a new item_scan_device was added, or an index of the updated item_scan_device.
     */
    fun getUpdatedDeviceIndex(): Int? {
        val i = updatedDeviceIndex
        updatedDeviceIndex = null
        return i
    }

    /**
     * Returns whether the list is empty.
     */
    fun isEmpty(): Boolean {
        return devices.isEmpty()
    }

    /**
     * Returns whether scanning is in progress.
     */
    fun isScanning(): Boolean {
        return scanningStarted
    }

    fun isScanStopped(): Boolean {
        return scanningStarted
    }

    /**
     * Returns whether Bluetooth adapter is enabled.
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothEnabled
    }

    /**
     * Returns whether Location is enabled.
     */
    fun isLocationEnabled(): Boolean {
        return locationEnabled
    }

    internal fun setLocationEnabled(enabled: Boolean) {
        locationEnabled = enabled
        postValue(this)
    }

    /**
     * Finds the index of existing devices on the scan results list.
     *
     * @param result scan result
     * @return index of -1 if not found
     */
    private fun indexOf(result: ScanResult): Int {
        var i = 0
        for (device in devices) {
            if (device.matches(result))
                return i
            i++
        }
        return -1
    }
}