package com.thinkup.connectivity.common

data class SimpleStep(
    val total: Int,
    var actual: Int = 0
) {
    fun isCompleted() = total == actual
}