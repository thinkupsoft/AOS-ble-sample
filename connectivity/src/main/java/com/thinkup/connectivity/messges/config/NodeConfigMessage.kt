package com.thinkup.connectivity.messges.config

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodeConfigMessage(
    val id: Int, val timeoutconfig: Int, val timeoutValue: Int, val flow: Int,
    appKey: ApplicationKey, modelId: Int, compId: Int, params: ByteArray = byteArrayOf()
) : VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_SET_CONFIG, params) {

    init {
        assembleMessageParameters()
    }

    override fun assembleMessageParameters() {
        super.assembleMessageParameters()
        val buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(id and 0x00000000)
        buffer.put(timeoutconfig.toByte())
        buffer.putInt(timeoutValue or 0x00000000)
        buffer.put(flow.toByte())
        mParameters = buffer.array()
    }
}