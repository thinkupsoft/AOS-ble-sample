package com.thinkup.connectivity.messges.config

import com.thinkup.connectivity.messges.OpCodes
import dalvik.bytecode.Opcodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.SecureUtils
import java.nio.ByteBuffer

class NodeConfigMessageStatus(accessMessage: AccessMessage, modelIdentifier: Int) :
    VendorModelMessageStatus(accessMessage, modelIdentifier) {

    // total = 9 bytes
    var nodeId: Int = 0 // 4 bytes
    var timeout: Int = 0 // 4 bytes
    var flow: Int = 0 // 1 byte

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_CONFIG_STATUS
    }

    override fun parseStatusParameters() {
        super.parseStatusParameters()
        val buffer = ByteBuffer.wrap(accessPayload)

        nodeId = buffer.getInt()
        timeout = buffer.getInt()
        flow = buffer.get().toInt()
    }

}