@file:Suppress("DeferredIsResult")

package com.x930073498.compoacture.ability

import com.x930073498.compoacture.component.*
import kotlinx.coroutines.Deferred
import kotlin.reflect.KClass

class ViewModelAbility<T : IStoreViewModel>(private val clazz: KClass<T>) : Action<T> {
    override fun action(storeViewModel: IStoreViewModel, scope: StoreViewModelScope): T {
        return scope[clazz]
    }
}

suspend fun <T : IStoreViewModel, R> IStoreViewModel.onViewModel(
    clazz: KClass<T>,
    action: suspend T.() -> R
): R {
    return pushAction(ViewModelAbility(clazz)).await().run {
        action(this)
    }
}

fun <T : IStoreViewModel, R> IStoreViewModel.runOnViewModel(
    clazz: KClass<T>,
    action: suspend T.() -> R
): Deferred<R> {
    return async {
        onViewModel(clazz, action)
    }
}


suspend inline fun <reified T : IStoreViewModel, R> IStoreViewModel.onViewModel(noinline action: suspend T.() -> R): R {
    return onViewModel(T::class, action)
}

inline fun <reified T : IStoreViewModel, R> IStoreViewModel.runOnViewModel(
    noinline action: suspend T.() -> R
): Deferred<R> {
    return runOnViewModel(T::class, action)
}

