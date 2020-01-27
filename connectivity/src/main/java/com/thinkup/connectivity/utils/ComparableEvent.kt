package com.thinkup.connectivity.utils

class ComparableEvent<T : ComparableEvent.ComparableLiveData> {

    private var eventSet = HashMap<Int, T>()

    fun compare(value: T): Boolean {
        val current = eventSet[value.getKey()]
        current?.let { v ->
            return if (v == value) {
                true
            } else {
                eventSet[value.getKey()] = value
                false
            }
        } ?: run {
            eventSet[value.getKey()] = value
            return false
        }
    }

    fun flush() {
        eventSet = HashMap<Int, T>()
    }

    interface ComparableLiveData {
        fun getKey(): Int
    }
}