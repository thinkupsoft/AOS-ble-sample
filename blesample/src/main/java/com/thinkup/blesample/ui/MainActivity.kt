package com.thinkup.blesample.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
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
        provisionedNodes.layoutManager = LinearLayoutManager(this)
        provisionedNodes.adapter = adapter
        provisionedNodes.addItemDecoration(DividerHelper(this))
        adapter.addRenderer(NodeRenderer(this))
        settings()
        updateList()
    }

    private fun settings() {
        bleNode.settings()
        settingsButton.setOnClickListener {
            val checkboxLayout: View = layoutInflater.inflate(R.layout.item_settings, null)
            (checkboxLayout.findViewById(R.id.startSetting) as CheckBox).isChecked = bleNode.settings().enabledStartConfig()
            (checkboxLayout.findViewById(R.id.keepSetting) as CheckBox).isChecked = bleNode.settings().enabledKeepAlive()
            (checkboxLayout.findViewById(R.id.configSetting) as CheckBox).isChecked = bleNode.settings().enabledProvisionConfig()
            AlertDialog.Builder(this)
                .setView(checkboxLayout)
                .setTitle("Settings")
                .setMessage("Choose your preferences")
                .setPositiveButton("OK") { _, _ ->
                    bleNode.settings().set(
                        (checkboxLayout.findViewById(R.id.startSetting) as CheckBox).isChecked,
                        (checkboxLayout.findViewById(R.id.keepSetting) as CheckBox).isChecked,
                        (checkboxLayout.findViewById(R.id.configSetting) as CheckBox).isChecked
                    )
                }
                .create().show()

        }
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
