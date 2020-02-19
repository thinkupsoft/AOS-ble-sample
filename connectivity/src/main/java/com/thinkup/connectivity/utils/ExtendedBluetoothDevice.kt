package com.thinkup.connectivity.utils

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import no.nordicsemi.android.meshprovisioner.MeshBeacon
import no.nordicsemi.android.support.v18.scanner.ScanResult

@Parcelize
class ExtendedBluetoothDevice(
    val scanResult: ScanResult,
    var beacon: MeshBeacon? = null,
    var timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    val device: BluetoothDevice = scanResult.device
    var name: String = scanResult.scanRecord?.deviceName ?: "Unknown"
    var rssi: Int = scanResult.rssi

    fun matches(scanResult: ScanResult): Boolean {
        return device.address == scanResult.device.address
    }

    fun rssiPercentage() = (100.0f * (127.0f + rssi) / (127.0f + 20.0f)).toInt()

    fun getAddress() = device.address

    override fun equals(o: Any?): Boolean {
        if (o is ExtendedBluetoothDevice) {
            val that = o as ExtendedBluetoothDevice?
            return device.address == that!!.device.address
        }
        return super.equals(o)
    }
}