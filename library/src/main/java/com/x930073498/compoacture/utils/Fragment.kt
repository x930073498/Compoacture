package com.x930073498.compoacture.utils

import androidx.fragment.app.Fragment
import com.x930073498.compoacture.component.*
import com.x930073498.compoacture.internal.lifecycleAwareLazy
import kotlin.reflect.KClass

val Fragment.parentStoreScope: StoreViewModelScope
    get() {
        val parent = parentFragment
        return if (parent != null) {
            if (parent is StoreViewModelScope) return parent
           storeViewModelScopeFrom(parent)
        } else {
          storeViewModelScopeFrom(requireActivity())
        }
    }
val Fragment.storeViewModelScope: StoreViewModelScope
    get() {
        if (this is StoreViewModelScope) return this
        return storeViewModelScopeFrom(this)
    }
val Fragment.activityStoreViewModelScope: StoreViewModelScope
    get() {
        if (this is StoreViewModelScope) return (this as StoreViewModelScope).activityStoreViewModelScope
        return storeViewModelScopeFrom(requireActivity())
    }

fun Fragment.withStoreViewModelProvider(action: StoreViewModelScope.() -> Unit) {
    action(storeViewModelScope)
}

fun Fragment.withActivityStoreViewModelProvider(action: StoreViewModelScope.() -> Unit) {
    action(activityStoreViewModelScope)
}

fun <T> Fragment.parentStoreViewModel(clazz: KClass<T>): Lazy<T> where T : IStoreViewModel {
    return lifecycleAwareLazy(this, viewLifecycleOwnerLiveData) {
        parentStoreScope[clazz]
    }
}
inline fun <reified T> Fragment.parentStoreViewModel(): Lazy<T> where T : IStoreViewModel {
    return parentStoreViewModel(T::class)
}

