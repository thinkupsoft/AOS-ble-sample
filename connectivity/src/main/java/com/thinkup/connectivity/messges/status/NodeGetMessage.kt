package com.thinkup.connectivity.messges.status

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked

class NodeGetMessage(appKey: ApplicationKey, modelId: Int, compId: Int, params: ByteArray = byteArrayOf()) :
    VendorModelMessageAcked(appKey, modelId, compId, OpCodes.NT_OPCODE_GET, params)