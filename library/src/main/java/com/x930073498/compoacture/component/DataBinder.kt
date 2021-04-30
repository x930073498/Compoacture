package com.x930073498.compoacture.component

import android.view.View
import com.x930073498.compoacture.R
import com.x930073498.compoacture.defaultDataBinderFeature
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

interface DataBinderFeatureChecker {
    fun valid(
        dataBinder: DataBinder<*, *>,
        targetFeature: DataBinder.Feature
    ): Boolean
}

class DefaultDataBinderFeatureChecker internal constructor() : DataBinderFeatureChecker {
    override fun valid(dataBinder: DataBinder<*, *>, targetFeature: DataBinder.Feature): Boolean {
        if (featureEnableMap[targetFeature] == false) return false
        if (enableMap[dataBinder::class] == false) return false
        val mapFeature = featureMap[dataBinder::class]
        if (mapFeature != null) {
            return mapFeature == targetFeature
        }
        val features = dataBinder.features
        if (features.any { featureEnableMap[it] == false }) return false
        return features.firstOrNull { it == targetFeature } != null
    }

    private val featureMap = mutableMapOf<KClass<*>, DataBinder.Feature>()
    private val enableMap = mutableMapOf<KClass<*>, Boolean>()
    private val featureEnableMap = mutableMapOf<DataBinder.Feature, Boolean>()
    fun <T : DataBinder<*, *>> setFeature(
        binderClass: KClass<T>,
        feature: DataBinder.Feature
    ) {
        featureMap[binderClass] = feature
    }

    fun <T : DataBinder<*, *>> setBinderEnable(binderClass: KClass<T>, enable: Boolean) {
        enableMap[binderClass] = enable
    }

    fun setFeatureEnable(feature: DataBinder.Feature, enable: Boolean) {
        featureEnableMap[feature] = enable
    }
}


class BinderAgent internal constructor(private var base: BinderAgent? = null) {
    private var wrapper = DataBindingWrapper()

    internal fun reset(){
        base=null
        wrapper= DataBindingWrapper()
    }

    fun <V : Any, T : Any> addBinder(
        valueType: KClass<V>,
        targetType: KClass<T>,
        binder: DataBinder<V, T>
    ) {
        val binding = DataBinding(valueType, targetType, binder)
        val wrapper = DataBindingWrapper(binding)
        this.wrapper = DelegateDataBindingWrapper(wrapper, this.wrapper)
    }

    internal fun <V, T> bindData(
        value: V,
        target: T,
        feature: DataBinder.Feature,
        checker: DataBinderFeatureChecker,
        scope: StoreViewModelScope
    ): Boolean {
        val result = wrapper.bindData(value, target, feature, checker, scope)
        return base?.wrapper?.run {
            if (!result) bindData(
                value,
                target,
                feature,
                checker,
                scope
            ) else result
        }
            ?: result
    }


}

private class DelegateDataBindingWrapper(
    private val delegate: DataBindingWrapper,
    private val origin: DataBindingWrapper
) :
    DataBindingWrapper() {


    override fun bindData(
        value: Any?,
        target: Any?,
        feature: DataBinder.Feature,
        checker: DataBinderFeatureChecker,
        scope: StoreViewModelScope,

        ): Boolean {
        return delegate.bindData(
            value,
            target,
            feature,
            checker,
            scope,

            ) || origin.bindData(
            value,
            target,
            feature,
            checker,
            scope,
        )
    }
}


open class DataBindingWrapper internal constructor(private val binding: DataBinding<*, *>? = null) {


    open fun bindData(
        value: Any?,
        target: Any?,
        feature: DataBinder.Feature,
        checker: DataBinderFeatureChecker,
        scope: StoreViewModelScope
    ): Boolean {
        return binding?.bindData(value, target, feature, scope, checker) ?: false
    }
}


@Suppress("UNCHECKED_CAST")
open class DataBinding<V : Any, T : Any> internal constructor(
    private val valueType: KClass<V>,
    private val targetType: KClass<T>,
    private val dataBinder: DataBinder<V, T>,
) {

    fun bindData(
        value: Any?,
        target: Any?,
        feature: DataBinder.Feature,
        scope: StoreViewModelScope,
        featureProvider: DataBinderFeatureChecker
    ): Boolean {
        if (target == null) return false
        if (!target::class.isSubclassOf(targetType)) return false
        if (!featureProvider.valid(dataBinder, feature)) return false
        if (value == null) {
            runCatching {
                dataBinder.bind(
                    null,
                    target as T,
                    scope
                )
                if (target is View) {
                    target.setTag(R.id.data_binder_reverse_tag, null)
                }
            }.onFailure { return false }
            return true
        } else {
            if (value::class.isSubclassOf(valueType)) {
                runCatching {
                    dataBinder.bind(
                        value as V,
                        target as T,
                        scope
                    )
                    if (target is View) {
                        target.setTag(R.id.data_binder_reverse_tag, value)
                    }
                }.onFailure { return false }
                return true
            }
            return false
        }
    }

}


inline fun <reified V : Any, reified T : Any> BinderAgent.addBinder(binder: DataBinder<V, T>) {
    addBinder(V::class, T::class, binder)
}

internal fun BinderAgent.setValue(
    valueProperty: KProperty0<*>,
    targetProperty: KProperty0<*>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker,
    scope: StoreViewModelScope
) {
    val value = valueProperty.get() ?: return
    val target = targetProperty.get() ?: return
    bindData(value, target, feature, checker, scope)
}

internal fun <T, V> BinderAgent.setValue(
    valueOwner: T,
    valueProperty: KProperty1<T, V>,
    targetProperty: KProperty0<*>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker,
    scope: StoreViewModelScope
) {
    val value = valueProperty.get(valueOwner) ?: return
    val target = targetProperty.get() ?: return
    bindData(value, target, feature, checker, scope)
}

internal fun <T, V, S, R> BinderAgent.setValue(
    valueOwner: T,
    valueProperty: KProperty1<T, V>,
    targetOwner: S,
    targetProperty: KProperty1<S, R>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker,
    scope: StoreViewModelScope
) {
    val value = valueProperty.get(valueOwner) ?: return
    val target = targetProperty.get(targetOwner) ?: return
    bindData(value, target, feature, checker, scope)
}

internal fun <V, T> BinderAgent.setValue(
    value: V,
    targetProperty: KProperty0<T>,
    feature: DataBinder.Feature = defaultDataBinderFeature,
    checker: DataBinderFeatureChecker,
    scope: StoreViewModelScope
) {
    val target = targetProperty.get() ?: return
    bindData(value, target, feature, checker, scope)
}


interface DataBinder<V, R> {
    val features: List<Feature>
        get() = arrayListOf(defaultDataBinderFeature)

    fun bind(
        value: V?,
        target: R,
        scope: StoreViewModelScope
    )

    interface Feature {
        companion object : Feature
    }

}

interface DataReverseBinder<V, R> : DataBinder<V, R> {
    fun value(target: R, scope: StoreViewModelScope): V?
}

interface ViewDataReverseBinder<V, T : View> : DataReverseBinder<V, T> {
    override fun value(target: T, scope: StoreViewModelScope): V? {
        @Suppress("UNCHECKED_CAST")
        return target.getTag(R.id.data_binder_reverse_tag) as? V
    }
}




