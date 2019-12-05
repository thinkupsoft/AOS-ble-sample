package com.thinkup.blesample.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.blesample.renderers.GroupRenderer
import com.thinkup.connectivity.groups.GroupViewModel
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_groups.*
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelSubscriptionStatus
import no.nordicsemi.android.meshprovisioner.transport.MeshModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.viewmodel.ext.android.viewModel

class GroupsActivity : AppCompatActivity(), GroupRenderer.Callback {

    val adapter = RendererAdapter()
    val viewModel: GroupViewModel by viewModel()
    var char = 'A'

    private fun startConnection() {
        toolbar.title = title
        connectionStatus.text = if (viewModel.isConnected()?.value == true) "Connected" else "Disconnected"
        connectionStatus.setOnClickListener {
            if (viewModel.isConnected()?.value == true) viewModel.disconnect()
            else viewModel.autoConnect()
        }
        viewModel.isConnected()?.observe(this, Observer {
            connectionStatus.text = if (viewModel.isConnected()?.value == true) {
                "Connected"
            } else {
                viewModel.autoConnect()
                "Disconnected"
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)
        addGroup.setOnClickListener {
            viewModel.addGroup("Grupo $char")
        }
        startConnection()
        updateMessage()

        listGroups.layoutManager = LinearLayoutManager(this)
        listGroups.adapter = adapter
        adapter.addRenderer(GroupRenderer(this))

        viewModel.getGroups().observe(this, Observer {
            if (it.isNotEmpty()) {
                listGroups.visibility = View.VISIBLE
                emptygroups.visibility = View.GONE
                adapter.setItems(it)
                char = 'A' + it.size
            } else {
                listGroups.visibility = View.GONE
                emptygroups.visibility = View.VISIBLE
            }
        })
    }

    override fun onDelete(item: Group) {
        viewModel.removeGroup(item)
    }

    private fun updateMessage() {
        viewModel.getMessages().observe(this, Observer {
            it?.let {
                if (it is ConfigModelSubscriptionStatus) {
                    val status = it
                    if (status.isSuccessful)
                        AlertDialog.Builder(this)
                            .setMessage("Operation success")
                            .setOnCancelListener { finish() }
                            .setPositiveButton("Ok") { p0, p1 -> }
                            .show()
                }
            }
        })
    }

    override fun onSubscribe(item: Group) {
        val nodes = viewModel.getNodes()
        val arrayAdapter = ArrayAdapter<ProvisionedMeshNode>(this, android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(nodes ?: mutableListOf())
        AlertDialog.Builder(this)
            .setAdapter(arrayAdapter) { p0, p1 ->
                val node = nodes?.get(p1)
                viewModel.addGroupNode(item, node)
            }.show()
    }

    override fun onDeleteItem(item: Group, model: VendorModel) {
        viewModel.removeGroupNode(item, model)
    }

    override fun getModels(item: Group): List<MeshModel> {
        return viewModel.getGroupNodes(item)
    }

    override fun onGetStatus(item: Group, model: List<MeshModel>) {
        viewModel.getStatus(item, model[0] as VendorModel)
    }

    override fun onGetTtl(item: Group) {
        viewModel.getTtl(item)
    }
}
