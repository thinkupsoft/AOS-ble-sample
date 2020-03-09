package com.thinkup.connectivity.mesh

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.thinkup.connectivity.utils.Constants
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.data.Data

class BleMeshManager(context: Context): LoggableBleManager<BleMeshManagerCallbacks>(context) {

    private var mMeshProvisioningDataInCharacteristic: BluetoothGattCharacteristic? = null
    private var mMeshProvisioningDataOutCharacteristic: BluetoothGattCharacteristic? = null
    private var mMeshProxyDataInCharacteristic: BluetoothGattCharacteristic? = null
    private var mMeshProxyDataOutCharacteristic: BluetoothGattCharacteristic? = null

    private var isProvisioningComplete = false
    private var mIsDeviceReady = false
    private var mNodeReset = false
    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving notifications, etc.
     */
    private val mGattCallback: BleManagerGattCallback = object : BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val meshProxyService =
                gatt.getService(Constants.MESH_PROXY_UUID)
            if (meshProxyService != null) {
                isProvisioningComplete = true
                mMeshProxyDataInCharacteristic =
                    meshProxyService.getCharacteristic(Constants.MESH_PROXY_DATA_IN)
                mMeshProxyDataOutCharacteristic =
                    meshProxyService.getCharacteristic(Constants.MESH_PROXY_DATA_OUT)
                return mMeshProxyDataInCharacteristic != null && mMeshProxyDataOutCharacteristic != null &&
                        hasNotifyProperty(mMeshProxyDataOutCharacteristic!!) &&
                        hasWriteNoResponseProperty(mMeshProxyDataInCharacteristic!!)
            }
            val meshProvisioningService =
                gatt.getService(Constants.MESH_PROVISIONING_UUID)
            if (meshProvisioningService != null) {
                isProvisioningComplete = false
                mMeshProvisioningDataInCharacteristic =
                    meshProvisioningService.getCharacteristic(Constants.MESH_PROVISIONING_DATA_IN)
                mMeshProvisioningDataOutCharacteristic =
                    meshProvisioningService.getCharacteristic(Constants.MESH_PROVISIONING_DATA_OUT)
                return mMeshProvisioningDataInCharacteristic != null && mMeshProvisioningDataOutCharacteristic != null &&
                        hasNotifyProperty(mMeshProvisioningDataOutCharacteristic!!) &&
                        hasWriteNoResponseProperty(mMeshProvisioningDataInCharacteristic!!)
            }
            return false
        }

        override fun initialize() {
            requestMtu(Constants.MTU_SIZE_MAX).enqueue()
            // This callback will be called each time a notification is received.
            val onDataReceived =
                DataReceivedCallback { device: BluetoothDevice?, data: Data ->
                    mCallbacks.onDataReceived(
                        device,
                        mtu,
                        data.value!!
                    )
                }
            // Set the notification callback and enable notification on Data In characteristic.
            val characteristic =
                if (isProvisioningComplete) mMeshProxyDataOutCharacteristic else mMeshProvisioningDataOutCharacteristic
            setNotificationCallback(characteristic).with(onDataReceived)
            enableNotifications(characteristic).enqueue()
        }

        override fun onDeviceDisconnected() {
            mIsDeviceReady = false
            isProvisioningComplete = false
            mMeshProvisioningDataInCharacteristic = null
            mMeshProvisioningDataOutCharacteristic = null
            mMeshProxyDataInCharacteristic = null
            mMeshProxyDataOutCharacteristic = null
        }

        override fun onDeviceReady() {
            mIsDeviceReady = true
            super.onDeviceReady()
        }
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return mGattCallback
    }

    override fun shouldClearCacheWhenDisconnected(): Boolean { // This is to make sure that Android will discover the services as the the mesh node will
// change the provisioning service to a proxy service.
        val result = !isProvisioningComplete || mNodeReset
        mNodeReset = false
        return result
    }

    /**
     * After calling this method the device cache will be cleared upon next disconnection.
     */
    fun setClearCacheRequired() {
        mNodeReset = true
    }

    /**
     * Sends the mesh pdu.
     *
     *
     * The function will chunk the pdu to fit in to the mtu size supported by the node.
     *
     * @param pdu mesh pdu.
     */
    fun sendPdu(pdu: ByteArray?) {
        if (!mIsDeviceReady) return
        // This callback will be called each time the data were sent.
        val callback =
            DataSentCallback { device: BluetoothDevice?, data: Data ->
                mCallbacks.onDataSent(
                    device,
                    mtu,
                    data.value!!
                )
            }
        // Write the right characteristic.
        val characteristic =
            if (isProvisioningComplete) mMeshProxyDataInCharacteristic else mMeshProvisioningDataInCharacteristic
        writeCharacteristic(characteristic, pdu)
            .split()
            .with(callback)
            .enqueue()
    }

    fun getPublicMtu(): Int {
        return super.getMtu()
    }

    fun isProvisioningComplete(): Boolean {
        return isProvisioningComplete
    }

    fun isDeviceReady(): Boolean {
        return mIsDeviceReady
    }

    private fun hasWriteNoResponseProperty(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
    }

    private fun hasNotifyProperty(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
    }
}