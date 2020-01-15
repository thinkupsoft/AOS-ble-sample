package com.thinkup.connectivity.messges.peripheral

import com.thinkup.connectivity.messges.OpCodes
import no.nordicsemi.android.meshprovisioner.transport.AccessMessage
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodePeripheralMessageStatus(accessMessage: AccessMessage, modelIdentifier: Int) :
    VendorModelMessageStatus(accessMessage, modelIdentifier) {

//    // 12 bytes
//    var shapeConf: Int = 0
//    var red: Int = 0
//    var green: Int = 0
//    var blue: Int = 0
//    var dimmerConf: Int = 0
//    var ledConf: Int = 0
//    var fillConf: Int = 0
//    var gestureConf: Int = 0
//    var distanceConf: Int = 0
//    var filterConf: Int = 0
//    var touchConf: Int = 0
//    var soundConf: Int = 0

    // 3 bytes
    var systemStat: Int = 0
    var ledStat: Int = 0
    var batteryLevel: Int = 0

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_PERIFERAL_STATUS
    }

    override fun parseStatusParameters() {
//        super.parseStatusParameters()
//        val buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN)
//
//        shapeConf = buffer.get(0).toInt()
//        red = buffer.get(1).toInt()
//        green = buffer.get(2).toInt()
//        blue = buffer.get(3).toInt()
//        dimmerConf = buffer.get(4).toInt()
//        ledConf = buffer.get(5).toInt()
//        fillConf = buffer.get(6).toInt()
//        gestureConf = buffer.get(7).toInt()
//        distanceConf = buffer.get(8).toInt()
//        filterConf = buffer.get(9).toInt()
//        touchConf = buffer.get(10).toInt()
//        soundConf = buffer.get(11).toInt()

        super.parseStatusParameters()
        val buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN)

        systemStat = buffer.get(0).toInt()
        ledStat = buffer.get(1).toInt()
        batteryLevel = buffer.get(2).toInt()
    }

    override fun toString(): String {
//        return "SHAPE_CONF =  ${shapeConf}, " +
//                "RED =  ${red}, " +
//                "GREEN =  ${green}, " +
//                "BLUE =  ${blue}, " +
//                "DIMMER_CONF =  ${dimmerConf}, " +
//                "LED_CONF =  ${ledConf}, " +
//                "FILL_CONF =  ${fillConf}, " +
//                "GESTURE_CONF =  ${gestureConf}, " +
//                "DISTANCE_CONF =  ${distanceConf}, " +
//                "FILTER_CONF =  ${filterConf}, " +
//                "TOUCH_CONF =  ${touchConf}, " +
//                "SOUND_CONF = ${soundConf}"
        return "SYSTEM_STAT = ${systemStat}, LED_STAT = ${ledStat}, BATTERY= ${batteryLevel}%"
    }

}