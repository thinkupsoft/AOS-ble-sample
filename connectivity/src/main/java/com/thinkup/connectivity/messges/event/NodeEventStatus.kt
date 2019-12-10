package com.thinkup.connectivity.messges.event

import com.thinkup.connectivity.messges.EventType
import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.transport.AccessMessage
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodeEventStatus(accessMessage: AccessMessage, modelIdentifier: Int) : VendorModelMessageStatus(accessMessage, modelIdentifier) {

    var eventType: EventType? = null
    var value: Int = 0 // TIME_SPENT (ms) or TIME_SPENT (ms)

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_EVENT
    }

    override fun parseStatusParameters() {
        super.parseStatusParameters()
        val buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN)

        val type = buffer.getInt(0)
        eventType = EventType.getType(type)
        value = buffer.getInt(1)
    }
}