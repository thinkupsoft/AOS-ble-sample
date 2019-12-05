package com.thinkup.blesample.renderers

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.easycore.ViewRenderer
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.item_group.view.*
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.MeshModel

class GroupRenderer(private val callback: Callback) : ViewRenderer<Group, View>(Group::class), MeshModelRenderer.Callback {
    override fun create(parent: ViewGroup): View = inflate(R.layout.item_group, parent, false)

    override fun bind(view: View, model: Group, position: Int) {
        val adapter = RendererAdapter()
        val models = callback.getModels(model)
        view.groupName.text = model.name
        view.groupAddress.text = "Address: ${model.address.toString()}"
        view.groupModels.text = "Nodes: ${models.size}"
        view.groupDelete.setOnClickListener { callback.onDelete(model) }
        view.groupNodes.layoutManager = LinearLayoutManager(view.context)
        adapter.addRenderer(MeshModelRenderer(model, this))
        view.groupNodes.adapter = adapter
        adapter.setItems(models)
        view.groupSubscribe.setOnClickListener { callback.onSubscribe(model) }
        view.groupStatus.setOnClickListener { callback.onGetStatus(model, models) }
        view.groupTtl.setOnClickListener { callback.onGetTtl(model) }
    }

    interface Callback {
        fun onDelete(item: Group)
        fun onDeleteItem(item: Group, model: VendorModel)
        fun onSubscribe(item: Group)
        fun onGetTtl(item: Group)
        fun onGetStatus(item: Group, models: List<MeshModel>)
        fun getModels(item: Group): List<MeshModel>
    }

    override fun onDelete(group: Group, item: VendorModel) {
        callback.onDeleteItem(group, item)
    }
}
