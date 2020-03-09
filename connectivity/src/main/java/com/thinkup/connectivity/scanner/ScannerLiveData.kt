package com.thinkup.connectivity.scanner

import android.os.Handler
import android.util.Log
import androidx.core.os.HandlerCompat.postDelayed
import androidx.lifecycle.LiveData
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import no.nordicsemi.android.meshprovisioner.MeshBeacon
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*

class ScannerLiveData(private var bluetoothEnabled: Boolean, private var locationEnabled: Boolean) :
    LiveData<ScannerLiveData>() {

    private val devices = ArrayList<ExtendedBluetoothDevice>()
    private var updatedDeviceIndex: Int? = null
    private var scanningStarted: Boolean = false
    private var startScanning: Boolean = false
    private var stopScanning: Boolean = false
    var requestingLocation: Boolean = false
    private val handler: Handler = Handler()
    private val delayMs: Long = 800
    private val timeLapse: Long = 800
    private lateinit var runnable: Runnable
    init {
        postValue(this)
        initRunnable()
        startTimer()
    }

    fun refresh() {
        postValue(this)
    }

    private fun initRunnable(){
         runnable = object : Runnable {
             override fun run() {
                 checkOnlineDevices()
                 handler.postDelayed(this, delayMs)
             }
         }
    }

    private fun startTimer(){
        handler.postDelayed(runnable, delayMs)
    }

    //Updates the devices collection
    private fun checkOnlineDevices(){
        val indexList = mutableListOf<Int>()
        //Iterate on devices collection to check online devices
        for (i in 0 until devices.size -1){
            val actualTime = System.currentTimeMillis()
            if ((actualTime - devices[i].timestamp) > timeLapse)
                indexList.add(i)
        }
        //remove devices that weren't scanned during timeLapse
        indexList.forEach{i ->
            devices.removeAt(i)
        }
        if (devices.isEmpty())
            postValue(this)
    }
    /**
     * Updates the flag to notify scanner live data that a stop scan was requested.
     */
    fun startScanning() {
        devices.clear() //Clear the devices on resuming the scan
        stopScanning = false
        startScanning = true
        postValue(this)
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
        handler.removeCallbacks(runnable) ///STOPS THE HANDLER
        postValue(this)
    }

    internal fun isStopScanRequested(): Boolean {
        return stopScanning
    }

    internal fun scanningStarted() {
        scanningStarted = true
        postValue(this)
    }

    internal fun scanningStopped() {
        scanningStarted = false
        postValue(this)
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
            device = ExtendedBluetoothDevice(result,timestamp = System.currentTimeMillis())
            devices.add(device)
            updatedDeviceIndex = null
        } else {
            device = devices[index]
            updatedDeviceIndex = index
        }
        // Update RSSI, name and timeStamp
        device.rssi = result.rssi
        device.name = result.scanRecord!!.deviceName!!
        device.timestamp = System.currentTimeMillis()

        postValue(this)
    }

    internal fun deviceDiscovered(result: ScanResult, beacon: MeshBeacon) {
        val device: ExtendedBluetoothDevice

        val index = indexOf(result)
        if (index == -1) {
            device = ExtendedBluetoothDevice(result, beacon, System.currentTimeMillis())
            devices.add(device)
            updatedDeviceIndex = null
        } else {
            device = devices[index]
            updatedDeviceIndex = index
        }
        // Update RSSI and name
        device.rssi = result.rssi
        device.name = result.scanRecord!!.deviceName!!
        device.timestamp = System.currentTimeMillis()

        postValue(this)
    }

    /**
     * Checks if the provisioned device is in the list
     * Remove the device from devices collection in case it exists
     */
    internal fun removeDeviceIfExists(result: ScanResult){
        val index = indexOf(result)
        if (index != -1){
           devices.removeAt(index)
            println("devices size "+ devices.size)
        }
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

    fun isRequestingLocation(): Boolean {
        return requestingLocation
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