package com.thinkup.connectivity.messges.config

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodeConfigMessageUnacked(
    val id: Int, appKey: ApplicationKey, modelId: Int, compId: Int
) : VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_SET_CONFIG_UNACKNOWLEDGED) {

    init {
        mParameters = byteArrayOf()
        assembleMessageParameters()
    }

    override fun assembleMessageParameters() {
        super.assembleMessageParameters()
        val buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(id or 0x00000000)
        buffer.put(OpCodes.getTransactionId())
        mParameters = buffer.array()
    }
}