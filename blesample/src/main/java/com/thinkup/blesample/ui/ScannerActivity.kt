package com.thinkup.blesample.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.PermissionUtil
import com.thinkup.blesample.R
import com.thinkup.blesample.renderers.DeviceRenderer
import com.thinkup.connectivity.BleProvisioner
import com.thinkup.connectivity.BleScanner
import com.thinkup.connectivity.common.Status
import com.thinkup.connectivity.scanner.ScannerLiveData
import com.thinkup.connectivity.utils.CapabilitiesUtil
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_scanner.*
import kotlinx.android.synthetic.main.activity_scanner.connectionStatus
import kotlinx.android.synthetic.main.activity_scanner.toolbar
import org.koin.android.ext.android.inject

class ScannerActivity : AppCompatActivity(), DeviceRenderer.Callback {

    private val permissionUtil = PermissionUtil()
    private val capabilitiesUtil = CapabilitiesUtil()
    private val adapter = RendererAdapter()
    private val bleScanner: BleScanner by inject()
    private val bleProvisioner: BleProvisioner by inject()

    private fun startConnection() {
        toolbar.title = title
        connectionStatus.text = if (bleProvisioner.isConnected()?.value == true) "Connected" else "Disconnected"
        connectionStatus.setOnClickListener {
            if (bleProvisioner.isConnected()?.value == true) bleProvisioner.disconnect()
            else bleProvisioner.autoConnect()
        }
        bleProvisioner.isConnected()?.observe(this, Observer {
            connectionStatus.text = if (bleProvisioner.isConnected()?.value == true) {
                "Connected"
            } else {
                bleProvisioner.autoConnect()
                "Disconnected"
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        startConnection()
        prepare()
        if (!permissionUtil.check(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionUtil.request(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
                PermissionUtil.REQUEST_PERMISSION_LOCATION
            )
        } else load()
    }

    override fun onStart() {
        super.onStart()
        bleScanner.getState().startScanning()
    }

    override fun onStop() {
        super.onStop()
        bleScanner.stop()
    }

    private fun prepare() {
        devicesList.layoutManager = LinearLayoutManager(this)
        devicesList.adapter = adapter
        adapter.addRenderer(DeviceRenderer(this))
    }

    private fun load() {
        bleScanner.getState().observe(this, Observer {
            when {
                !it.isBluetoothEnabled() -> capabilitiesUtil.requestBluetooth(this)
                !it.isLocationEnabled() -> capabilitiesUtil.requestLocation(this)
                it.isBluetoothEnabled() && !it.isScanning() -> bleScanner.scan()
                it.isEmpty() -> Toast.makeText(
                    this,
                    "Aún no hay devices",
                    Toast.LENGTH_SHORT
                ).show()
            }
            if (it.getDevices().isNotEmpty()) setItems(it)
        })
        bleScanner.scan()
    }

    private fun setItems(it: ScannerLiveData) {
        val index = it.getUpdatedDeviceIndex()
        adapter.setItems(it.getDevices())
        if (index != null && index != -1)
            adapter.notifyItemChanged(index)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PermissionUtil.REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                load()
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PermissionUtil.REQUEST_PERMISSION_LOCATION) {
            load()
        } else super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onConnect(device: ExtendedBluetoothDevice, textView: TextView) {
        bleProvisioner.getStatus().removeObservers(this)
        bleProvisioner.connect(this, this, device).observe(this, Observer {
            textView.isClickable = false
            textView.text = it.getValue()
            when (it) {
                Status.ERROR,
                Status.PROVISIONING_FAILED,
                Status.DISCONNECTING -> {
                    textView.setTextColor(ContextCompat.getColor(this, R.color.red))
                    textView.isClickable = true
                    textView.setOnClickListener { bleProvisioner.connect(this, this, device) }
                }
                Status.NODE_FOUND,
                Status.CONNECTED,
                Status.BINDING_KEY,
                Status.PROVISIONING_COMPLETE -> {
                    textView.setTextColor(ContextCompat.getColor(this, R.color.green))
                }
                Status.FULL_CONFIGURED -> {
                    AlertDialog.Builder(this)
                        .setMessage("Node provisioned and configured successfully")
                        .setOnCancelListener { finish() }
                        .setPositiveButton("Ok") { p0, p1 -> finish() }
                        .show()
                }
                Status.READY -> {
                    textView.text = "Provision"
                    bleProvisioner.provisioningAction(this)
                }
                else -> {
                    textView.setTextColor(ContextCompat.getColor(this, R.color.neutral))
                }
            }

        })
    }
}
