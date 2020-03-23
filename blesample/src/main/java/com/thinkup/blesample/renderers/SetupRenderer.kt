package com.thinkup.blesample.renderers

import android.view.View
import android.view.ViewGroup
import com.thinkup.blesample.R
import com.thinkup.connectivity.messges.setup.TrainSetup
import com.thinkup.easycore.ViewRenderer
import kotlinx.android.synthetic.main.item_setup_event.view.*

class SetupRenderer() : ViewRenderer<TrainSetup, View>(TrainSetup::class) {
    override fun create(parent: ViewGroup): View = inflate(R.layout.item_setup_event, parent, false)

    override fun bind(view: View, model: TrainSetup, position: Int) {
        view.detail.text = "SHAPE=${model.shape}, LED=${model.led}, COLOR=${model.color}"
    }
}