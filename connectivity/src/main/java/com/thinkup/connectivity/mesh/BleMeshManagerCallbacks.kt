package com.thinkup.connectivity.mesh

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.BleManagerCallbacks

interface BleMeshManagerCallbacks: BleManagerCallbacks {
    fun onDataReceived(bluetoothDevice: BluetoothDevice?, mtu: Int, pdu: ByteArray)
    fun onDataSent(device: BluetoothDevice?, mtu: Int, pdu: ByteArray)
}