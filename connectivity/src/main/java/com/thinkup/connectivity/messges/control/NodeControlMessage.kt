package com.thinkup.connectivity.messges.control

import com.thinkup.connectivity.messges.DYNAMIC_MASK
import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class NodeControlMessage(
    private val action: Byte, private val timeout: Int,
    appKey: ApplicationKey, modelId: Int, compId: Int,
    private val destination: String? = DYNAMIC_MASK
) :
    VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_CTRL) {

    init {
        mParameters = byteArrayOf()
        assembleMessageParameters()
    }

    override fun assembleMessageParameters() {
        super.assembleMessageParameters()
        val buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
        val dest = destination ?: DYNAMIC_MASK
        val info1 = dest.substring(0,8)
        val info2 = "${dest.substring(8,12)}${action.toString(2).padStart(4, '0')}"
        buffer.put(info1.toShort(2).toByte())
        buffer.put(info2.toShort(2).toByte())
        buffer.putShort(timeout.toShort())
        buffer.put(OpCodes.getTransactionId())
        mParameters = buffer.array()
    }
}