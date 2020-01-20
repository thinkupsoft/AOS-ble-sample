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
    val distance: Int = 1, // 0- Bajo, 1- Medio, 2- Alto
    val dimmer: Int = 1, // 0- Bajo, 1- Medio, 2- Alto
    val sound: Boolean = false,
    val endWithLight: Boolean = false
)