package com.thinkup.connectivity.utils

import java.util.*

object Constants {
    /**
     * Mesh provisioning service UUID
     */
    val MESH_PROVISIONING_UUID = UUID.fromString("00001827-0000-1000-8000-00805F9B34FB")
    /**
     * Mesh provisioning data in characteristic UUID
     */
    val MESH_PROVISIONING_DATA_IN = UUID.fromString("00002ADB-0000-1000-8000-00805F9B34FB")
    /**
     * Mesh provisioning data out characteristic UUID
     */
    val MESH_PROVISIONING_DATA_OUT = UUID.fromString("00002ADC-0000-1000-8000-00805F9B34FB")

    /**
     * Mesh provisioning service UUID
     */
    val MESH_PROXY_UUID = UUID.fromString("00001828-0000-1000-8000-00805F9B34FB")

    /**
     * Mesh provisioning data in characteristic UUID
     */
    val MESH_PROXY_DATA_IN = UUID.fromString("00002ADD-0000-1000-8000-00805F9B34FB")

    /**
     * Mesh provisioning data out characteristic UUID
     */
    val MESH_PROXY_DATA_OUT = UUID.fromString("00002ADE-0000-1000-8000-00805F9B34FB")

    const val MTU_SIZE_MAX = 517
}