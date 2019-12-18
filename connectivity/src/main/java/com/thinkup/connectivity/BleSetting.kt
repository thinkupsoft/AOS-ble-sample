package com.thinkup.connectivity

interface BleSetting {
    fun set(start: Boolean, keepAlive: Boolean, config: Boolean)
    fun enabledKeepAlive(): Boolean
    fun enabledStartConfig(): Boolean
    fun enabledProvisionConfig(): Boolean
}