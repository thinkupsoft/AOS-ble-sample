package com.thinkup.connectivity.messges.status

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.transport.AccessMessage
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodeGetMessageStatus(accessMessage: AccessMessage, modelIdentifier: Int) :
    VendorModelMessageStatus(accessMessage, modelIdentifier) {

    // total = 24 bytes
    var fwVersion: Int = 0
    var battery: Int = 0
    var systemStat: Int = 0 // 1 byte
    var ledStat: Int = 0 // 1 byte
    var nodeId: Int = 0 // 4 bytes
    var timeout: Int = 0 // bytes
    var shape: Int = 0 // 1 byte
    var r: Int = 0 // 1 byte
    var g: Int = 0 // 1 byte
    var b: Int = 0 // 1 byte
    var dimmer: Int = 0 // 1 byte
    var led: Int = 0 // 1 byte
    var fill: Int = 0 // 1 byte
    var gesture: Int = 0 // 1 byte
    var distance: Int = 0 // 1 byte
    var filter: Int = 0 // 1 byte
    var touch: Int = 0 // 1 byte
    var sound: Int = 0 // 1 byte

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_GENERAL_STATUS
    }

    override fun parseStatusParameters() {
        super.parseStatusParameters()
        val buffer = ByteBuffer.wrap(parameters).order(ByteOrder.LITTLE_ENDIAN)
        fwVersion = buffer.get(0).toInt()
        battery = buffer.get(1).toInt()
        systemStat = buffer.get(2).toInt()
        ledStat = buffer.get(3).toInt()
        nodeId = buffer.getInt(4)
        timeout = buffer.getInt(8)
        shape = buffer.get(12).toInt()
        r = buffer.get(13).toInt()
        g = buffer.get(14).toInt()
        b = buffer.get(15).toInt()
        dimmer = buffer.get(16).toInt()
        led = buffer.get(17).toInt()
        fill = buffer.get(18).toInt()
        gesture = buffer.get(19).toInt()
        distance = buffer.get(20).toInt()
        filter = buffer.get(21).toInt()
        touch = buffer.get(22).toInt()
        sound = buffer.get(23).toInt()
    }

    override fun toString(): String {
        return "FWVERSION = ${fwVersion}," +
                "BATTERY = ${battery}," +
                "SYSTEMSTAT = ${systemStat}," +
                "LEDSTAT = ${ledStat}," +
                "NODEID =  ${nodeId}," +
                "TIMEOUT = ${timeout}," +
                "SHAPE = ${shape}," +
                "RED = ${r}," +
                "GREEN = ${g}," +
                "BLUE = ${b}," +
                "DIMMER = ${dimmer}," +
                "LED = ${led}," +
                "FILL = ${fill}," +
                "GESTURE = ${gesture}," +
                "DISTANCE = ${distance}," +
                "FILTER =  ${filter}," +
                "TOUCH = ${touch}," +
                "SOUND = ${sound}"
    }
}