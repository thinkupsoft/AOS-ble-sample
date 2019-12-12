package com.thinkup.connectivity.utils

import android.os.Handler
import androidx.lifecycle.MutableLiveData

class TimeoutLiveData<T>(private val timeout: Long, private val default: T, action: (() -> Unit)? = null) : MutableLiveData<T>() {

    private val handler: Handler = Handler()
    private val runnable = Runnable {
        postValue(default)
        action?.invoke()
    }

    init {
        handler.postDelayed(runnable, timeout)
    }

    override fun setValue(value: T) {
        handler.removeCallbacks(runnable)
        super.setValue(value)
    }

    override fun postValue(value: T) {
        handler.removeCallbacks(runnable)
        super.postValue(value)
    }

}