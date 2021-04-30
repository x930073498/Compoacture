package com.x930073498.compoacture.utils

import androidx.activity.ComponentActivity
import com.x930073498.compoacture.component.StoreViewModelScope
import com.x930073498.compoacture.component.storeViewModelScopeFrom

val ComponentActivity.storeViewModelScope: StoreViewModelScope
    get() {
        return storeViewModelScopeFrom(this)
    }

fun ComponentActivity.withStoreViewModelProvider(action: StoreViewModelScope.() -> Unit) {
    action(storeViewModelScope)
}
