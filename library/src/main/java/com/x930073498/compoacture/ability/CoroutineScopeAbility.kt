package com.x930073498.compoacture.ability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.x930073498.compoacture.component.IStoreViewModel
import com.x930073498.compoacture.component.fromStore
import com.x930073498.compoacture.component.getOrCreate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.Closeable
import kotlin.coroutines.CoroutineContext


internal val IStoreViewModel.storeViewModelScope: CoroutineScope
    get() {
        if (this is ViewModel) return viewModelScope
        return fromStore {
            getOrCreate("815d6a74-b0f5-497c-8f83-2eaa065e478e") {
                CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            }
        }
    }



private class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
    override val coroutineContext: CoroutineContext = context

    override fun close() {
        coroutineContext.cancel()
    }
}