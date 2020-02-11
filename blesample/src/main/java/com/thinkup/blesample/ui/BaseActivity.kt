package com.thinkup.blesample.ui

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.thinkup.blesample.R
import com.thinkup.connectivity.BleSession
import kotlinx.android.synthetic.main.toolbar.*
import org.koin.android.ext.android.inject

abstract class BaseActivity : AppCompatActivity() {

    val SCANNER_REQUEST = 100
    val GROUPS_REQUEST = 101
    val DETAIL_REQUEST = 102
    val TRAINING_REQUEST = 103
    val SCHEDULED_REQUEST = 104
    val bleSession: BleSession by inject()

    private fun loadToolbar() {

        connectionStatus.setOnClickListener {
            if (bleSession.isConnected().value == true) bleSession.disconnect()
            else bleSession.autoConnect()
        }
        bleSession.isConnected().observe(this, Observer {
            connectionStatus.contentDescription = if (bleSession.isConnected().value == true) "Connected" else "Disconnected"
            connectionStatus.setImageResource(
                if (bleSession.isConnected().value == true) {
                    R.drawable.ic_bluetooth_connected_black
                } else {
                    bleSession.autoConnect()
                    R.drawable.ic_bluetooth_disabled_black
                }
            )
        })
        bleSession.getBatteryAverage().observe(this, Observer {
            battery.text = if (it == null || it == -1) "N/A" else "$it%"
            battery.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                when (it) {
                    in 0..25 -> R.drawable.ic_battery_low
                    in 26..75 -> R.drawable.ic_battery_middle
                    in 76..100 -> R.drawable.ic_battery_full
                    else -> R.drawable.ic_battery_unknown
                }, 0
            )
        })
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        loadToolbar()
        toolbar.title = title()
        settings()
    }

    private fun settings() {
        bleSession.settings()
        settingsAction.setOnClickListener {
            val checkboxLayout: View = layoutInflater.inflate(R.layout.item_settings, null)
            (checkboxLayout.findViewById(R.id.startSetting) as CheckBox).isChecked = bleSession.settings().enabledStartConfig()
            (checkboxLayout.findViewById(R.id.keepSetting) as CheckBox).isChecked = bleSession.settings().enabledKeepAlive()
            (checkboxLayout.findViewById(R.id.configSetting) as CheckBox).isChecked = bleSession.settings().enabledProvisionConfig()
            AlertDialog.Builder(this)
                .setView(checkboxLayout)
                .setTitle("Settings")
                .setMessage("Choose your preferences")
                .setPositiveButton("OK") { _, _ ->
                    bleSession.settings().set(
                        (checkboxLayout.findViewById(R.id.startSetting) as CheckBox).isChecked,
                        (checkboxLayout.findViewById(R.id.keepSetting) as CheckBox).isChecked,
                        (checkboxLayout.findViewById(R.id.configSetting) as CheckBox).isChecked
                    )
                }
                .create().show()

        }
    }

    abstract fun title(): String
}