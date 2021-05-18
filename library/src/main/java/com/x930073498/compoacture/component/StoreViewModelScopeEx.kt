@file:Suppress("UNCHECKED_CAST")

package com.x930073498.compoacture.component

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.x930073498.compoacture.application
import com.x930073498.compoacture.databinder.DataBinder
import com.x930073498.compoacture.databinder.DataChangeListener
import com.x930073498.compoacture.databinder.DataReverseBinder
import com.x930073498.compoacture.internal.lifecycleAwareLazy
import com.x930073498.compoacture.utils.invokeOnMain
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1


val StoreViewModelScope.storeViewModel: IStoreViewModel
    get() {
        return environment.getCache("d703a756-a5d8-4729-b89f-a6dfaab448d8") ?: getBaseViewModel(
            environment,
            environment.extras
        ).apply {
            putCache("d703a756-a5d8-4729-b89f-a6dfaab448d8", this)
            this.fromStore {
                if (!contains("66717253-00fd-439a-bb17-e72452bf690e")) {
                    putCache("66717253-00fd-439a-bb17-e72452bf690e", 1)
                    dispatchAttach(this@storeViewModel)
                }
            }
        }
    }


internal fun <T> getViewModelProvider(
    environment: T,
    extra: Bundle?
): ViewModelProvider where T : ViewModelStoreOwner, T : SavedStateRegistryOwner {
    return invokeOnMain {
        ViewModelProvider(
            environment,
            SavedStateViewModelFactory(application, environment, extra)
        )
    }
}

internal fun <T> getBaseViewModel(
    environment: T,
    extra: Bundle?
): IStoreViewModel where T : ViewModelStoreOwner, T : SavedStateRegistryOwner {
    return getViewModelProvider(environment, extra)[StoreViewModel::class.java]
}


val StoreViewModelScope.activityStoreViewModelScope: StoreViewModelScope
    get() {
        return fromViewModel {
            getOrCreate("c905f722-0515-4d54-b467-a58a1a15e326") {
                storeViewModelScopeFrom(environment.hostActivity)
            }
        }
    }


@Suppress("UNCHECKED_CAST")
fun <T> StoreViewModelScope.getViewModel(
    clazz: KClass<T>
): T where T : IStoreViewModel {
    return invokeOnMain {
        storeViewModel.getOrCreate("ef84fb33-a4a8-4603-a649-54a416901078" + clazz.qualifiedName) {
            environment.factory(this@getViewModel, clazz).apply {
                storeViewModel.addChild(this)
                dispatchAttach(this@getViewModel)
                bindViewModelAction(this)
            }
        }
    }
}

fun <T> StoreViewModelScope.bindViewModelAction(
    t: T
) where T : IStoreViewModel {
    fromViewModel {
        val key = "ae5f08cb-2cfd-4dc8-9d02-609720fe8bb9"
        getOrCreate(key) {
            setKeyRemoveOnDestroy(key)
            t.lifecycleActionLiveData.observe(environment.lifecycleOwner) {
                val action = it.action as Action<Any?>
                val handle = it.handle as ActionHandle<Any?>
                handle.complete(action.action(t, this@bindViewModelAction))
            }
        }
    }

}

inline fun <reified T> StoreViewModelScope.getViewModel(): T where T : IStoreViewModel {
    return getViewModel(T::class)
}

fun <T> StoreViewModelScope.fromViewModel(action: IStoreViewModel.() -> T): T {
    return action(storeViewModel)
}

operator fun <T> StoreViewModelScope.get(clazz: KClass<T>): T where T : IStoreViewModel {
    return getViewModel(clazz)
}

fun <T, R> StoreViewModelScope.withViewModel(
    clazz: KClass<R>,
    action: R.() -> T
): T where R : IStoreViewModel {
    return with(get(clazz), action)
}


inline fun <reified R, T> StoreViewModelScope.withViewModel(
    noinline action: R.() -> T
): T where R : IStoreViewModel {
    return withViewModel(R::class, action)
}

fun <T, V, S> StoreViewModelScope.withProperty(
    clazz: KClass<T>,
    property: KProperty1<T, V>,
    action: V.() -> S
): S where T : IStoreViewModel {
    return action(property.get(get(clazz)))
}


inline fun <reified T, V, S> StoreViewModelScope.withProperty(
    property: KProperty1<T, V>,
    noinline action: V.() -> S
): S where T : IStoreViewModel {
    return withProperty(T::class, property, action)
}

fun <T, V> StoreViewModelScope.propertyValue(
    clazz: KClass<T>,
    property: KProperty1<T, V>,
): V where T : IStoreViewModel {
    return property.get(get(clazz))
}

inline fun <reified T, V> StoreViewModelScope.propertyValue(
    property: KProperty1<T, V>,
): V where T : IStoreViewModel {
    return property.get(get(T::class))
}


inline fun <T, reified R, S> StoreViewModelScope.bindViewLifecycle(
    property: KProperty1<R, T>,
    noinline action: S.(R) -> Unit
) where R : IStoreViewModel, T : LiveData<S> {
    bindViewLifecycle(R::class, property, action)
}

inline fun <T, reified R, S> StoreViewModelScope.bindViewLifecycle(
    property: KProperty1<R, T>,
    vararg binders: DataBinder<S>
) where R : IStoreViewModel, T : LiveData<S> {
    bindViewLifecycle(R::class, property, *binders)
}

fun <T, R, S> StoreViewModelScope.bindViewLifecycle(
    clazz: KClass<R>,
    property: KProperty1<R, T>,
    action: S.(R) -> Unit
) where R : IStoreViewModel, T : LiveData<S> {
    withViewModel(clazz) {
        property.invoke(this).observe(environment.lifecycleOwner) {
            action(it, this)
        }
    }
}

fun <T, R, S> StoreViewModelScope.bindViewLifecycle(
    clazz: KClass<R>,
    property: KProperty1<R, T>,
    vararg binders: DataBinder<S>
) where R : IStoreViewModel, T : LiveData<S> {
    for (binder in binders) {
        withViewModel(clazz) {
            val liveData: LiveData<S> = property.invoke(this)
            if (liveData is MutableLiveData<S> && binder is DataReverseBinder<S>) {
                binder.register(DataChangeListener {
                    liveData.postValue(it)
                })
            }
            liveData.observe(environment.lifecycleOwner) {
                binder.bind(it)
            }
        }
    }
}

fun <T, R, S> StoreViewModelScope.bindViewLifecycle(
    value: R,
    property: KProperty1<R, T>,
    action: S.(R) -> Unit
) where R : IStoreViewModel, T : LiveData<S> {
    with(value) {
        property.invoke(this).observe(environment.lifecycleOwner) {
            action(it, this)
        }
    }
}

fun <T, R, S> StoreViewModelScope.bindViewLifecycle(
    value: R,
    property: KProperty1<R, T>,
    vararg binders: DataBinder<S>
) where R : IStoreViewModel, T : LiveData<S> {
    with(value) {
        val livedata: LiveData<S> = property.invoke(this)
        binders.forEach { binder ->
            if (binder is DataReverseBinder<S> && livedata is MutableLiveData<S>) {
                binder.register(DataChangeListener {
                    livedata.postValue(it)
                })
            }
            livedata.observe(environment.lifecycleOwner) {
                binder.bind(it)
            }

        }
    }
}

fun <T, S> StoreViewModelScope.bindViewLifecycle(
    property: KProperty0<T>,
    action: S.() -> Unit
) where  T : LiveData<S> {
    property.invoke().observe(environment.lifecycleOwner) {
        action(it)
    }
}

fun <T, S> StoreViewModelScope.bindViewLifecycle(
    property: KProperty0<T>,
    vararg binders: DataBinder<S>
) where  T : LiveData<S> {
    val liveData: LiveData<S> = property.get()
    binders.forEach { binder ->
        if (binder is DataReverseBinder<S> && liveData is MutableLiveData<S>) {
            binder.register(DataChangeListener {
                liveData.postValue(it)
            })
        }
        property.invoke().observe(environment.lifecycleOwner) {
            binder.bind(it)
        }

    }
}


fun <T, S> StoreViewModelScope.bindViewLifecycle(
    liveData: T,
    action: S.() -> Unit
) where T : LiveData<S> {
    liveData.observe(environment.lifecycleOwner, action)
}

fun <T, S> StoreViewModelScope.bindViewLifecycle(
    liveData: T,
    vararg binders: DataBinder<S>
) where T : LiveData<S> {
    val currentLiveData: LiveData<S> = liveData
    binders.forEach { binder ->
        if (binder is DataReverseBinder<S> && currentLiveData is MutableLiveData<S>) {
            binder.register(DataChangeListener {
                currentLiveData.postValue(it)
            })
        }
        liveData.observe(environment.lifecycleOwner) {
            binder.bind(it)
        }
    }
}


fun <T, S> StoreViewModelScope.bindComponentLifecycle(
    liveData: T,
    action: S.() -> Unit
) where T : LiveData<S> {
    liveData.observe(environment.componentLifecycleOwner, action)
}

fun <T, S> StoreViewModelScope.bindComponentLifecycle(
    liveData: T,
    vararg binders: DataBinder<S>
) where T : LiveData<S> {
    val currentLiveData: LiveData<S> = liveData
    binders.forEach { binder ->
        if (binder is DataReverseBinder<S> && currentLiveData is MutableLiveData<S>) {
            binder.register(DataChangeListener {
                currentLiveData.postValue(it)
            })
        }
        liveData.observe(environment.componentLifecycleOwner) {
            binder.bind(it)
        }
    }
}

inline fun <T, reified R, S> StoreViewModelScope.bindComponentLifecycle(
    property: KProperty1<R, T>,
    noinline action: S.(R) -> Unit
) where R : IStoreViewModel, T : LiveData<S> {
    bindComponentLifecycle(R::class, property, action)
}

inline fun <T, reified R, S> StoreViewModelScope.bindComponentLifecycle(
    property: KProperty1<R, T>,
    vararg binders: DataBinder<S>
) where R : IStoreViewModel, T : LiveData<S> {
    bindComponentLifecycle(R::class, property, *binders)
}

fun <T, R, S> StoreViewModelScope.bindComponentLifecycle(
    clazz: KClass<R>,
    property: KProperty1<R, T>,
    action: S.(R) -> Unit
) where R : IStoreViewModel, T : LiveData<S> {
    withViewModel(clazz) {
        property.invoke(this).observe(environment.componentLifecycleOwner) {
            action(it, this@withViewModel)
        }
    }
}

fun <T, R, S> StoreViewModelScope.bindComponentLifecycle(
    clazz: KClass<R>,
    property: KProperty1<R, T>,
    vararg binders: DataBinder<S>
) where R : IStoreViewModel, T : LiveData<S> {
    binders.forEach { binder ->
        withViewModel(clazz) {
            val liveData: LiveData<S> = property.get(this)
            if (binder is DataReverseBinder<S> && liveData is MutableLiveData<S>) {
                binder.register(DataChangeListener {
                    liveData.postValue(it)
                })
            }
            property.invoke(this).observe(environment.componentLifecycleOwner) {
                binder.bind(it)
            }
        }
    }
}

fun <T, R, S> StoreViewModelScope.bindComponentLifecycle(
    value: R,
    property: KProperty1<R, T>,
    action: S.(R) -> Unit
) where R : IStoreViewModel, T : LiveData<S> {
    with(value) {
        property.invoke(this).observe(environment.componentLifecycleOwner) {
            action(it, this)
        }
    }
}

fun <T, R, S> StoreViewModelScope.bindComponentLifecycle(
    value: R,
    property: KProperty1<R, T>,
    vararg binders: DataBinder<S>
) where R : IStoreViewModel, T : LiveData<S> {
    for (binder in binders) {
        with(value) {
            val liveData: LiveData<S> = property.get(this)
            if (binder is DataReverseBinder<S> && liveData is MutableLiveData<S>) {
                binder.register(DataChangeListener {
                    liveData.postValue(it)
                })
            }
            liveData.observe(environment.componentLifecycleOwner) {
                binder.bind(it)
            }
        }
    }
}

fun <T, S> StoreViewModelScope.bindComponentLifecycle(
    property: KProperty0<T>,
    action: S.() -> Unit
) where T : LiveData<S> {
    property.invoke().observe(environment.componentLifecycleOwner) {
        action(it)
    }
}

fun <T, S> StoreViewModelScope.bindComponentLifecycle(
    property: KProperty0<T>,
    vararg binders: DataBinder<S>
) where T : LiveData<S> {
    val liveData: LiveData<S> = property.get()
    for (binder in binders) {
        if (binder is DataReverseBinder<S> && liveData is MutableLiveData<S>) {
            binder.register(DataChangeListener {
                liveData.postValue(it)
            })
        }
        liveData.observe(environment.componentLifecycleOwner) {
            binder.bind(it)
        }
    }

}

inline fun <T, reified R> StoreViewModelScope.bindViewLifecycleViewModelProperty(
    property: KProperty1<R, T>,
    noinline action: T.(R) -> Unit = {}
) where R : IStoreViewModel {
    bindViewLifecycleViewModelProperty(R::class, property, action)
}


fun <T, R> StoreViewModelScope.bindViewLifecycleViewModelProperty(
    clazz: KClass<R>,
    property: KProperty1<R, T>,
    action: T.(R) -> Unit = {}
) where R : IStoreViewModel {
    withViewModel(clazz) {
        getPropertyLiveData(property).observe(environment.lifecycleOwner) {
            action(it, this)
        }
    }
}


inline fun <T, reified R> StoreViewModelScope.bindComponentLifecycleViewModelProperty(
    property: KProperty1<R, T>,
    noinline action: T.(R) -> Unit
) where R : IStoreViewModel {
    bindComponentLifecycleViewModelProperty(R::class, property, action)
}

fun <T, R> StoreViewModelScope.bindComponentLifecycleViewModelProperty(
    valueOwner: R,
    valueProperty: KProperty1<R, T>,
    action: T.() -> Unit
) where R : IStoreViewModel {
    valueOwner.getPropertyLiveData(valueProperty)
        .observe(environment.componentLifecycleOwner, action)
}

fun <T, R> StoreViewModelScope.bindComponentLifecycleViewModelProperty(
    valueOwner: R,
    valueProperty: KProperty1<R, T>,
    vararg binders: DataBinder<T>
) where R : IStoreViewModel {
    binders.forEach { binder ->
        if (valueProperty is KMutableProperty1 && binder is DataReverseBinder) {
            binder.register(DataChangeListener {
                valueProperty.set(valueOwner, it)
            })
        }
        valueOwner.getPropertyLiveData(valueProperty)
            .observe(environment.componentLifecycleOwner) {
                binder.bind(it)
            }
    }

}

fun <T, R> StoreViewModelScope.bindComponentLifecycleViewModelProperty(
    clazz: KClass<R>,
    valueProperty: KProperty1<R, T>,
    action: T.() -> Unit
) where R : IStoreViewModel {
    get(clazz).getPropertyLiveData(valueProperty)
        .observe(environment.componentLifecycleOwner, action)
}

fun <T, R> StoreViewModelScope.bindComponentLifecycleViewModelProperty(
    clazz: KClass<R>,
    valueProperty: KProperty1<R, T>,
    vararg binders: DataBinder<T>
) where R : IStoreViewModel {
    binders.forEach { binder ->
        val valueOwner = get(clazz)
        if (valueProperty is KMutableProperty1 && binder is DataReverseBinder) {
            binder.register(DataChangeListener {
                valueProperty.set(valueOwner, it)
            })
        }
        valueOwner.getPropertyLiveData(valueProperty)
            .observe(environment.componentLifecycleOwner) {
                binder.bind(it)
            }
    }
}

inline fun <T, reified R> StoreViewModelScope.bindComponentLifecycleViewModelProperty(
    valueProperty: KProperty1<R, T>,
    noinline action: T.() -> Unit
) where R : IStoreViewModel {
    bindComponentLifecycleViewModelProperty(R::class, valueProperty, action)
}

fun <T, R> StoreViewModelScope.bindComponentLifecycleViewModelProperty(
    clazz: KClass<R>,
    property: KProperty1<R, T>,
    action: T.(R) -> Unit
) where R : IStoreViewModel {
    withViewModel(clazz) {
        getPropertyLiveData(property).observe(environment.componentLifecycleOwner) {
            action(it, this@withViewModel)
        }
    }
}


fun <T, R> StoreViewModelScope.bindViewLifecycleViewModelProperty(
    valueOwner: R,
    valueProperty: KProperty1<R, T>,
    action: T.() -> Unit
) where R : IStoreViewModel {
    valueOwner.getPropertyLiveData(valueProperty).observe(environment.lifecycleOwner, action)
}

fun <T, R> StoreViewModelScope.bindViewLifecycleViewModelProperty(
    valueOwner: R,
    valueProperty: KProperty1<R, T>,
    vararg binders: DataBinder<T>
) where R : IStoreViewModel {
    binders.forEach { binder ->
        if (valueProperty is KMutableProperty1 && binder is DataReverseBinder) {
            binder.register(DataChangeListener {
                valueProperty.set(valueOwner, it)
            })
        }
        valueOwner.getPropertyLiveData(valueProperty).observe(environment.lifecycleOwner) {
            binder.bind(it)
        }
    }
}

fun <T, R> StoreViewModelScope.bindViewLifecycleViewModelProperty(
    clazz: KClass<R>,
    valueProperty: KProperty1<R, T>,
    action: T.() -> Unit
) where R : IStoreViewModel {
    get(clazz).getPropertyLiveData(valueProperty).observe(environment.lifecycleOwner, action)
}

fun <T, R> StoreViewModelScope.bindViewLifecycleViewModelProperty(
    clazz: KClass<R>,
    valueProperty: KProperty1<R, T>,
    vararg binders: DataBinder<T>
) where R : IStoreViewModel {
    binders.forEach { binder ->
        val valueOwner = get(clazz)
        if (valueProperty is KMutableProperty1 && binder is DataReverseBinder) {
            binder.register(DataChangeListener {
                valueProperty.set(valueOwner, it)
            })
        }
        valueOwner.getPropertyLiveData(valueProperty).observe(environment.lifecycleOwner) {
            binder.bind(it)
        }

    }
}

inline fun <T, reified R> StoreViewModelScope.bindViewLifecycleViewModelProperty(
    valueProperty: KProperty1<R, T>,
    noinline action: T.() -> Unit
) where R : IStoreViewModel {
    bindViewLifecycleViewModelProperty(R::class, valueProperty, action)
}

inline fun <T, reified R> StoreViewModelScope.bindViewLifecycleViewModelProperty(
    valueProperty: KProperty1<R, T>,
    vararg binders: DataBinder<T>
) where R : IStoreViewModel {
    bindViewLifecycleViewModelProperty(R::class, valueProperty, *binders)
}


fun <T> StoreViewModelScope.storeViewModel(clazz: KClass<T>): Lazy<T> where T : IStoreViewModel {
    return lifecycleAwareLazy(
        environment.componentLifecycleOwner,
        environment.lifecycleOwnerLiveData
    ) {
        get(clazz)
    }
}

fun <T> StoreViewModelScope.activityStoreViewModel(clazz: KClass<T>): Lazy<T> where T : IStoreViewModel {
    return lifecycleAwareLazy(
        environment.componentLifecycleOwner,
        environment.lifecycleOwnerLiveData
    ) {
        activityStoreViewModelScope[clazz]
    }
}

inline fun <reified T> StoreViewModelScope.storeViewModel(): Lazy<T> where T : IStoreViewModel =
    storeViewModel(T::class)

inline fun <reified T> StoreViewModelScope.activityStoreViewModel(): Lazy<T> where T : IStoreViewModel =
    activityStoreViewModel(T::class)


fun StoreViewModelScope.doOnBackPressedCallback(
    enable: Boolean,
    action: OnBackPressedCallback.(OnBackPressedDispatcher) -> Unit
) {
    fromViewModel {
        val key = "b315cdd1-0831-4f42-97f3-c07728ee6ede"
        setKeyRemoveOnDestroy(key)
        getOrCreate(key) {
            environment.hostActivity.onBackPressedDispatcher.apply {
                addCallback(environment.lifecycleOwner, object : OnBackPressedCallback(enable) {
                    override fun handleOnBackPressed() {
                        action(this@apply)
                    }
                })
            }
        }
    }
}

