package com.x930073498.compoacture.component

import androidx.lifecycle.LiveData
import kotlin.reflect.KProperty1

interface CurrentViewModelTypeInstance<T : IStoreViewModel> {
    val currentInstance: T
        get() {
            return this as T
        }

    fun <V> KProperty1<T, V>.asLiveData(): LiveData<V> {
        return currentInstance.asLiveData(this)
    }
}




