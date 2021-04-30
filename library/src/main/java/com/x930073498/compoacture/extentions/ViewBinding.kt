package com.x930073498.compoacture.extentions


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.IdRes
import androidx.annotation.RestrictTo
import androidx.collection.LruCache
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.viewbinding.ViewBinding
import com.x930073498.compoacture.utils.invokeOnMain
import java.lang.reflect.Method
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


inline fun <T, reified VB> T.viewBinding(
    setContent: Boolean = true,
    noinline create: ViewBindingStore<T, VB>.() -> VB = {
        inflate(
            layoutInflater,
            null,
            false
        )
    }
): ReadOnlyProperty<T, VB> where VB : ViewBinding, T : ComponentActivity {
    return viewBinding(setContent, VB::class.java, create)
}

inline fun <T, reified VB> T.viewBinding(crossinline create: ViewBindingStore<T, VB>.() -> VB = { bind() }): ReadOnlyProperty<T, VB> where VB : ViewBinding, T : Fragment {
    return viewBinding(lifecycle = FragmentViewBindingLifecycle(), create)
}


inline fun <T, reified VB> T.viewBinding(@IdRes id: Int): ReadOnlyProperty<T, VB> where VB : ViewBinding, T : Fragment {
    return viewBinding(lifecycle = FragmentViewBindingLifecycle()) { bind(id) }
}

inline fun <T, reified VB> T.viewBinding(
    lifecycle: ViewBindingLifecycle<T, VB> = ViewBindingLifecycle.none(),
    crossinline create: ViewBindingStore<T, VB>.() -> VB
): ReadOnlyProperty<T, VB> where VB : ViewBinding {
    return viewBinding(VB::class.java, ViewBindingFactory.create(lifecycle) { create(this) })
}

inline fun <T, reified VB> T.viewBinding(factory: ViewBindingFactory<T, VB>): ReadOnlyProperty<T, VB> where VB : ViewBinding {
    return viewBinding(VB::class.java, factory)
}

fun <T, VB> T.viewBinding(
    setContent: Boolean = true,
    clazz: Class<VB>,
    create: (ViewBindingStore<T, VB>.() -> VB)? = null
): ReadOnlyProperty<T, VB> where VB : ViewBinding, T : ComponentActivity {
    var hasSetContent = false
    fun setCurrentContentView(binding: VB) {
        if (!setContent) return
        if (!hasSetContent) {
            hasSetContent = true
            setContentView(binding.root)
        }
    }

    val creator: ViewBindingStore<T, VB>.() -> VB = {
        (create?.invoke(this) ?: inflate(layoutInflater, null, false)).apply {
            setCurrentContentView(this)
        }
    }

    return viewBinding(clazz, ViewBindingFactory.create({ target, handle ->
        if (setContent) {
            if (!target.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) return@create
            target.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    handle.create()
                }
            })
        }
    }, creator))
}

fun <T, VB> T.viewBinding(
    clazz: Class<VB>,
    factory: ViewBindingFactory<T, VB>
): ReadOnlyProperty<T, VB> where VB : ViewBinding {
    return ViewBindingDelegate(this, clazz, factory)
}


internal class DelegateViewBindingFactory<T, VB>(
    lifecycle: ViewBindingLifecycle<T, VB> = ViewBindingLifecycle.none(),
    creator: ViewBindingCreator<T, VB>
) : ViewBindingCreator<T, VB> by creator, ViewBindingLifecycle<T, VB> by lifecycle,
    ViewBindingFactory<T, VB> where VB : ViewBinding

interface ViewBindingFactory<T, VB> : ViewBindingCreator<T, VB>,
    ViewBindingLifecycle<T, VB> where VB : ViewBinding {
    companion object {
        fun <T, VB> create(
            lifecycle: ViewBindingLifecycle<T, VB> = ViewBindingLifecycle.none(),
            create: ViewBindingStore<T, VB>.() -> VB
        ): ViewBindingFactory<T, VB> where VB : ViewBinding {
            return DelegateViewBindingFactory(lifecycle, create)
        }
    }

    interface ViewBindingHandle<VB> where VB : ViewBinding {
        val binding: VB?
        fun destroy()
        fun create()
    }
}

fun interface ViewBindingCreator<T, VB> where VB : ViewBinding {
    fun create(store: ViewBindingStore<T, VB>): VB
}

internal class ClassViewBindingStore<T, VB>(private val clazz: Class<VB>, override val target: T) :
    ViewBindingStore<T, VB> where VB : ViewBinding {

    override fun inflate(
        layoutInflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ): VB {
        return ViewBindingCache.getInflateWithLayoutInflater(clazz)
            .inflate(layoutInflater, parent, attachToParent)
    }

    override fun bind(view: View): VB {
        return ViewBindingCache.getBind(clazz).bind(view)
    }

}

interface ViewBindingStore<T, VB> where VB : ViewBinding {
    val target: T
    fun inflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean): VB
    fun bind(view: View): VB
}

fun <T, VB> ViewBindingStore<T, VB>.inflate(layoutInflater: LayoutInflater): VB where VB : ViewBinding {
    return inflate(layoutInflater, null, false)
}

val <T : Fragment, VB : ViewBinding> ViewBindingStore<T, VB>.view: View
    get() {
        return target.requireView()
    }

fun <T : Fragment, VB : ViewBinding> ViewBindingStore<T, VB>.bind(): VB = bind(view)
fun <T : Fragment, VB : ViewBinding> ViewBindingStore<T, VB>.bind(@IdRes id: Int): VB =
    runOnView(id) { bind(this) }

fun <T : Fragment, VB : ViewBinding> ViewBindingStore<T, VB>.inflate(): VB = inflate(layoutInflater)

val <T : Fragment, VB : ViewBinding> ViewBindingStore<T, VB>.layoutInflater: LayoutInflater
    get() {
        return target.layoutInflater
    }
val <T : Fragment, VB : ViewBinding> ViewBindingStore<T, VB>.lifecycleOwner: LifecycleOwner
    get() {
        return target
    }
val <T : Fragment, VB : ViewBinding> ViewBindingStore<T, VB>.viewLifecycleOwner: LifecycleOwner
    get() {
        return target.viewLifecycleOwner
    }
val <T : Fragment, VB : ViewBinding> ViewBindingStore<T, VB>.viewLifecycleOwnerLiveData: LiveData<LifecycleOwner>
    get() {
        return target.viewLifecycleOwnerLiveData
    }

fun <T : Fragment, VB : ViewBinding, R> ViewBindingStore<T, VB>.runOnView(block: View.() -> R): R {
    return block(view)
}

fun <T : Fragment, VB : ViewBinding, R> ViewBindingStore<T, VB>.runOnView(
    @IdRes id: Int,
    block: View.() -> R
): R {
    return block(view.requireViewWithId(id))
}


fun interface ViewBindingLifecycle<T, VB> where VB : ViewBinding {
    fun lifecycle(target: T, handle: ViewBindingFactory.ViewBindingHandle<VB>)

    companion object {
        fun <T, VB> none(): ViewBindingLifecycle<T, VB> where VB : ViewBinding {
            return ViewBindingLifecycle { _, _ -> }
        }
    }

}


class FragmentViewBindingLifecycle<T, VB> :
    ViewBindingLifecycle<T, VB> where T : Fragment, VB : ViewBinding {
    override fun lifecycle(target: T, handle: ViewBindingFactory.ViewBindingHandle<VB>) {
        if (!target.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) return
        target.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                target.viewLifecycleOwnerLiveData.observe(owner) {
                    it.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            handle.destroy()
                        }
                    })
                }
            }
        })
    }
}

internal class ViewBindingDelegate<T, VB>(
    target: T,
    clazz: Class<VB>,
    private val factory: ViewBindingFactory<T, VB>
) : ReadOnlyProperty<T, VB>,
    ViewBindingFactory.ViewBindingHandle<VB> where VB : ViewBinding {

    private var _binding: VB? = null
    private val store = ClassViewBindingStore(clazz, target)

    init {
        invokeOnMain { factory.lifecycle(target, this) }
    }

    override fun getValue(thisRef: T, property: KProperty<*>): VB {
        return createOrGet()
    }

    @Synchronized
    private fun createOrGet(): VB {
        val binding = _binding
        if (binding != null) {
            return binding
        }
        return factory.create(store).also {
            this._binding = it
        }
    }

    override fun destroy() {
        _binding = null
    }

    override val binding by ::_binding


    override fun create() {
        createOrGet()
    }

}


public inline fun <K : Any, V : Any> LruCache<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        put(key, answer)
        answer
    } else {
        value
    }
}


internal object ViewBindingCache {

    private val inflateCache =
        LruCache<Class<out ViewBinding>, InflateViewBinding<ViewBinding>>(100)
    private val bindCache = LruCache<Class<out ViewBinding>, BindViewBinding<ViewBinding>>(100)

    @Suppress("UNCHECKED_CAST")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun <T : ViewBinding> getInflateWithLayoutInflater(viewBindingClass: Class<T>): InflateViewBinding<T> {
        return inflateCache.getOrPut(viewBindingClass) { InflateViewBinding(viewBindingClass) } as InflateViewBinding<T>
    }

    @Suppress("UNCHECKED_CAST")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun <T : ViewBinding> getBind(viewBindingClass: Class<T>): BindViewBinding<T> {
        return bindCache.getOrPut(viewBindingClass) { BindViewBinding(viewBindingClass) } as BindViewBinding<T>
    }

    /**
     * Reset all cached data
     */
    fun clear() {
        inflateCache.evictAll()
        bindCache.evictAll()
    }
}

/**
 * Wrapper of ViewBinding.inflate(LayoutInflater, ViewGroup, Boolean)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class InflateViewBinding<out VB : ViewBinding>(
    private val inflateViewBinding: Method
) {

    @Suppress("UNCHECKED_CAST")
    abstract fun inflate(
        layoutInflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ): VB
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("FunctionName")
internal fun <VB : ViewBinding> InflateViewBinding(viewBindingClass: Class<VB>): InflateViewBinding<VB> {
    return try {
        val method = viewBindingClass.getMethod(
            "inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java
        )
        FullInflateViewBinding(method)
    } catch (e: NoSuchMethodException) {
        val method =
            viewBindingClass.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java)
        MergeInflateViewBinding(method)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class FullInflateViewBinding<out VB : ViewBinding>(
    private val inflateViewBinding: Method
) : InflateViewBinding<VB>(inflateViewBinding) {

    @Suppress("UNCHECKED_CAST")
    override fun inflate(
        layoutInflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ): VB {
        return inflateViewBinding(null, layoutInflater, parent, attachToParent) as VB
    }
}


@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class MergeInflateViewBinding<out VB : ViewBinding>(
    private val inflateViewBinding: Method
) : InflateViewBinding<VB>(inflateViewBinding) {

    @Suppress("UNCHECKED_CAST")
    override fun inflate(
        layoutInflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ): VB {
        require(attachToParent) {
            "${InflateViewBinding::class.java.simpleName} supports inflate only with attachToParent=true"
        }
        return inflateViewBinding(null, layoutInflater, parent) as VB
    }
}

/**
 * Wrapper of ViewBinding.bind(View)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class BindViewBinding<out VB : ViewBinding>(viewBindingClass: Class<VB>) {

    private val bindViewBinding = viewBindingClass.getMethod("bind", View::class.java)

    @Suppress("UNCHECKED_CAST")
    fun bind(view: View): VB {
        return bindViewBinding(null, view) as VB
    }
}


