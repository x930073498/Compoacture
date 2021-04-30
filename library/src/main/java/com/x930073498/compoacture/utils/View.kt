package com.x930073498.compoacture.utils

import android.view.View
import com.x930073498.compoacture.component.StoreViewModelScope
import com.x930073498.compoacture.component.storeViewModelScopeFrom

val View.storeViewModelScope: StoreViewModelScope
    get() {
        return storeViewModelScopeFrom(this)
    }
val View.activityStoreViewModelScope: StoreViewModelScope
    get() {
        return storeViewModelScopeFrom(context.componentActivity!!)
    }

fun View.withStoreViewModelProvider(action: StoreViewModelScope.() -> Unit) {
    action(storeViewModelScope)
}

fun View.withActivityStoreViewModelProvider(action: StoreViewModelScope.() -> Unit) {
    action(activityStoreViewModelScope)
}
