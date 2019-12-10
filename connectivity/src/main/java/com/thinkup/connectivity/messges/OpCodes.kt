package com.thinkup.connectivity.messges

object OpCodes {
    var TRANSACTION_ID = 0x00

    // client
    const val NT_OPCODE_CTRL = 0xE0
    const val NT_OPCODE_CTRL_UNACKNOWLEDGED = 0xE1
    const val NT_OPCODE_SET_CONFIG = 0xF0
    const val NT_OPCODE_SET_CONFIG_UNACKNOWLEDGED = 0xF1
    const val NT_OPCODE_SET_PERIFERAL = 0xF2
    const val NT_OPCODE_SET_PERIFERAL_UNACKNOWLEDGED = 0xF3
    const val NT_OPCODE_GET = 0xc0

    // server-nodes
    const val NT_OPCODE_EVENT = 0xD0
    const val NT_OPCODE_SYSTEM_STATUS = 0xD1
    const val NT_OPCODE_CONFIG_STATUS = 0xD2
    const val NT_OPCODE_PERIFERAL_STATUS = 0xD3
    const val NT_OPCODE_GENERAL_STATUS = 0xD4

    fun getOpCode(opCode: Int) = opCode.toShort()

    fun getTransactionId(): Byte {
        val getter = TRANSACTION_ID
        TRANSACTION_ID++
        return getter.toByte()
    }
}