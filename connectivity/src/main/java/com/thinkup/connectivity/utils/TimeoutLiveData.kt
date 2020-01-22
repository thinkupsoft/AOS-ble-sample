package com.thinkup.connectivity.utils

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData

class TimeoutLiveData<T>(
    private val timeout: Long, private val default: T,
    private val control: T? = null, action: (() -> Unit)? = null
) : MutableLiveData<T>() {

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        postValue(default)
        action?.invoke()
    }

    init {
        handler.postDelayed(runnable, timeout)
    }

    override fun setValue(value: T) {
        if (control == null || control == value) handler.removeCallbacks(runnable)
        super.setValue(value)
    }

    override fun postValue(value: T) {
        if (control == null || control == value) handler.removeCallbacks(runnable)
        super.postValue(value)
    }

}