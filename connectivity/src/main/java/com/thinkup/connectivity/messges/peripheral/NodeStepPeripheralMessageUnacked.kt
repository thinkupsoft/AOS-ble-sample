package com.thinkup.connectivity.messges.peripheral

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodeStepPeripheralMessageUnacked(
    val shape: Int, val color: Int, val led: Int,
    appKey: ApplicationKey,
    modelId: Int,
    compId: Int,
    params: ByteArray = byteArrayOf()
) :
    VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_SET_PERIPHERAL_PRE_UNACKNOWLEDGED, params) {
    init {
        assembleMessageParameters()
    }

    override fun assembleMessageParameters() {
        super.assembleMessageParameters()
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(shape.toByte())
        buffer.put(color.toByte())
        buffer.put(led.toByte())
        buffer.put(OpCodes.getTransactionId())
        mParameters = buffer.array()
    }
}