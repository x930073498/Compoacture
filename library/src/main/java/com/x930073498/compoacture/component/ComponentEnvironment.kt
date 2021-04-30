package com.x930073498.compoacture.component

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import com.x930073498.compoacture.dialog.invokeOnMain
import kotlin.reflect.KClass

interface ExtraProvider {
    val extras: Bundle?
        get() {
            return null
        }
}

interface ComponentEnvironment : LifecycleOwnerProvider,
    ComponentContext,
    IStoreViewModelFactory,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    ExtraProvider

private val map = mutableMapOf<Any, ComponentEnvironment>()

internal fun environmentFrom(scope: StoreViewModelScope): ComponentEnvironment {
    if (scope is Fragment) return environmentFrom(scope as Fragment)
    if (scope is ComponentActivity) return environmentFrom(scope as ComponentActivity)
    error("请手动提供ComponentEnvironment")
}

internal fun environmentFrom(fragment: Fragment): ComponentEnvironment {
    return map[fragment] ?: getBaseViewModel(
        fragment,
        fragment.arguments
    ).getOrCreate("14784d23-18d2-4bf8-9a81-a1665dce8efc") {
        object : ComponentEnvironment, DefaultLifecycleObserver {
            init {
                invokeOnMain { fragment.lifecycle.addObserver(this) }
                map[fragment] = this
            }

            override fun onDestroy(owner: LifecycleOwner) {
                map.remove(fragment)
            }

            override val lifecycleOwnerLiveData: LiveData<LifecycleOwner> =
                fragment.viewLifecycleOwnerLiveData

            override val componentLifecycleOwner: LifecycleOwner = fragment


            override val current: Any
                get() = fragment
            override val hostActivity: ComponentActivity
                get() = fragment.requireActivity()


            override fun getViewModelStore(): ViewModelStore {
                return fragment.viewModelStore
            }

            override fun getLifecycle(): Lifecycle {
                return fragment.lifecycle
            }

            override fun getSavedStateRegistry(): SavedStateRegistry {
                return fragment.savedStateRegistry
            }

            override val extras: Bundle?
                get() = fragment.arguments

        }
    }


}

internal fun environmentFrom(activity: ComponentActivity): ComponentEnvironment {
    map[activity]?.run { return this }
    val baseViewModel = getBaseViewModel(activity, activity.intent?.extras)
    return baseViewModel.getOrCreate("14784d23-18d2-4bf8-9a81-a1665dce8efc") {
        object : ComponentEnvironment, DefaultLifecycleObserver {
            init {
                invokeOnMain { activity.lifecycle.addObserver(this) }
                map[activity] = this
            }

            override fun onDestroy(owner: LifecycleOwner) {
                map.remove(activity)
            }

            override val lifecycleOwnerLiveData: LiveData<LifecycleOwner> =
                baseViewModel.getOrCreate("c0d3034f-5d97-4148-b6de-f1e6d3c51b04") {
                    MutableLiveData(activity)
                }

            override val componentLifecycleOwner: LifecycleOwner = activity


            override val current: Any
                get() = activity
            override val hostActivity: ComponentActivity
                get() = activity


            override fun getViewModelStore(): ViewModelStore {
                return activity.viewModelStore
            }

            override fun getLifecycle(): Lifecycle {
                return activity.lifecycle
            }

            override fun getSavedStateRegistry(): SavedStateRegistry {
                return activity.savedStateRegistry
            }

            override val extras: Bundle?
                get() = activity.intent?.extras

        }
    }


}

val ComponentEnvironment.viewModelProvider: ViewModelProvider
    get() {
        return getViewModelProvider(this, extras)
    }