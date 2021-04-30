package com.x930073498.compoacture.internal

import androidx.lifecycle.MutableLiveData

internal class ObservableLiveData<T> : MutableLiveData<T> {
    private val action: T.() -> Unit

    constructor(t: T, action: T.() -> Unit) : super(t) {
        this.action = action
    }

    constructor(action: T.() -> Unit) : super() {
        this.action = action
    }

    override fun setValue(value: T) {
        action(value)
        super.setValue(value)
    }


}