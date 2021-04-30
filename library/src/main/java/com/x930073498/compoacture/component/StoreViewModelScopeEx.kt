package com.x930073498.compoacture.component

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.x930073498.compoacture.application
import com.x930073498.compoacture.defaultDataBinderFeature
import com.x930073498.compoacture.defaultDataBinderHandle
import com.x930073498.compoacture.internal.lifecycleAwareLazy
import com.x930073498.compoacture.utils.invokeOnMain
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

val StoreViewModelScope.storeViewModel: IStoreViewModel
    get() {
        return getBaseViewModel(environment, environment.extras).apply {
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

internal val StoreViewModelScope.dataBinderHandle: BinderAgent
    get() {
        return storeViewModel.getOrCreate("97786091-d712-41ed-abaa-8cdf94982d42") {
            BinderAgent(defaultDataBinderHandle)
        }
    }
val StoreViewModelScope.dataBinderFeatureChecker: DefaultDataBinderFeatureChecker
    get() {
        return storeViewModel.getOrCreate("ed74e6e4-f0e5-4e69-8901-b795624dde73") {
            DefaultDataBinderFeatureChecker()
        }
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
                lifecycleActionLiveData.observe(environment.lifecycleOwner) {
                    val action = it.action as Action<Any?>
                    val handle = it.handle as ActionHandle<Any?>
                    handle.complete(action.action(this, this@getViewModel))
                }
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

fun <T, S> StoreViewModelScope.bindViewLifecycle(
    property: KProperty0<T>,
    action: S.() -> Unit
) where  T : LiveData<S> {
    property.invoke().observe(environment.lifecycleOwner) {
        action(it)
    }
}


fun <T, S> StoreViewModelScope.bindViewLifecycle(
    liveData: T,
    action: S.() -> Unit
) where T : LiveData<S> {
    liveData.observe(environment.lifecycleOwner, action)
}


fun <T, S> StoreViewModelScope.bindComponentLifecycle(
    liveData: T,
    action: S.() -> Unit
) where T : LiveData<S> {
    liveData.observe(environment.componentLifecycleOwner, action)
}

inline fun <T, reified R, S> StoreViewModelScope.bindComponentLifecycle(
    property: KProperty1<R, T>,
    noinline action: S.(R) -> Unit
) where R : IStoreViewModel, T : LiveData<S> {
    bindComponentLifecycle(R::class, property, action)
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

fun <T, S> StoreViewModelScope.bindComponentLifecycle(
    property: KProperty0<T>,
    action: S.() -> Unit
) where T : LiveData<S> {
    property.invoke().observe(environment.componentLifecycleOwner) {
        action(it)
    }
}


fun <T, V, R, S> StoreViewModelScope.bindViewLifecycle(
    clazz: KClass<R>,
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where R : IStoreViewModel, T : LiveData<S> {
    val viewModel = get(clazz)
    val value = valueProperty.get(viewModel)
    value.observe(environment.lifecycleOwner) {
        dataBinderHandle.setValue(transform(it), targetProperty, feature, checker, this)
    }
}

fun <T, V, R, S> StoreViewModelScope.bindViewLifecycle(
    value: R,
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where R : IStoreViewModel, T : LiveData<S> {
    val liveData = valueProperty.get(value)
    liveData.observe(environment.lifecycleOwner) {
        dataBinderHandle.setValue(transform(it), targetProperty, feature, checker, this)
    }
}

fun <T, V, R, S> StoreViewModelScope.bindViewLifecycle(
    value: R,
    valueProperty: KProperty1<R, T>,
    target: V,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where R : IStoreViewModel, T : LiveData<S> {
    val liveData = valueProperty.get(value)
    liveData.observe(environment.lifecycleOwner) {
        dataBinderHandle.bindData(transform(it), target, feature, checker, this)
    }
}

fun <T, V, S> StoreViewModelScope.bindViewLifecycle(
    valueProperty: KProperty0<T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where T : LiveData<S> {
    val value = valueProperty.get()
    value.observe(environment.lifecycleOwner) {
        dataBinderHandle.bindData(transform(it), targetProperty.get(), feature, checker, this)
    }
}

fun <T, V, S> StoreViewModelScope.bindViewLifecycle(
    valueProperty: KProperty0<T>,
    target: V,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where T : LiveData<S> {
    val value = valueProperty.get()
    value.observe(environment.lifecycleOwner) {
        dataBinderHandle.bindData(transform(it), target, feature, checker, this)
    }
}

fun <T, V, S> StoreViewModelScope.bindViewLifecycle(
    value: T,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where T : LiveData<S> {
    value.observe(environment.lifecycleOwner) {
        dataBinderHandle.setValue(transform(it), targetProperty, feature, checker, this)
    }
}

fun <T, V, S> StoreViewModelScope.bindViewLifecycle(
    value: T,
    target: V,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where T : LiveData<S> {
    value.observe(environment.lifecycleOwner) {
        dataBinderHandle.bindData(transform(it), target, feature, checker, this)
    }
}

inline fun <T, V, reified R, S> StoreViewModelScope.bindViewLifecycle(
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    noinline transform: ((S) -> Any?) = { it }
) where R : IStoreViewModel, T : LiveData<S> {
    bindViewLifecycle(R::class, valueProperty, targetProperty, feature, checker, transform)
}


fun <T, V, R, S> StoreViewModelScope.bindComponentLifecycle(
    clazz: KClass<R>,
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where R : IStoreViewModel, T : LiveData<S> {
    val viewModel = get(clazz)
    val value = valueProperty.get(viewModel)
    value.observe(environment.componentLifecycleOwner) {
        dataBinderHandle.setValue(transform(it), targetProperty, feature, checker, this)
    }
}

fun <T, V, R, S> StoreViewModelScope.bindComponentLifecycle(
    value: R,
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where R : IStoreViewModel, T : LiveData<S> {
    val liveData = valueProperty.get(value)
    liveData.observe(environment.componentLifecycleOwner) {
        dataBinderHandle.setValue(transform(it), targetProperty, feature, checker, this)
    }
}

fun <T, V, R, S> StoreViewModelScope.bindComponentLifecycle(
    value: R,
    valueProperty: KProperty1<R, T>,
    target: V,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where R : IStoreViewModel, T : LiveData<S> {
    val liveData = valueProperty.get(value)
    liveData.observe(environment.componentLifecycleOwner) {
        dataBinderHandle.bindData(transform(it), target, feature, checker, this)
    }
}

fun <T, V, S> StoreViewModelScope.bindComponentLifecycle(
    valueProperty: KProperty0<T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where  T : LiveData<S> {
    val value = valueProperty.get()
    value.observe(environment.componentLifecycleOwner) {
        dataBinderHandle.setValue(transform(it), targetProperty, feature, checker, this)
    }
}

fun <T, V, S> StoreViewModelScope.bindComponentLifecycle(
    valueProperty: KProperty0<T>,
    target: V,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where  T : LiveData<S> {
    val value = valueProperty.get()
    value.observe(environment.componentLifecycleOwner) {
        dataBinderHandle.bindData(transform(it), target, feature, checker, this)
    }
}

fun <T, V, S> StoreViewModelScope.bindComponentLifecycle(
    value: T,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where  T : LiveData<S> {
    value.observe(environment.componentLifecycleOwner) {
        dataBinderHandle.setValue(transform(it), targetProperty, feature, checker, this)
    }
}

fun <T, V, S> StoreViewModelScope.bindComponentLifecycle(
    value: T,
    target: V,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((S) -> Any?) = { it }
) where  T : LiveData<S> {
    value.observe(environment.componentLifecycleOwner) {
        dataBinderHandle.bindData(transform(it), target, feature, checker, this)
    }
}

inline fun <T, V, reified R, S> StoreViewModelScope.bindComponentLifecycle(
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    noinline transform: ((S) -> Any?) = { it }
) where R : IStoreViewModel, T : LiveData<S> {
    bindComponentLifecycle(R::class, valueProperty, targetProperty, feature, checker, transform)
}


inline fun <T, reified R> StoreViewModelScope.bindViewLifecycleProperty(
    property: KProperty1<R, T>,
    noinline action: T.(R) -> Unit = {}
) where R : IStoreViewModel {
    bindViewLifecycleProperty(R::class, property, action)
}

fun <T, R> StoreViewModelScope.bindViewLifecycleProperty(
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


inline fun <T, reified R> StoreViewModelScope.bindComponentLifecycleProperty(
    property: KProperty1<R, T>,
    noinline action: T.(R) -> Unit
) where R : IStoreViewModel {
    bindComponentLifecycleProperty(R::class, property, action)
}

fun <T, R> StoreViewModelScope.bindComponentLifecycleProperty(
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


fun <T, V, R> StoreViewModelScope.bindViewLifecycleProperty(
    clazz: KClass<R>,
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((T) -> Any?) = { it }
) where R : IStoreViewModel {
    val viewModel = get(clazz)
    viewModel.getPropertyLiveData(valueProperty).observe(environment.lifecycleOwner) {
        dataBinderHandle.bindData(transform(it), targetProperty.get(), feature, checker, this)
    }
}


inline fun <T, V, reified R> StoreViewModelScope.bindViewLifecycleProperty(
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    noinline transform: ((T) -> Any?) = { it }
) where R : IStoreViewModel {
    bindViewLifecycleProperty(R::class, valueProperty, targetProperty, feature, checker, transform)
}

fun <T, V, R> StoreViewModelScope.bindViewLifecycleProperty(
    valueOwner: R,
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((T) -> Any?) = { it }
) where R : IStoreViewModel {
    valueOwner.getPropertyLiveData(valueProperty).observe(environment.lifecycleOwner) {
        dataBinderHandle.bindData(transform(it), targetProperty.get(), feature, checker, this)
    }

}

fun <T, V, R> StoreViewModelScope.bindViewLifecycleProperty(
    valueOwner: R,
    valueProperty: KProperty1<R, T>,
    target: V,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((T) -> Any?) = { it }
) where R : IStoreViewModel {
    valueOwner.getPropertyLiveData(valueProperty).observe(environment.lifecycleOwner) {
        dataBinderHandle.bindData(transform(it), target, feature, checker, this)
    }
}


fun <T, V, R> StoreViewModelScope.bindComponentLifecycleProperty(
    clazz: KClass<R>,
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: ((T) -> Any?) = { it }
) where R : IStoreViewModel {
    val viewModel = get(clazz)
    viewModel.getPropertyLiveData(valueProperty).observe(environment.componentLifecycleOwner) {
        dataBinderHandle.bindData(transform(it), targetProperty.get(), feature, checker, this)
    }
}

inline fun <T, V, reified R> StoreViewModelScope.bindComponentLifecycleProperty(
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<V>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    noinline transform: ((T) -> Any?) = { it }
) where R : IStoreViewModel {
    bindComponentLifecycleProperty(
        R::class,
        valueProperty,
        targetProperty,
        feature,
        checker,
        transform
    )
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


fun <V : Any, T : Any> StoreViewModelScope.addDataBinder(
    valueType: KClass<V>,
    targetType: KClass<T>,
    binder: DataBinder<V, T>
) {
    dataBinderHandle.addBinder(valueType, targetType, binder)
}

inline fun <reified T : Any, reified V : Any> StoreViewModelScope.addDataBinder(binder: DataBinder<T, V>) {
    addDataBinder(T::class, V::class, binder)
}

fun StoreViewModelScope.resetDataBinder() {
    dataBinderHandle.reset()
}

fun <T : DataBinder<*, *>> StoreViewModelScope.setBinderFeature(
    binderClass: KClass<T>,
    feature: DataBinder.Feature
) {
    dataBinderFeatureChecker.setFeature(binderClass, feature)
}

fun <T : DataBinder<*, *>> StoreViewModelScope.setBinderEnable(
    binderClass: KClass<T>,
    enable: Boolean
) {
    dataBinderFeatureChecker.setBinderEnable(binderClass, enable)
}

fun StoreViewModelScope.setFeatureEnbale(
    feature: DataBinder.Feature,
    enable: Boolean
) {
    dataBinderFeatureChecker.setFeatureEnable(feature, enable)
}

fun <T> StoreViewModelScope.setData(
    value: T,
    target: Any?,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: (T) -> Any? = { it }
) {
    dataBinderHandle.bindData(transform(value), target, feature, checker, this)
}

fun <T> StoreViewModelScope.setData(
    value: T,
    targetProperty: KProperty0<*>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: (T) -> Any? = { it }
) {
    dataBinderHandle.bindData(transform(value), targetProperty.get(), feature, checker, this)
}

fun <T, R : IStoreViewModel> StoreViewModelScope.setData(
    valueOwner: R,
    property: KProperty1<R, T>,
    target: Any?,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: (T) -> Any? = { it }
) {
    dataBinderHandle.bindData(transform(property.get(valueOwner)), target, feature, checker, this)
}

fun <T, R : IStoreViewModel> StoreViewModelScope.setData(
    clazz: KClass<R>,
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<*>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: (T) -> Any? = { it }
) {
    dataBinderHandle.setValue(
        transform(valueProperty.get(get(clazz))),
        targetProperty,
        feature,
        checker,
        this
    )
}

inline fun <T, reified R : IStoreViewModel> StoreViewModelScope.setData(
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<*>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    noinline transform: (T) -> Any? = { it }
) {
    setData(R::class, valueProperty, targetProperty, feature, checker, transform)
}

fun <T, R : IStoreViewModel> StoreViewModelScope.setData(
    valueOwner: R,
    valueProperty: KProperty1<R, T>,
    targetProperty: KProperty0<*>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker = dataBinderFeatureChecker,
    transform: (T) -> Any? = { it }
) {
    dataBinderHandle.setValue(
        transform(valueProperty.get(valueOwner)),
        targetProperty,
        feature,
        checker,
        this
    )
}

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

