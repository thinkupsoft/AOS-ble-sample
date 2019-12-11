package com.thinkup.connectivity.common

enum class Status(private var text: String) {
    IDLE("Waiting..."),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    DISCOVERING("Discovering services..."),
    RECONNECTING("Reconnecting..."),
    DISCONNECTING("Disconnecting..."),
    INITIALIZING("Initializing..."),
    READY("Ready..."),
    ERROR(""),
    SCANNING("Scanning for provisioned node"),
    NODE_FOUND("Provisioned node found"),
    PROVISIONING_START("Provisioning started..."),
    PROVISIONING_PROGRESS("Provisioning in progress..."),
    PROVISIONING_COMPLETE("Provisioning completed"),
    PROVISIONING_FAILED("Provisioning failed"),
    BINDING_KEY("Binding key..."),
    SET_PUBLISH_ADDRESS("Setting pusblishing address"),
    FULL_CONFIGURED("Configured successfully");

    fun getValue() = text

    companion object {
        fun getError(message: String): Status {
            val status = ERROR
            status.text = message
            return status
        }
    }
}