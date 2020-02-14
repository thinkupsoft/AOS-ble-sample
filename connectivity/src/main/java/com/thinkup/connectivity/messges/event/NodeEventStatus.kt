package com.thinkup.connectivity.messges.event

import com.thinkup.connectivity.messges.EventType
import com.thinkup.connectivity.messges.OpCodes
import com.thinkup.connectivity.utils.ComparableEvent
import no.nordicsemi.android.meshprovisioner.transport.AccessMessage
import no.nordicsemi.android.meshprovisioner.transport.GenericStatusMessage
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.abs

class NodeEventStatus(accessMessage: AccessMessage) : GenericStatusMessage(accessMessage), ComparableEvent.ComparableLiveData {

    constructor(eventType: EventType, value: Int = 0): this(AccessMessage()) {
        this.eventType = eventType
        this.value = value
    }

    init {
        mParameters = accessMessage.parameters
        parseStatusParameters()
    }

    // 5 bytes
    var eventType: EventType? = null
    var value: Int = 0 // TIME_SPENT (ms) or TIME_SPENT (ms)
    var id: Int = 0
    var timestamp: Long = 0 // Mark to filter duplicated events

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_EVENT
    }

    override fun parseStatusParameters() {
        if (mParameters == null) return
        val buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN)

        val type = buffer.get(0)
        eventType = EventType.getType(MeshParserUtils.unsignedByteToInt(type))
        value = buffer.getShort(1).toInt()
        id = buffer.get(3).toInt()
        timestamp = Calendar.getInstance().timeInMillis
    }

    override fun getKey(): Int {
        return srcAddress
    }

    override fun equals(other: Any?): Boolean {
        val comparable = other as NodeEventStatus?
        comparable?.let {
            return it.id == id
        } ?: run { return false }
    }

    override fun toString(): String {
        return "SRC=${MeshAddress.formatAddress(srcAddress, true)}, TYPE=${eventType}, VALUE=${value}, TIME=${timestamp}"
    }
}