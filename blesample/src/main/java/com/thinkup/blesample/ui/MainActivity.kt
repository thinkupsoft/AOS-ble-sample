package com.thinkup.blesample.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.blesample.renderers.DeviceRenderer
import com.thinkup.blesample.renderers.DividerHelper
import com.thinkup.blesample.renderers.NodeRenderer
import com.thinkup.connectivity.BleNode
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity(), DeviceRenderer.Callback, NodeRenderer.Callback {

    val adapter = RendererAdapter()
    val bleNode: BleNode by inject()
    override fun title(): String = "Neural BLE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addNodeButton.setOnClickListener {
            startActivityForResult(Intent(this, ScannerActivity::class.java), SCANNER_REQUEST)
        }
        groupsButton.setOnClickListener {
            startActivityForResult(Intent(this, GroupsActivity::class.java), GROUPS_REQUEST)
        }
        trainingButton.setOnClickListener {
            startActivityForResult(Intent(this, TrainingActivity::class.java), TRAINING_REQUEST)
        }
        scheduledButton.setOnClickListener {
            startActivityForResult(Intent(this, ScheduledActivity::class.java), SCHEDULED_REQUEST)
        }
        provisionedNodes.layoutManager = LinearLayoutManager(this)
        provisionedNodes.adapter = adapter
        provisionedNodes.addItemDecoration(DividerHelper(this))
        adapter.addRenderer(NodeRenderer(this))
        updateList()
    }

    private fun updateList() {
        bleNode.getProvisionedNodes().observe(this, Observer { list ->
            identify.setOnClickListener { bleNode.identify(list) }
            adapter.setItems(list)
        })
    }

    override fun onConnect(device: ExtendedBluetoothDevice, textView: TextView) {}

    override fun onDelete(node: ProvisionedMeshNode) {
        bleNode.delete(node)
    }

    override fun onForceDelete(node: ProvisionedMeshNode) {
        bleNode.deleteDB(node)
    }

    override fun onClick(node: ProvisionedMeshNode) {
        val intent = Intent(this, NodeDetailActivity::class.java).apply {
            putExtra(NodeDetailActivity.NODE, node)
        }
        startActivityForResult(intent, DETAIL_REQUEST)
    }
}
