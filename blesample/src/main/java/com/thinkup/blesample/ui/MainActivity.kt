package com.thinkup.blesample.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.blesample.renderers.DeviceRenderer
import com.thinkup.blesample.renderers.NodeRenderer
import com.thinkup.connectivity.BleNode
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_main.*
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity(), DeviceRenderer.Callback, NodeRenderer.Callback {

    val SCANNER_REQUEST = 100
    val GROUPS_REQUEST = 101
    val DETAIL_REQUEST = 102
    val adapter = RendererAdapter()
    val bleNode: BleNode by inject()

    private fun startConnection() {
        toolbar.title = title
        connectionStatus.text = if (bleNode.isConnected()?.value == true) "Connected" else "Disconnected"
        connectionStatus.setOnClickListener {
            if (bleNode.isConnected()?.value == true) bleNode.disconnect()
            else bleNode.autoConnect()
        }
        bleNode.isConnected()?.observe(this, Observer {
            connectionStatus.text = if (bleNode.isConnected()?.value == true) {
                "Connected"
            } else {
                bleNode.autoConnect()
                "Disconnected"
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startConnection()
        addNodeButton.setOnClickListener {
            startActivityForResult(Intent(this, ScannerActivity::class.java), SCANNER_REQUEST)
        }
        groupsButton.setOnClickListener {
            startActivityForResult(Intent(this, GroupsActivity::class.java), GROUPS_REQUEST)
        }

        provisionedNodes.layoutManager = LinearLayoutManager(this)
        provisionedNodes.adapter = adapter
        adapter.addRenderer(NodeRenderer(this))
        updateList()
    }

    private fun updateList() {
        bleNode.getProvisionedNodes().observe(this, Observer {
            adapter.setItems(it)
        })
    }

    override fun onConnect(device: ExtendedBluetoothDevice, textView: TextView) {}

    override fun onDelete(node: ProvisionedMeshNode) {
        bleNode.delete(this, node)
    }

    override fun onClick(node: ProvisionedMeshNode) {
        val intent = Intent(this, NodeDetailActivity::class.java).apply {
            putExtra(NodeDetailActivity.NODE, node)
        }
        startActivityForResult(intent, DETAIL_REQUEST)
    }
}
