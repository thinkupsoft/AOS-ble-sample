package com.thinkup.connectivity.messges.status

import com.thinkup.connectivity.messges.OpCodes
import dalvik.bytecode.Opcodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.SecureUtils
import java.nio.ByteBuffer

class NodeGetMessageStatus(accessMessage: AccessMessage, modelIdentifier: Int) :
    VendorModelMessageStatus(accessMessage, modelIdentifier) {

    // total = 23 bytes
    var systemStat: Int = 0 // 1 byte
    var ledStat: Int = 0 // 1 byte
    var nodeId: Int = 0 // 4 bytes
    var timeout: Int = 0 // bytes
    var flow: Int = 0 // 1 byte
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
        return OpCodes.NT_OPCODE_GET
    }

    override fun parseStatusParameters() {
        super.parseStatusParameters()
        val buffer = ByteBuffer.wrap(parameters)
        systemStat = buffer.get().toInt()
        ledStat = buffer.get().toInt()
        nodeId = buffer.getInt()
        timeout = buffer.getInt()
        flow = buffer.get().toInt()
        shape = buffer.get().toInt()
        r = buffer.get().toInt()
        g = buffer.get().toInt()
        b = buffer.get().toInt()
        dimmer = buffer.get().toInt()
        led = buffer.get().toInt()
        fill = buffer.get().toInt()
        gesture = buffer.get().toInt()
        distance = buffer.get().toInt()
        filter = buffer.get().toInt()
        touch = buffer.get().toInt()
        sound = buffer.get().toInt()
    }

}