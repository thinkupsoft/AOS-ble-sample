package com.thinkup.connectivity.common

import com.thinkup.connectivity.messges.PeripheralParams

data class FastOptions(
    val touches: Int = 0,
    val timeout: Int = 0,
    val delay: Int = 0,
    val shapes: List<Int> = listOf(),
    val colors: List<Int> = listOf(),
    val startWithCountdown: Boolean = false,
    val flashMode: Int = PeripheralParams.LED_PERMANENT,
    val sound: Boolean = false,
    val endWithLight: Boolean = false
)