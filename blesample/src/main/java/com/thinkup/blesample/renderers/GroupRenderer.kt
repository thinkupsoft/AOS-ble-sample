package com.thinkup.blesample.renderers

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.easycore.ViewRenderer
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.item_group.view.*
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class GroupRenderer(private val callback: Callback) : ViewRenderer<Group, View>(Group::class), MeshModelRenderer.Callback {
    override fun create(parent: ViewGroup): View = inflate(R.layout.item_group, parent, false)

    override fun bind(view: View, model: Group, position: Int) {
        val adapter = RendererAdapter()
        val nodes = callback.getModels(model)
        view.groupName.text = model.name
        view.groupAddress.text = "Address: ${model.address.toString()}"
        view.groupModels.text = "Subscriptions: ${nodes.size}"
        view.groupDelete.setOnClickListener { callback.onDelete(model) }
        view.groupNodes.layoutManager = LinearLayoutManager(view.context)
        view.groupNodes.adapter = adapter
        adapter.addRenderer(MeshModelRenderer(model, this))
        adapter.setItems(nodes)
        view.groupSubscribe.setOnClickListener { callback.onSubscribe(model) }
        //view.groupStatus.setOnClickListener { callback.onGetStatus(model, nodes) }
        view.groupMore.setOnClickListener { callback.onMore(model) }
    }

    interface Callback {
        fun onDelete(item: Group)
        fun onDeleteItem(item: Group, meshNode: ProvisionedMeshNode)
        fun onSubscribe(item: Group)
        fun onMore(item: Group)
        fun getModels(item: Group): List<ProvisionedMeshNode>
    }

    override fun onDelete(group: Group, item: ProvisionedMeshNode) {
        callback.onDeleteItem(group, item)
    }
}
