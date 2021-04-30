@file:Suppress("UNCHECKED_CAST")

package com.x930073498.compoacture.component

import android.view.View
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.x930073498.compoacture.utils.componentActivity


class DelegateStoreViewModelScope(
    override val environment: ComponentEnvironment
) : StoreViewModelScope {

}

fun storeViewModelScopeFrom(fragment: Fragment): StoreViewModelScope {
    if (fragment is StoreViewModelScope) return fragment
    return getBaseViewModel(fragment, fragment.arguments)
        .getOrCreate("f54a0301-f7c6-4ed3-946a-496f3d201d13") {
            DelegateStoreViewModelScope(environmentFrom(fragment))
        }
}

fun storeViewModelScopeFrom(view: View): StoreViewModelScope {
    val owner = ViewTreeViewModelStoreOwner.get(view)
    if (owner is Fragment) {
        return storeViewModelScopeFrom(owner)
    }
    if (owner is ComponentActivity) {
        storeViewModelScopeFrom(owner)
    }
    val activity = view.context.componentActivity
    require(activity != null) {
        "请确认view属于某个fragment或者ComponentActivity"
    }
    return storeViewModelScopeFrom(activity)
}

fun storeViewModelScopeFrom(activity: ComponentActivity): StoreViewModelScope {
    if (activity is StoreViewModelScope) return activity
    return getBaseViewModel(activity, activity.intent?.extras)
        .getOrCreate("f54a0301-f7c6-4ed3-946a-496f3d201d13") {
            DelegateStoreViewModelScope(environmentFrom(activity))
        }
}

interface StoreViewModelScope {

    val environment: ComponentEnvironment
    get() {
        return environmentFrom(this)
    }



}








