package com.x930073498.compoacture.component

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

interface LifecycleOwnerProvider:LifecycleOwner {

    val lifecycleOwnerLiveData: LiveData<LifecycleOwner>

    val componentLifecycleOwner: LifecycleOwner

}

val LifecycleOwnerProvider.lifecycleOwner: LifecycleOwner
    get() {
        val value=lifecycleOwnerLiveData.value
        require(value!=null){
            "view lifecycle not init"
        }
        return value
    }