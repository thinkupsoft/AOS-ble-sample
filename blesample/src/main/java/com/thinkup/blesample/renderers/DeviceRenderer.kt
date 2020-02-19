package com.thinkup.blesample.renderers

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isGone
import com.thinkup.blesample.R
import com.thinkup.connectivity.utils.ExtendedBluetoothDevice
import com.thinkup.easycore.ViewRenderer
import kotlinx.android.synthetic.main.item_scan_device.view.*

class DeviceRenderer(private val callback: Callback) :
    ViewRenderer<ExtendedBluetoothDevice, View>(ExtendedBluetoothDevice::class) {
    override fun bind(view: View, model: ExtendedBluetoothDevice, position: Int) {
        view.deviceName.text = model.name
        view.deviceAddress.text = model.getAddress()
        view.deviceConnect.setOnClickListener {
            val textView = (it as TextView)
            textView.isClickable = false
            callback.onConnect(model, textView)
        }
        view.deviceStatus.visibility = View.GONE
    }

    override fun create(parent: ViewGroup): View = inflate(R.layout.item_scan_device, parent, false)

    interface Callback {
        fun onConnect(device: ExtendedBluetoothDevice, textView: TextView)
    }
}