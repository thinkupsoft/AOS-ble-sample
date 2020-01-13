package com.thinkup.connectivity.utils

import androidx.lifecycle.MutableLiveData

class ComparableSingleEvent<T> : MutableLiveData<T>() {

    override fun postValue(value: T?) {
        if (compare(value))
            super.postValue(value)
    }

    private fun compare(newValue: T?): Boolean = newValue != null && newValue != value
}