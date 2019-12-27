package com.thinkup.connectivity.messges.control

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.transport.AccessMessage
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodeControlMessageStatus(accessMessage: AccessMessage, modelIdentifier: Int) : VendorModelMessageStatus(accessMessage, modelIdentifier) {

    // 3 bytes
    var systemStat: Int = 0
    var ledStat: Int = 0
    var batteryLevel: Int = 0

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_SYSTEM_STATUS
    }

    override fun parseStatusParameters() {
        super.parseStatusParameters()
        val buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN)

        systemStat = buffer.get(0).toInt()
        ledStat = buffer.get(1).toInt()
        batteryLevel = buffer.get(2).toInt()
    }

    override fun toString(): String {
        return "SYSTEM_STAT = ${systemStat}, LED_STAT = ${ledStat}, BATTERY= ${batteryLevel}%"
    }
}