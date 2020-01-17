package com.thinkup.connectivity.messges.peripheral

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.transport.AccessMessage
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodePeripheralMessageStatus(accessMessage: AccessMessage, modelIdentifier: Int) :
    VendorModelMessageStatus(accessMessage, modelIdentifier) {

    // 1 bytes
    var state: Int = 0

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_PERIPHERAL_STATUS
    }

    override fun parseStatusParameters() {
        super.parseStatusParameters()
        val buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN)

        state = buffer.get(0).toInt()
    }

    override fun toString(): String {
        return "STATE = $state"
    }

}