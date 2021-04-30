package com.x930073498.compoacture.component

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

typealias UnitAction=Action<Unit>

interface Action<R> : Disposable {

    fun action(storeViewModel: IStoreViewModel, scope: StoreViewModelScope):R

    override fun dispose() {

    }
}

internal data class ActionBoat<R>(val action: Action<R>, val handle: ActionHandle<R>)

class ActionHandle<R> {
    private val completableDeferred = CompletableDeferred<R>()
    private var callback: R.() -> Unit = {}

    suspend fun await() = completableDeferred.await()


    fun complete(result:R) {
        completableDeferred.complete(result)
        callback(result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun setCallback(callback: R.() -> Unit) {
        if (isCompleted()) callback(completableDeferred.getCompleted())
        else {
            this.callback = callback
        }
    }

    fun isCompleted() = completableDeferred.isCompleted


}