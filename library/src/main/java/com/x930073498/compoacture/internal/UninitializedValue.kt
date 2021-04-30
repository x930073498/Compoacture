@file:Suppress("ClassName")

package com.x930073498.compoacture.internal

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import java.io.Serializable

private object UninitializedValue

/**
 * This was copied from SynchronizedLazyImpl but modified to automatically initialize in ON_CREATE.
 */
@SuppressWarnings("Detekt.ClassNaming")
class lifecycleAwareLazy<out T>(
    private val componentLifecycleOwner: LifecycleOwner,
    private val ownerLiveData:LiveData<LifecycleOwner>,
    isMainThread: () -> Boolean = { Looper.myLooper() == Looper.getMainLooper() },
    initializer: () -> T
) :
    Lazy<T>,
    Serializable {
    private var initializer: (() -> T)? = initializer

    @Volatile
    @SuppressWarnings("Detekt.VariableNaming")
    private var _value: Any? = UninitializedValue

    // final field is required to enable safe publication of constructed instance
    private val lock = this

    init {
        if (isMainThread()) {
            initializeWhenCreated(componentLifecycleOwner)
        } else {
            Handler(Looper.getMainLooper()).post {
                initializeWhenCreated(componentLifecycleOwner)
            }
        }
    }

    private fun initializeWhenCreated(owner: LifecycleOwner) {
        ownerLiveData.observe(owner){
            val lifecycleState = it.lifecycle.currentState
            when {
                lifecycleState == Lifecycle.State.DESTROYED || isInitialized() -> {
                    return@observe
                }
                lifecycleState == Lifecycle.State.INITIALIZED -> {
                    it.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onCreate(owner: LifecycleOwner) {
                            if (!isInitialized()) value
                            owner.lifecycle.removeObserver(this)
                        }
                    })
                }
                else -> {
                    if (!isInitialized()) value
                }
            }
        }

    }

    @Suppress("LocalVariableName")
    override val value: T
        get() {
            @SuppressWarnings("Detekt.VariableNaming")
            val _v1 = _value
            if (_v1 !== UninitializedValue) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as T
            }

            return synchronized(lock) {
                @SuppressWarnings("Detekt.VariableNaming")
                val _v2 = _value
                if (_v2 !== UninitializedValue) {
                    @Suppress("UNCHECKED_CAST") (_v2 as T)
                } else {
                    val typedValue = initializer!!()
                    _value = typedValue
                    initializer = null
                    typedValue
                }
            }
        }

    override fun isInitialized(): Boolean = _value !== UninitializedValue

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}
