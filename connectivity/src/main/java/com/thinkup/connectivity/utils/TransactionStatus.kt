package com.thinkup.connectivity.utils

data class TransactionStatus(
    private var elementAddress: Int = 0,
    private var incompleteTimerExpired: Boolean = false
)