package com.thinkup.connectivity.utils

class EventObserver<T> {

    private var callback: Callback<T>? = null

    fun setObserver(callback: Callback<T>) {
        this.callback = callback
    }

    fun removeObserver() {
        this.callback = null
    }

    @Synchronized
    fun postValue(value: T?) = callback?.onPost(value)

    interface Callback<T> {
        fun onPost(e: T?)
    }
}