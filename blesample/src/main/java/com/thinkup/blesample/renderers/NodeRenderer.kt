package com.thinkup.blesample.renderers

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.thinkup.blesample.R
import com.thinkup.easycore.ViewRenderer
import kotlinx.android.synthetic.main.item_scan_device.view.*
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress

class NodeRenderer (private val callback: Callback) : ViewRenderer<ProvisionedMeshNode, View>(ProvisionedMeshNode::class) {
    override fun bind(view: View, model: ProvisionedMeshNode, position: Int) {
        view.deviceName.text = "Node ${model.nodeName}"
        view.deviceAddress.text = MeshAddress.formatAddress(model.unicastAddress, true)
        view.deviceConnect.text = "Delete"
        view.deviceConnect.setOnClickListener { callback.onDelete(model) }
        view.deviceConnect.setOnLongClickListener { callback.onForceDelete(model); true }
        view.setOnClickListener { callback.onClick(model) }
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

    override fun create(parent: ViewGroup): View = inflate(R.layout.item_scan_device, parent, false)

    interface Callback {
        fun onDelete(node: ProvisionedMeshNode)
        fun onForceDelete(node: ProvisionedMeshNode)
        fun onClick(node: ProvisionedMeshNode)
    }
}