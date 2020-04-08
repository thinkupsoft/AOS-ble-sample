package com.thinkup.connectivity.messges.setup

import com.thinkup.connectivity.messges.DYNAMIC_MASK
import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodeTrainSetupMessage(
    val dimmer: Int, val gesture: Int, val distance: Int, val sound: Int,val nodeQty: Int,
    val steps: List<TrainSetup>,
    appKey: ApplicationKey,
    modelId: Int,
    compId: Int,
    val destination: String? = DYNAMIC_MASK
) : VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_SETUP_TRAIN) {
    var received : Boolean = false

    companion object {
        const val FIXED_SIZE = 6
        const val STEP_SIZE = 3
    }

    init {
        mParameters = byteArrayOf()
        assembleMessageParameters()
    }

    fun incrementTId() {
        mParameters = byteArrayOf()
        assembleMessageParameters()
    }

    private fun getTotalSize() = (STEP_SIZE * steps.size) + FIXED_SIZE

    override fun assembleMessageParameters() {
        super.assembleMessageParameters()

        val buffer = ByteBuffer.allocate(getTotalSize()).order(ByteOrder.LITTLE_ENDIAN)
        val dest = destination ?: DYNAMIC_MASK
        val info1 = dest.substring(0, 8)
        val info2 = "${dest.substring(8, 12)}${gesture.toString(2).padStart(2, '0')}${distance.toString(2).padStart(2, '0')}"

        buffer.put(info1.toShort(2).toByte())
        buffer.put(info2.toShort(2).toByte())
        buffer.put(dimmer.toByte())
        buffer.put(sound.toByte())
        buffer.put(OpCodes.getTransactionId())
        buffer.put(nodeQty.toByte())

        steps.forEach {
            buffer.put(it.led.toByte())
            buffer.put(it.shape.toByte())
            buffer.put(it.color.toByte())
        }

        mParameters = buffer.array()
    }

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_SETUP_TRAIN
    }
}