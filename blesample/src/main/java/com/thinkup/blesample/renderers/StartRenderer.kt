package com.thinkup.blesample.renderers

import android.view.View
import android.view.ViewGroup
import com.thinkup.blesample.R
import com.thinkup.blesample.ui.SetupTrainingActivity
import com.thinkup.easycore.ViewRenderer
import kotlinx.android.synthetic.main.item_setup_event.view.*

class StartRenderer : ViewRenderer<SetupTrainingActivity.StartAction, View>(SetupTrainingActivity.StartAction::class) {
    override fun create(parent: ViewGroup): View = inflate(R.layout.item_setup_event, parent, false)

    override fun bind(view: View, model: SetupTrainingActivity.StartAction, position: Int) {
        view.detail.text = "MASK=${model.ids}, TEIMEOUT=${model.timeout}"
    }
}