package com.thinkup.blesample.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.blesample.renderers.DividerHelper
import com.thinkup.blesample.renderers.GroupRenderer
import com.thinkup.connectivity.BleGroup
import com.thinkup.connectivity.utils.EventObserver
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_groups.*
import kotlinx.android.synthetic.main.toolbar.*
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelSubscriptionStatus
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.ext.android.inject
import java.util.concurrent.ThreadPoolExecutor

class GroupsActivity : BaseActivity(), GroupRenderer.Callback {

    val adapter = RendererAdapter()
    val viewModel: BleGroup by inject()
    var char = 'A'

    override fun title(): String = "Groups"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)
        addGroup.setOnClickListener {
            Thread { viewModel.addGroup("Grupo $char") }.start()
        }
        updateMessage()

        listGroups.layoutManager = LinearLayoutManager(this)
        listGroups.adapter = adapter
        listGroups.addItemDecoration(DividerHelper(this))
        adapter.addRenderer(GroupRenderer(this))

        viewModel.getGroups().observe(this, Observer {
            if (it.isNotEmpty()) {
                identify.setOnClickListener { v -> viewModel.identify(it) }
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
        Thread { viewModel.removeGroup(item) }.start()
    }

    private fun updateMessage() {
        viewModel.getMessages().setObserver(object : EventObserver.Callback<MeshMessage?> {
            override fun onPost(e: MeshMessage?) {
                runOnUiThread {
                    e?.let {
                        if (it is ConfigModelSubscriptionStatus) {
                            val status = it
                            if (status.isSuccessful)
                                AlertDialog.Builder(this@GroupsActivity)
                                    .setMessage("Operation success")
                                    .setOnCancelListener { finish() }
                                    .setPositiveButton("Ok") { p0, p1 -> }
                                    .show()
                        }
                    }
                }
            }
        })
    }

    override fun onSubscribe(item: Group) {
        val nodes = viewModel.getNodes()
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(nodes?.map { "Node ${it.nodeName}" } ?: mutableListOf())
        AlertDialog.Builder(this)
            .setAdapter(arrayAdapter) { p0, p1 ->
                val node = nodes?.get(p1)
                viewModel.addGroupNode(item, node)
            }.show()
    }

    override fun onDeleteItem(item: Group, meshNode: ProvisionedMeshNode) {
        viewModel.removeGroupNode(item, meshNode)
    }

    override fun getModels(item: Group): List<ProvisionedMeshNode> {
        return viewModel.getGroupNodes(item)
    }

    override fun onMore(item: Group) {
        val intent = Intent(this, GroupDetailActivity::class.java).apply {
            putExtra(GroupDetailActivity.GROUP, item)
        }
        startActivityForResult(intent, GroupDetailActivity.GROUP_INTENT)
    }
}
