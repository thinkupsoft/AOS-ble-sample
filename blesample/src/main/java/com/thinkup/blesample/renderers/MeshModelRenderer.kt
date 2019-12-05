package com.thinkup.blesample.renderers

import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import com.thinkup.blesample.R
import com.thinkup.easycore.ViewRenderer
import kotlinx.android.synthetic.main.item_mesh_model.view.*
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.models.VendorModel

class MeshModelRenderer(private val group: Group, private val callback: Callback) : ViewRenderer<VendorModel, View>(VendorModel::class) {
    override fun bind(view: View, model: VendorModel, position: Int) {
        view.modelName.text = model.modelName
        view.setOnClickListener {
            var alert: AlertDialog? = null
            alert = AlertDialog.Builder(view.context)
                .setMessage("Delete from group?")
                .setNegativeButton("Cancel") { p0, p1 -> alert?.dismiss() }
                .setPositiveButton("Ok") { p0, p1 -> callback.onDelete(group, model) }
                .show()
        }
    }

    override fun create(parent: ViewGroup): View = inflate(R.layout.item_mesh_model, parent, false)

    interface Callback {
        fun onDelete(group:Group, item: VendorModel)
    }
}