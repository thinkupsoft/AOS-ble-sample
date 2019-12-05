package com.thinkup.connectivity.messges.status

import com.thinkup.connectivity.messges.OpCodes
import dalvik.bytecode.Opcodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.ConfigMessage
import no.nordicsemi.android.meshprovisioner.transport.GenericMessage
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageUnacked
import no.nordicsemi.android.meshprovisioner.utils.SecureUtils

class NodeGetMessage(appKey: ApplicationKey, modelId: Int, compId: Int, params: ByteArray = byteArrayOf()) :
    VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_GET, params) {

}