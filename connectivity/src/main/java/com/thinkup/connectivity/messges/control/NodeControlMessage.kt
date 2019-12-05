package com.thinkup.connectivity.messges.control

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils

class NodeControlMessage(private val action: Int, appKey: ApplicationKey, modelId: Int, compId: Int, params: ByteArray = byteArrayOf()) :
    VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_CTRL, params) {

    init {
        assembleMessageParameters()
    }

    override fun assembleMessageParameters() {
        super.assembleMessageParameters()
        mParameters = byteArrayOf(action.toByte())
    }
}