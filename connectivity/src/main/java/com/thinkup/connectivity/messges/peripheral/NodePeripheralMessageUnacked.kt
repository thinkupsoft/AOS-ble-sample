package com.thinkup.connectivity.messges.peripheral

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodePeripheralMessageUnacked(
    val shape: Int, val color: Int, val red: Int = 0, val green: Int = 0, val blue: Int = 0, val dimmer: Int, val led: Int,
    val fill: Int, val gesture: Int, val distance: Int, val filter: Int, val touch: Int, val sound: Int,
    appKey: ApplicationKey,
    modelId: Int,
    compId: Int,
    params: ByteArray = byteArrayOf()
) :
    VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_SET_PERIFERAL_UNACKNOWLEDGED, params) {
    init {
        assembleMessageParameters()
    }

    override fun assembleMessageParameters() {
        super.assembleMessageParameters()
        val buffer = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(shape.toByte())
        buffer.put(color.toByte())
        buffer.put(red.toByte())
        buffer.put(green.toByte())
        buffer.put(blue.toByte())
        buffer.put(dimmer.toByte())
        buffer.put(led.toByte())
        buffer.put(fill.toByte())
        buffer.put(gesture.toByte())
        buffer.put(distance.toByte())
        buffer.put(filter.toByte())
        buffer.put(touch.toByte())
        buffer.put(sound.toByte())
        buffer.put(OpCodes.getTransactionId())
        mParameters = buffer.array()
    }
}