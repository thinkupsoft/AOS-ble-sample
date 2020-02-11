package com.thinkup.connectivity.utils

class ComparableSingleEvent<T> {

    private var currentValue: T? = null
    private var callback: Callback<T>? = null

    fun setObserver(callback: Callback<T>) {
        this.callback = callback
    }

    fun removeObserver() {
        this.callback = null
    }

    fun postValue(value: T?) {
        if (compare(value)) {
            currentValue = value
            callback?.onPost(value)
        }
    }

    private fun compare(newValue: T?): Boolean = newValue != null && newValue != currentValue

    interface Callback<T> {
        fun onPost(e: T?)
    }
}