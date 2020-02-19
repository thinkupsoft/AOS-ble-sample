package com.thinkup.connectivity.messges

object OpCodes {
    var TRANSACTION_ID = 0x00

    // client
    const val NT_OPCODE_CTRL = 0xE0
    const val NT_OPCODE_CTRL_UNACKNOWLEDGED = 0xE1
    const val NT_OPCODE_SET_CONFIG = 0xF0
    const val NT_OPCODE_SET_CONFIG_UNACKNOWLEDGED = 0xF1
    const val NT_OPCODE_SET_PERIPHERAL_PRE = 0xF2
    const val NT_OPCODE_SET_PERIPHERAL_PRE_UNACKNOWLEDGED = 0xF3
    const val NT_OPCODE_SET_PERIPHERAL_TRAIN = 0xF4
    const val NT_OPCODE_SET_PERIPHERAL_TRAIN_UNACKNOWLEDGED = 0xF5
    const val NT_OPCODE_GET = 0xC0

    // server-nodes
    const val NT_OPCODE_EVENT = 0xD0
    const val NT_OPCODE_SYSTEM_STATUS = 0xD1
    const val NT_OPCODE_CONFIG_STATUS = 0xD2
    const val NT_OPCODE_PERIPHERAL_STATUS = 0xD3
    const val NT_OPCODE_GENERAL_STATUS = 0xD4

    fun getOpCode(opCode: Int) = opCode.toShort()

    fun getTransactionId(): Byte {
        val getter = TRANSACTION_ID
        TRANSACTION_ID++
        return getter.toByte()
    }

    fun getUnicastMask(index: Int): String {
        var mask = BASIC_MASK
        mask = mask.replaceRange(index, index + 1, "1")
        return mask
    }

    fun getGroupMask(index: List<Int>): String {
        var mask = BASIC_MASK
        index.forEach { i ->
            mask = mask.replaceRange(i, i + 1, "1")
        }
        return mask
    }
}