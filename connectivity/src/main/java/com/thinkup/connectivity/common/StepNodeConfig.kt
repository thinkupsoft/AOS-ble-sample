package com.thinkup.connectivity.common

class StepNodeConfig(val nodeIndex: Int, val shapes: List<Int>, val color: Int, val led: Int) {
    var sended: Boolean = false
    var stepIndex: Int = 0
}