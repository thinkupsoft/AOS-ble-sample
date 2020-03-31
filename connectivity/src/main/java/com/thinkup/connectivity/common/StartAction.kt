package com.thinkup.connectivity.common

import com.thinkup.connectivity.messges.BASIC_MASK

data class StartAction(var ids: String = BASIC_MASK, val timeout: Int, var count: Int = 1){
    var eventsReceived = 0
}
