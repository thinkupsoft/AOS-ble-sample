package com.thinkup.blesample.renderers

import android.view.View
import android.view.ViewGroup
import com.thinkup.blesample.R
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.easycore.ViewRenderer
import kotlinx.android.synthetic.main.item_node_event.view.*
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import java.text.SimpleDateFormat
import java.util.*

class EventRenderer() : ViewRenderer<NodeEventStatus, View>(NodeEventStatus::class) {
    override fun create(parent: ViewGroup): View = inflate(R.layout.item_node_event, parent, false)

    override fun bind(view: View, model: NodeEventStatus, position: Int) {
        view.address.text = MeshAddress.formatAddress(model.srcAddress, true)
        view.type.text = model.eventType?.text
        view.time.text = model.value.toString()
        val date = Date()
        date.time = model.timestamp
        view.date.text = SimpleDateFormat("mm:ss.SSS", Locale.US).format(date)
    }
}