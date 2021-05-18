@file:Suppress("ObjectPropertyName", "EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")

package com.x930073498.compoacture.component

import androidx.lifecycle.*
import com.x930073498.compoacture.ability.storeViewModelScope
import com.x930073498.compoacture.internal.ObservableLiveData
import com.x930073498.compoacture.isDebug
import com.x930073498.compoacture.utils.invokeOnMain
import kotlinx.coroutines.*
import java.lang.IllegalStateException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible


interface IStoreViewModel : StoreViewModelLifecycle {
    val storeId: String

    fun addStore(store: SavedStateStore?)

    val savedStateStore: SavedStateStore


}

fun IStoreViewModel.addStore(builder: (String) -> SavedStateStore?) {
    addStore(builder(storeId))
}

fun IStoreViewModel.clearCache() {
    savedStateStore.clearCache()
}


internal fun IStoreViewModel.dispatchAttach(scope: StoreViewModelScope) {
    invokeOnMain {
        onAttach(scope)
        scope.environment.lifecycleOwner.lifecycle.addObserver(getOrCreate("33687f17-2eda-4e3f-80ad-c6ef44e18ded") {
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    clearFlagKeyOnDestroy()
                }
            }
        })
    }

}

interface IStoreViewModelFactory {
    fun <T> factory(
        storeScope: StoreViewModelScope,
        clazz: KClass<T>
    ): T where T : IStoreViewModel {
        return storeScope.run {
            val provider = storeViewModel.getOrCreate("bbabb3e8-5f58-4d34-881a-6f7c0c0c5aac") {
                environment.viewModelProvider
            }
            if (ViewModel::class.java.isAssignableFrom(clazz.java)) {
                provider[clazz.java as Class<ViewModel>] as T
            } else error("please override getStore function")
        }

    }

}


fun IStoreViewModel.setKeyRemoveOnDestroy(key: String) {
    val list = getOrCreate("0b7fa768-ebfa-46be-a866-2317c9040e72") {
        arrayListOf<String>()
    }
    if (list.contains(key)) return
    list.add(key)
}

private fun IStoreViewModel.clearFlagKeyOnDestroy() {
    val list = getOrNull<MutableList<String>>("0b7fa768-ebfa-46be-a866-2317c9040e72") ?: return
    list.forEach {
        fromStore { removeCache(it) }
    }
    list.clear()
}


fun IStoreViewModel.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return storeViewModelScope.launch(context, start, block)
}

fun <T> IStoreViewModel.async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    return storeViewModelScope.async(context, start, block)
}


internal val IStoreViewModel._children: MutableList<IStoreViewModel>
    get() {
        return getOrCreate("d08ce7cb-e748-4efe-8001-684610bc5f5c") {
            arrayListOf()
        }
    }

internal fun IStoreViewModel.addChild(viewModel: IStoreViewModel) {
    if (_children.contains(viewModel)) return
    _children.add(viewModel)
}

fun <T : IStoreViewModel> T.currentTypeInstance(): CurrentViewModelTypeInstance<T> {
    return this as? CurrentViewModelTypeInstance<T> ?: getOrCreate("2a354d24-8efa-4974-876b-24b9bc697da3") {
        object : CurrentViewModelTypeInstance<T> {
            override val currentInstance: T
                get() = this@currentTypeInstance
        }
    }
}

fun <T> IStoreViewModel.fromStore(action: SavedStateStore.() -> T): T {
    return action(savedStateStore)
}

fun IStoreViewModel.removeCache(key: String) {
    return fromStore {
        this.removeCache(key)
    }
}

fun IStoreViewModel.containsCache(key: String): Boolean {
    return fromStore {
        this.containsCache(key)
    }
}

fun <T> IStoreViewModel.getOrCreate(key: String, action: SavedStateStore.() -> T): T {
    return fromStore {
        var result = getCache<T>(key)
        if (result == null) {
            result = action()
            putCache(key, result)
            result
        } else
            result
    }
}

fun IStoreViewModel.putCache(key: String, value: Any?) {
    fromStore { this.putCache(key, value) }
}

fun <T> IStoreViewModel.require(key: String): T {
    return getOrNull(key) ?: error("please put key-value pair first")
}

fun <T> IStoreViewModel.getOrNull(key: String): T? {
    return fromStore {
        getCache<T>(key)
    }
}

private const val prefixPropertyKey = "96ec3c1f-5d96-4973-a7a6-536c4916b5c5"

private fun getPropertyKey(property: KProperty<*>): String {
    return getPropertyKey(property.name)
}

private fun getPropertyKey(propertyName: String): String {
    return "_${prefixPropertyKey}_${propertyName}_"
}

private fun getPropertyLiveDataKey(propertyName: String): String {
    return getPropertyKey(propertyName) + "*_live_data_*"
}


internal val IStoreViewModel.lifecycleActionLiveData: MutableLiveData<ActionBoat<*>>
    get() {
        return getOrCreate("79d80c4d-da13-4739-bfd4-de1dc68530c1") {
            MutableLiveData()
        }
    }

fun <R> IStoreViewModel.pushAction(action: Action<R>): ActionHandle<R> {
    val handle = ActionHandle<R>()
    val boat = ActionBoat(action, handle)
    invokeOnMain { lifecycleActionLiveData.value = boat }
    return handle
}

fun <T : IStoreViewModel, R> T.asLiveData(property: KProperty1<T, R>): LiveData<R> {
    return getPropertyLiveData(property)
}

internal fun <T, V> V.getPropertyLiveData(property: KProperty<T>): MutableLiveData<T> where V : IStoreViewModel {
    return getOrCreate(getPropertyLiveDataKey(property.name)) {
        if (property is KProperty0<*>) {
            property as KProperty0<T>
            if (isDebug) {
                property.isAccessible = true
                if (property.getDelegate() !is StoreViewModelReadWriteProperty<*, *>) {
                    throw StorePropertyException(property, this@getPropertyLiveData)
                }
            }
            return@getOrCreate MutableLiveData(property.get())
        } else if (property is KProperty1<*, *>) {
            property as KProperty1<IStoreViewModel, T>
            if (isDebug) {
                property.isAccessible = true
                if (property.getDelegate(this@getPropertyLiveData) !is StoreViewModelReadWriteProperty<*, *>) {
                    throw StorePropertyException(property, this@getPropertyLiveData)
                }
            }
            return@getOrCreate MutableLiveData(property.get(this@getPropertyLiveData))
        }
        MutableLiveData()
    }
}

private fun IStoreViewModel.setPropertyChange(property: KProperty<*>, value: Any?) {
    fromStore {
        getCache<MutableLiveData<Any>>(getPropertyLiveDataKey(property.name))?.postValue(value)
    }
}

fun <T> IStoreViewModel.saveStateProperty(defaultValue: () -> T): ReadWriteProperty<IStoreViewModel, T> {
    return object : StoreViewModelReadWriteProperty<IStoreViewModel, T> {
        override fun setValue(thisRef: IStoreViewModel, property: KProperty<*>, value: T) {
            fromStore {
                saveState(getPropertyKey(property), value)
                setPropertyChange(property, value)
            }
        }

        override fun getValue(thisRef: IStoreViewModel, property: KProperty<*>): T {
            return fromStore {
                getSavedState(getPropertyKey(property)) ?: defaultValue()
            }
        }
    }
}

fun <T> IStoreViewModel.saveStateProperty(defaultValue: T) = saveStateProperty { defaultValue }
fun <T> IStoreViewModel.saveStateProperty(): ReadWriteProperty<IStoreViewModel, T?> {
    return object : StoreViewModelReadWriteProperty<IStoreViewModel, T?> {
        override fun setValue(thisRef: IStoreViewModel, property: KProperty<*>, value: T?) {
            fromStore {
                saveState(getPropertyKey(property), value)
                setPropertyChange(property, value)
            }
        }

        override fun getValue(thisRef: IStoreViewModel, property: KProperty<*>): T? {
            return fromStore { getSavedState(getPropertyKey(property)) }
        }
    }
}


fun <T> IStoreViewModel.saveStateLiveDataProperty(defaultValue: () -> T?): ReadWriteProperty<IStoreViewModel, MutableLiveData<T>> {
    return object : StoreViewModelReadWriteProperty<IStoreViewModel, MutableLiveData<T>> {
        override fun setValue(
            thisRef: IStoreViewModel,
            property: KProperty<*>,
            value: MutableLiveData<T>
        ) {
            fromStore {
                putCache(getPropertyKey(property), value)
                setPropertyChange(property, value)
            }
        }

        override fun getValue(
            thisRef: IStoreViewModel,
            property: KProperty<*>
        ): MutableLiveData<T> {
            val key = getPropertyKey(property)
            var result = fromStore {
                getCache<MutableLiveData<T>>(key)
            }
            if (result != null) return result
            val data = fromStore { if (contains(key)) getSavedState<T>(key) else defaultValue() }
            result = if (data != null) ObservableLiveData(data) {
                fromStore {
                    saveState(key, this@ObservableLiveData)
                }
            } else ObservableLiveData {
                fromStore {
                    saveState(key, this@ObservableLiveData)
                }
            }
            fromStore { putCache(key, result) }
            return result

        }

    }
}

fun <T> IStoreViewModel.saveStateLiveDataProperty(defaultValue: T? = null): ReadWriteProperty<IStoreViewModel, MutableLiveData<T>> =
    saveStateLiveDataProperty { defaultValue }

fun <T> IStoreViewModel.saveStateListLiveDataProperty(): ReadWriteProperty<IStoreViewModel, MutableLiveData<List<T>>> =
    saveStateLiveDataProperty()

fun <T> IStoreViewModel.property(defaultValue: () -> T): ReadWriteProperty<IStoreViewModel, T> {
    return object : StoreViewModelReadWriteProperty<IStoreViewModel, T> {
        override fun setValue(thisRef: IStoreViewModel, property: KProperty<*>, value: T) {
            fromStore {
                putCache(getPropertyKey(property), value)
                setPropertyChange(property, value)
            }

        }

        override fun getValue(thisRef: IStoreViewModel, property: KProperty<*>): T {
            return fromStore {
                getCache(getPropertyKey(property))
                    ?: defaultValue()
            }
        }
    }
}

fun <T> IStoreViewModel.property(defaultValue: T): ReadWriteProperty<IStoreViewModel, T> =
    property { defaultValue }

fun <T> IStoreViewModel.liveDataProperty(defaultValue: T? = null) =
    liveDataProperty { defaultValue }

fun <T> IStoreViewModel.liveDataProperty(defaultValue: () -> T?): ReadWriteProperty<IStoreViewModel, MutableLiveData<T>> {
    return object : StoreViewModelReadWriteProperty<IStoreViewModel, MutableLiveData<T>> {
        override fun setValue(
            thisRef: IStoreViewModel,
            property: KProperty<*>,
            value: MutableLiveData<T>
        ) {
            fromStore {
                putCache(getPropertyKey(property), value)
                setPropertyChange(property, value)
            }
        }

        override fun getValue(
            thisRef: IStoreViewModel,
            property: KProperty<*>
        ): MutableLiveData<T> {
            val key = getPropertyKey(property)
            var result = fromStore { this.getCache<MutableLiveData<T>>(key) }
            if (result == null) {
                val value = defaultValue()
                result = if (value == null) MutableLiveData() else MutableLiveData(value)
                fromStore {
                    putCache(key, result)
                }
            }
            return result
        }
    }
}

fun <T> IStoreViewModel.listLiveDataProperty(): ReadWriteProperty<IStoreViewModel, MutableLiveData<List<T>>> =
    liveDataProperty()

fun <T> IStoreViewModel.property(): ReadWriteProperty<IStoreViewModel, T?> {
    return object : StoreViewModelReadWriteProperty<IStoreViewModel, T?> {
        override fun setValue(thisRef: IStoreViewModel, property: KProperty<*>, value: T?) {
            fromStore {
                putCache(getPropertyKey(property), value)
                setPropertyChange(property, value)
            }
        }

        override fun getValue(thisRef: IStoreViewModel, property: KProperty<*>): T? {
            return fromStore { getCache(getPropertyKey(property)) }
        }
    }
}

internal interface StoreViewModelReadWriteProperty<T, V> : ReadWriteProperty<T, V>
internal interface StoreViewModelReadOnlyProperty<T, V> : ReadOnlyProperty<T, V>
internal class StorePropertyException(property: KProperty<*>, viewModel: IStoreViewModel) :
    IllegalStateException("当前属性{${viewModel::class.qualifiedName}.${property.name}}不是StoreViewModelReadWriteProperty类型代理，请使用IStoreViewModel提供的扩展代理该属性")





