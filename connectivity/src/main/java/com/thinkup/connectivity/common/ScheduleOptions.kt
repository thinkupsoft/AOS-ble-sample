package com.thinkup.connectivity.common

class ScheduleOptions(
    val steps: List<StepOption>,
    val starterMethod: StarterMethod,
    val randomFirstStep: Boolean,
    val endWithLight: Boolean,
    val distance: Int, // 0- Bajo, 1- Medio, 2- Alto
    val dimmer: Int, // 0- Bajo, 1- Medio, 2- Alto
    val sound: Boolean = false
)