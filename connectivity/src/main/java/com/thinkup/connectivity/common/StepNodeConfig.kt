package com.thinkup.connectivity.common

data class StepNodeConfig(
    val nodeIndex: Int,
    val shapes: List<Int>,
    val color: Int,
    val led: Int,
    var sended: Boolean = false,
    var stepIndex: Int = 0,
    var timeout: Int = 0,
    var delay: Long = 0
)