package com.thinkup.connectivity.provisioning

enum class Status(val priority: Int, private var text: String) {
    TIMEOUT(-1, "Timeout!"),
    ERROR(0, "Error!"),
    CONNECTING(1, "Connecting..."),
    READY(2, "Ready..."),
    DISCOVERING(3, "Discovering services..."),
    INITIALIZING(4, "Initializing..."),
    NODE_FOUND(5, "Provisioned node found"),
    PROVISIONING_FAILED(6, "Provisioning failed"),
    PROVISIONING_PROGRESS(7, "Provisioning in progress..."),
    SETTING(8, "Setting data..."),
    BINDING_APP_KEY(9, "Binding app key..."),
    SET_PUBLISH_ADDRESS(10, "Setting publishing address"),
    FULL_CONFIGURED(11, "Configured successfully");

    fun getValue() = text

    companion object {
        fun getError(message: String): Status {
            val status = ERROR
            status.text = message
            return status
        }
    }
}