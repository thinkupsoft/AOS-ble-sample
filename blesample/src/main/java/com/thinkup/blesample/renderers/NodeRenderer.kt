package com.thinkup.blesample.renderers

import android.view.View
import android.view.ViewGroup
import com.thinkup.blesample.R
import com.thinkup.easycore.ViewRenderer
import kotlinx.android.synthetic.main.item_scan_device.view.*
import no.nordicsemi.android.meshprovisioner.MeshTypeConverters
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import no.nordicsemi.android.meshprovisioner.utils.CompanyIdentifiers
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils

class NodeRenderer(private val callback: Callback) : ViewRenderer<ProvisionedMeshNode, View>(ProvisionedMeshNode::class) {
    override fun bind(view: View, model: ProvisionedMeshNode, position: Int) {
        view.deviceName.text = "Node ${model.nodeName}"
        view.deviceAddress.text = MeshAddress.formatAddress(model.unicastAddress, true)
        view.deviceConnect.text = "Delete"
        view.deviceConnect.setOnClickListener { callback.onDelete(model) }
        view.setOnClickListener { callback.onClick(model) }
    }

    override fun create(parent: ViewGroup): View = inflate(R.layout.item_scan_device, parent, false)

    interface Callback {
        fun onDelete(node: ProvisionedMeshNode)
        fun onClick(node: ProvisionedMeshNode)
    }
}