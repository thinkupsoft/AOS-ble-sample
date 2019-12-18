package com.thinkup.blesample

import android.content.Context
import com.thinkup.connectivity.BleSetting

class BleSettingImpl(private val context: Context) : BleSetting {

    private var enabledStart: Boolean
    private var enabledKeepAlive: Boolean
    private var enabledConfig: Boolean

    companion object {
        val PREFERENCES = "ble_sample_pref"
        val START = "enabled_start"
        val KEEP_ALIVE = "enabled_keep_alive"
        val PRO_CONFIG = "enabled_pro_config"
    }

    init {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        enabledStart = preferences.getBoolean(START, true)
        enabledKeepAlive = preferences.getBoolean(KEEP_ALIVE, true)
        enabledConfig = preferences.getBoolean(PRO_CONFIG, true)
    }

    override fun set(start: Boolean, keepAlive: Boolean, config: Boolean) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        preferences.edit().apply {
            putBoolean(START, start)
            putBoolean(KEEP_ALIVE, keepAlive)
            putBoolean(PRO_CONFIG, config)
        }.apply()
        enabledStart = start
        enabledKeepAlive = keepAlive
        enabledConfig = config
    }

    override fun enabledKeepAlive() = enabledKeepAlive

    override fun enabledStartConfig() = enabledStart

    override fun enabledProvisionConfig() = enabledConfig
}