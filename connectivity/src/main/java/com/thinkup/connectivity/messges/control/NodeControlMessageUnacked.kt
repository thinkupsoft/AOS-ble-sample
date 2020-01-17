package com.thinkup.connectivity.messges.control

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodeControlMessageUnacked(private val action: Int, private val timeout: Int, appKey: ApplicationKey, modelId: Int, compId: Int, params: ByteArray = byteArrayOf()) :
    VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_CTRL_UNACKNOWLEDGED, params) {

    init {
        assembleMessageParameters()
    }

    override fun assembleMessageParameters() {
        super.assembleMessageParameters()
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(action.toByte())
        buffer.putShort(timeout.toShort())
        buffer.put(OpCodes.getTransactionId())
        mParameters = buffer.array()
    }
}