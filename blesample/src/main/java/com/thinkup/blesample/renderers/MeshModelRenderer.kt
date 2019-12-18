package com.thinkup.blesample.renderers

import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.thinkup.blesample.R
import com.thinkup.easycore.ViewRenderer
import kotlinx.android.synthetic.main.item_mesh_model.view.*
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class MeshModelRenderer(private val group: Group, private val callback: Callback) :
    ViewRenderer<ProvisionedMeshNode, View>(ProvisionedMeshNode::class) {
    override fun bind(view: View, model: ProvisionedMeshNode, position: Int) {
        view.modelName.text = " * Node ${model.nodeName}"
        view.setOnClickListener {
            var alert: AlertDialog? = null
            alert = AlertDialog.Builder(view.context)
                .setMessage("Delete from group?")
                .setNegativeButton("Cancel") { p0, p1 -> alert?.dismiss() }
                .setPositiveButton("Ok") { p0, p1 -> callback.onDelete(group, model) }
                .show()
        }
        if (model.isOnline) {
            view.deviceStatus.text = "Online"
            view.deviceStatus.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(view.context, R.drawable.ic_state_online),
                null,
                null,
                null
            )
        } else {
            view.deviceStatus.text = "Offline"
            view.deviceStatus.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(view.context, R.drawable.ic_state_offline),
                null,
                null,
                null
            )
        }
    }

    override fun create(parent: ViewGroup): View = inflate(R.layout.item_mesh_model, parent, false)

    interface Callback {
        fun onDelete(group: Group, item: ProvisionedMeshNode)
    }
}