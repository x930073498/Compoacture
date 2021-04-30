package com.x930073498.compoacture.component

import android.app.Application
import androidx.annotation.CallSuper
import androidx.collection.arrayMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.didi.drouter.api.DRouter
import java.io.Closeable
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class DefaultStoreViewModel(savedStateHandle: SavedStateHandle) : IStoreViewModel {

    private var delegate: SavedStateStore = SavedStateHandleStore(savedStateHandle)
    override val storeId: String = delegate.id


    init {
        val factory = DRouter.build(SavedStateStoreFactory::class.java)
            .setDefaultIfEmpty(DefaultSavedStateStoreFactory())
            .getService()
        addStore(factory::create)
    }

     override fun addStore(store:SavedStateStore?) {
        if (store?.id == storeId) {
            delegate = DelegateSavedStateStore(delegate, store)
        }
    }

     override val savedStateStore: SavedStateStore
        get() = delegate

}


open class StoreViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application),
    IStoreViewModel {
    private val defaultStoreViewModel = DefaultStoreViewModel(savedStateHandle)

    @CallSuper
    override fun onCleared() {
        _children.forEach {
            it.clearCache()
        }
        clearCache()
    }

    final override val storeId: String
        get() = defaultStoreViewModel.storeId

    final override fun addStore(store: SavedStateStore?) {
        defaultStoreViewModel.addStore(store)
    }

    final override val savedStateStore: SavedStateStore
        get() = defaultStoreViewModel.savedStateStore


}


abstract class AbstractSavedStateStore(final override val id: String) : SavedStateStore,
    CacheStore by DefaultCacheStore()

interface SavedStateStore : CacheStore {
    fun contains(key: String): Boolean
    fun saveState(key: String, value: Any?)
    fun <T> getSavedState(key: String): T?
    fun removeSaveState(key: String)
    val id: String
        get() {
            val idKey = "6198d5d6-593b-44d6-ac51-9b6402a13045"
            return getSavedState<String>(idKey) ?: run {
                UUID.randomUUID().toString().apply {
                    saveState(idKey, this)
                    putCache("${this}_initialState", InitialState.NewInstance)
                }
            }
        }
}

/**
 * viewModel销毁时会清空
 */
interface CacheStore {
    fun putCache(key: String, value: Any?)
    fun <T> putCacheIfAbsent(key: String, value: T): T
    fun removeCache(key: String)
    fun <T> getCache(key: String): T?
    fun containsCache(key: String): Boolean
    fun clearCache()
}

@Suppress("UNCHECKED_CAST")
class DefaultCacheStore : CacheStore {
    private val map = arrayMapOf<String, Any>()
    private val lock = ReentrantLock()
    override fun putCache(key: String, value: Any?) {
        lock.withLock {
            map[key] = value
        }
    }

    override fun <T> putCacheIfAbsent(key: String, value: T): T {
        return lock.withLock {
            val previous: T? = map[key] as? T
            if (previous == null) {
                map[key] = value
            }
            val result: T = previous ?: value
            result
        }
    }

    override fun removeCache(key: String) {
        lock.withLock {
            map.remove(key)
        }
    }

    override fun <T> getCache(key: String): T? {
        return lock.withLock {
            map[key] as? T
        }
    }

    override fun containsCache(key: String): Boolean {
        return lock.withLock {
            map.containsKey(key)
        }
    }

    override fun clearCache() {
        lock.withLock {
            map.values.forEach {
                if (it is Closeable) {
                    try {
                        it.close()
                    } catch (e: Exception) {

                    }
                }
                if (it is Disposable) {
                    it.dispose()
                }
            }
            map.clear()
        }
    }

}


val SavedStateStore.initialState: InitialState
    get() {
        val currentId = id
        return getCache<InitialState>("${currentId}_initialState") ?: InitialState.Restore
    }


sealed class InitialState {
    object Restore : InitialState()
    object NewInstance : InitialState()
}

private class DelegateSavedStateStore(val origin: SavedStateStore, val delegate: SavedStateStore) :
    SavedStateStore {

    override fun contains(key: String): Boolean {
        return delegate.contains(key) || origin.contains(key)
    }

    override fun saveState(key: String, value: Any?) {
        delegate.saveState(key, value)
    }

    override fun <T> getSavedState(key: String): T? {
        return if (delegate.contains(key)) delegate.getSavedState(key)
        else origin.getSavedState(key)
    }

    override fun removeSaveState(key: String) {
        delegate.removeSaveState(key)
        origin.removeSaveState(key)
    }

    override fun putCache(key: String, value: Any?) {
        delegate.putCache(key, value)
        if (origin.contains(key)) origin.removeCache(key)
    }

    override fun <T> putCacheIfAbsent(key: String, value: T): T {
        val result = delegate.putCacheIfAbsent(key, value)
        if (origin.contains(key)) origin.removeCache(key)
        return result
    }

    override fun removeCache(key: String) {
        delegate.removeCache(key)
        origin.removeCache(key)
    }

    override fun <T> getCache(key: String): T? {
        if (delegate.containsCache(key)) return delegate.getCache(key)
        return origin.getCache(key)
    }

    override fun containsCache(key: String): Boolean {
        return origin.containsCache(key) || delegate.containsCache(key)
    }

    override fun clearCache() {
        origin.clearCache()
        delegate.clearCache()
    }

}

class SavedStateHandleStore(private val savedStateHandle: SavedStateHandle) : SavedStateStore,
    CacheStore by DefaultCacheStore() {
    override fun contains(key: String): Boolean {
        return savedStateHandle.contains(key)
    }

    override fun saveState(key: String, value: Any?) {
        savedStateHandle.set(key, value)
    }

    override fun <T> getSavedState(key: String): T? {
        return savedStateHandle.get<T>(key)
    }

    override fun removeSaveState(key: String) {
        savedStateHandle.remove<Any>(key)
    }

}

