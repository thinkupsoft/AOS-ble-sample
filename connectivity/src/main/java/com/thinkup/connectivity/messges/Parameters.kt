package com.thinkup.connectivity.messges

const val NO_CONFIG = 0x00
const val BROADCAST = 0xFFFF
const val DYNAMIC_MASK = "111111111111"
const val BASIC_MASK = "000000000000"

object ControlParams {
    const val TURN_OFF = 0x01
    const val START = 0x02
    const val PAUSE = 0x03
    const val STOP = 0x04
    const val RECALIBRAR = 0x05
    const val SET_LED_ON = 0x06
    const val SET_LED_OFF = 0x07
    const val KEEP_ALIVE = 0x08
    const val ENTER_DFU = 0x09
}

object ShapeParams {
    // SHAPES
    const val TRIANGLE = 0x54
    const val SQUARE = 0x53
    const val CIRCLE = 0x4F
    const val CROSS = 0x4D
    const val X = 0x58
    // NUMBERS
    const val NUMBER_0 = 0x30
    const val NUMBER_1 = 0x31
    const val NUMBER_2 = 0x32
    const val NUMBER_3 = 0x33
    const val NUMBER_4 = 0x34
    const val NUMBER_5 = 0x35
    const val NUMBER_6 = 0x36
    const val NUMBER_7 = 0x37
    const val NUMBER_8 = 0x38
    const val NUMBER_9 = 0x39
    // LETTERS
    const val LETTER_A = 0x41
    const val LETTER_B = 0x42
    const val LETTER_C = 0x43
    const val LETTER_D = 0x44
    // ARROWS
    const val ARROW_RIGTH = 0x52
    const val ARROW_LEFT = 0x4C
    const val ARROW_UP = 0x55
    const val ARROW_DOWN = 0x57
    const val ARROW_NORTHEAST = 0x48
    const val ARROW_NORTHWEST = 0x46
    const val ARROW_SOUTHEAST = 0x47
    const val ARROW_SOUTHWEST = 0x4E
}

object ColorParams {
    // COLORS
    const val COLOR_CUSTOM = 0x01
    const val COLOR_WITHE = 0x42
    const val COLOR_RED = 0x52
    const val COLOR_GREEN = 0x56
    const val COLOR_BLUE = 0x41
    const val COLOR_YELLOW = 0x59
    const val COLOR_CYAN = 0x43
    const val COLOR_MAGENT = 0x4D
}

object PeripheralParams {
    // SOUND
    const val NO_SOUND = 0x30
    const val BIP_START = 0x31
    const val BIP_HIT = 0x32
    const val BIP_START_HIT = 0x33
    // LED CONFIG
    const val LED_PERMANENT = 0x01
    const val LED_FLICKER = 0x02
    const val LED_FLASH = 0x03
    const val LED_FAST_FLASH = 0x04
    // GESTURE
    const val HOVER = 0b01
    const val TOUCH = 0b10
    const val BOTH = 0b11
    // DISTANCE
    const val HIGH = 0b01
    const val MIDDLE = 0b10
    const val LOW = 0b11
    // FILTER
    const val SUN = 0x53
    const val INDOOR = 0x49
    // FILL CONFIG
    const val FILL = 0x46
    const val STROKE = 0x43
}

object SystemParams {
    // system stat
    const val SYS_START = 0xA0
    const val SYS_PAUSE = 0xA1
    const val SYS_STOP = 0xA2
    // led stat
    const val LED_START = 0xC0
    const val LED_PAUSE = 0xC1
}

object StatusParams {
    const val HIT = 0xA0
    const val TIMEOUT = 0xA1
    const val LOW_BAT = 0xB0
}

object DimmerParams {
    const val LOW = 0x05
    const val MEDIUM = 0x32
    const val HIGH = 0x64
}

enum class EventType(val value: Int, val text: String) {
    HELLO(0x01, "HELLO"),
    HIT(0xa0, "HIT"),
    TIMEOUT(0xa1, "TIMEOUT"),
    LOW_BAT(0xb0, "LOW_BATTERY"),
    FAKE(0x00, "FAKE");

    companion object {
        fun getType(value: Int): EventType {
            for (v in values()) {
                if (v.value == value) return v
            }
            return TIMEOUT
        }
    }
}