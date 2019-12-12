package com.thinkup.connectivity.utils

class ComparableSingleLiveData<T> : SingleLiveEvent<T>() {
    override fun postValue(value: T) {
        if (!compare(value))
            super.postValue(value)
    }

    private fun compare(value: T): Boolean {
        return (this.value != null && this.value == value)
    }
}