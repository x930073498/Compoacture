@file:Suppress("unused", "LocalVariableName", "MemberVisibilityCanBePrivate")

package com.x930073498.compoacture.dialog

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.collection.SparseArrayCompat
import androidx.collection.arrayMapOf
import androidx.collection.set
import androidx.collection.valueIterator
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.x930073498.compoacture.application
import com.x930073498.compoacture.component.*
import com.x930073498.compoacture.currentActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import per.goweii.anylayer.Layer
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.PriorityBlockingQueue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val mainHandler by lazy {
    Handler(Looper.getMainLooper())
}

fun <R> invokeOnMain(block: () -> R): R {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        return block()
    }
    var result: R? = null
    val countDownLatch = CountDownLatch(1)
    mainHandler.post {
        result = block()
        countDownLatch.countDown()
    }
    countDownLatch.await()
    return result!!

}

/**
 * 要求：
 * 1、单一activity具有唯一作用域
 * 2、具有管道概念，单一管道具有唯一队列
 * 3、每个dialog具有生命周期,dialog随lifecycle 切换到后台后，不能操作dialog，否则会出现异常
 */

/**
 * 全局作用域
 */
fun DialogManager.requestApplicationStore(): IDialogStore {
    return requestStore(ProcessLifecycleOwner.get())
}

/**
 * activity作用域
 */
fun DialogManager.requestActivityStore(fragment: Fragment): IDialogStore {
    return requestStore(fragment.requireActivity())
}

fun DialogManager.show(environmentBuilder: EnvironmentBuilder.() -> Unit): DialogHandle {
    val builder = InternalEnvironmentBuilder()
    environmentBuilder(builder)
    return builder.show()
}

fun DialogManager.show(environment: Environment, factory: suspend FactoryParams.() -> IDialog) =
    show {
        environment(environment)
        content {
            factory()
        }
        scope(environment.base)
    }

fun IDialogStore.show(environmentBuilder: StoreEnvironmentBuilder.() -> Unit): DialogHandle {
    val builder = InternalEnvironmentBuilder()
    environmentBuilder(builder)
    return builder.show(this)
}

object DialogManager {


    /**
     * LifecycleOwner 作用域
     */
    fun requestStore(lifecycleOwner: LifecycleOwner): IDialogStore {
        return DialogStore.get(lifecycleOwner)
    }

    fun requestStore(scope: StoreViewModelScope): IDialogStore {
        return DialogStore.get(scope.environment.lifecycleOwner)
    }


    fun show(environment: Environment, factory: DialogFactory) = show {
        environment(environment)
        content {
            factory.createDialog(environment, handle)
        }

    }

}

fun IEnvironment.attach(base: LifecycleOwner): Environment {
    return Environment.create(this, base)
}


fun EnvironmentProperty.withComponent(
    component: FragmentManagerEnvironmentComponent,
    change: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
): IEnvironment {
    if (this is EnvironmentPropertyWithComponent) {
        return withComponent(component as FragmentManagerEnvironmentComponent?, change)
    }
    if (component is PriorityEnvironmentComponent) {
        return withComponent(component, change)
    }
    val result = DelegateEnvironmentComponentBuilder(component)
    change(result)
    return DelegateIEnvironment(component = result, property = this)
}

fun EnvironmentPropertyWithComponent.withComponent(
    component: FragmentManagerEnvironmentComponent? = null,
    change: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
): IEnvironment {
    val result = DelegateEnvironmentComponentBuilder(component ?: this)
    change(result)
    return DelegateIEnvironment(component = result, property = this)
}

fun EnvironmentPropertyWithComponent.attach(base: LifecycleOwner): IEnvironment {
    return withComponent().attach(base)
}

fun EnvironmentPropertyWithComponent.attach(): IEnvironment {
    return withComponent().attach(lifecycleOwner)
}

fun EnvironmentProperty.withComponent(
    fragment: Fragment,
    change: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
) = withComponent(FragmentManagerEnvironmentComponent.from(fragment, change))

fun EnvironmentProperty.withComponent(
    activity: FragmentActivity,
    change: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
) = withComponent(FragmentManagerEnvironmentComponent.from(activity, change))


fun EnvironmentProperty.withComponent(
    component: PriorityEnvironmentComponent,
    change: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
): IEnvironment {
    val property =
        if (this is EnvironmentProperty.Default) DelegateEnvironmentPropertyBuilder(this).apply {
            this.priority = component.priority
        } else this
    val result = DelegateEnvironmentComponentBuilder(component)
    change(result)
    return DelegateIEnvironment(property, result)
}

fun PriorityEnvironmentComponent.withProperty(
    property: EnvironmentProperty = EnvironmentProperty.create(),
    change: EnvironmentPropertyBuilder.() -> Unit = {}
): IEnvironment {
    val builder = DelegateEnvironmentPropertyBuilder(property)
    if (property is EnvironmentProperty.Default) {
        builder.priority = priority
    }
    change(builder)
    return DelegateIEnvironment(component = this, property = builder)
}

fun PriorityEnvironmentComponent.attach(base: LifecycleOwner): IEnvironment {
    return withProperty().attach(base)
}

fun PriorityEnvironmentComponent.attach(): IEnvironment {
    return withProperty().attach(lifecycleOwner)
}

fun FragmentManagerEnvironmentComponent.withProperty(
    property: EnvironmentProperty = EnvironmentProperty.create(),
    change: EnvironmentPropertyBuilder.() -> Unit = {}
): IEnvironment {
    val builder = DelegateEnvironmentPropertyBuilder(property)
    change(builder)
    return DelegateIEnvironment(builder, this)
}

/**
 * 环境组件
 */
interface EnvironmentComponent {
    val context: Context
    val lifecycleOwner: LifecycleOwner
}


interface FragmentManagerEnvironmentComponent : EnvironmentComponent {
    val fragmentManager: FragmentManager?


    companion object {
        fun from(
            fragment: Fragment,
            change: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
        ): PriorityEnvironmentComponent {
            val delegate = object : FragmentManagerEnvironmentComponent {
                override val context: Context
                    get() = fragment.requireContext()
                override val fragmentManager: FragmentManager
                    get() = fragment.childFragmentManager
                override val lifecycleOwner: LifecycleOwner
                    get() = fragment.viewLifecycleOwnerLiveData.value ?: fragment
            }
            val result = DelegatePriorityEnvironmentComponentBuilder(delegate, fragment.priority)
            change(result)
            return result
        }

        fun application(change: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}): PriorityEnvironmentComponent {
            val delegate = object : FragmentManagerEnvironmentComponent {
                override val context: Context
                    get() = com.x930073498.compoacture.application
                override val fragmentManager: FragmentManager?
                    get() = null
                override val lifecycleOwner: LifecycleOwner
                    get() = ProcessLifecycleOwner.get()
            }
            val result = DelegatePriorityEnvironmentComponentBuilder(delegate, -1)
            change(result)
            return result
        }

        fun from(
            activity: FragmentActivity,
            change: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
        ): PriorityEnvironmentComponent {
            val delegate = object : FragmentManagerEnvironmentComponent {
                override val context: Context
                    get() = activity
                override val fragmentManager: FragmentManager
                    get() = activity.supportFragmentManager
                override val lifecycleOwner: LifecycleOwner
                    get() = activity
            }
            val result = DelegatePriorityEnvironmentComponentBuilder(delegate, activity.priority)
            change(result)
            return result
        }
    }
}

interface PriorityEnvironmentComponent : FragmentManagerEnvironmentComponent {
    val priority: Int
}

private class DelegatePriorityEnvironmentComponentBuilder constructor(
    component: FragmentManagerEnvironmentComponent,
    mPriority: Int
) : PriorityEnvironmentComponent, FragmentManagerEnvironmentComponentBuilder(),
    PropertyChangeListener {
    override var context: Context by listenProperty(component.context)
    override var fragmentManager: FragmentManager? by listenProperty(component.fragmentManager)

    override var lifecycleOwner: LifecycleOwner by listenProperty(component.lifecycleOwner)

    override val priority: Int by listenProperty(mPriority)


    override fun onPropertyChanged() {

    }
}


interface EnvironmentPropertyWithComponent : EnvironmentProperty,
    FragmentManagerEnvironmentComponent


/**
 * 可编辑component
 */
private class DelegateEnvironmentPropertyWithComponent(
    component: FragmentManagerEnvironmentComponent,
    property: EnvironmentProperty
) : EnvironmentPropertyWithComponent, EnvironmentProperty by property,
    FragmentManagerEnvironmentComponent by component

/**
 * 环境属性
 */
interface EnvironmentProperty : ID, LayerChannel {
    val tag: String//给dialog打标签
    val displayTime: Long//dialog展示时间（毫秒），例如设置2000，则表示dialog展示2秒后（非精确）自动消失，
    val timeout: Long//dialog等待超时时间（毫秒），如指定时间内没有展示则不再展示
    val timestamp: Long//dialog 环境的时间戳，指环境的创建时间，毫秒
    val priority: Int


    open class Empty : EnvironmentProperty {
        override val tag: String
            get() = UUID.randomUUID().toString()
        override val displayTime: Long
            get() = -1
        override val timeout: Long
            get() = -1
        override val id: String
            get() = UUID.randomUUID().toString()
        override val channelId: Int
            get() = LayerChannel.Default.channelId
        override val timestamp: Long
            get() = getTimestamp()
        override val priority: Int
            get() = 0
    }

    class Default : Empty()

    companion object {


        fun create(): EnvironmentProperty {
            return Default()
        }

        private fun delegate(
            delegate: EnvironmentProperty,
            change: EnvironmentPropertyBuilder.() -> Unit = {}
        ): EnvironmentProperty {
            val result = DelegateEnvironmentPropertyBuilder(delegate)
            change(result)
            return result
        }

        fun create(
            fragment: Fragment,
            change: EnvironmentPropertyBuilder.() -> Unit = {}
        ): EnvironmentPropertyWithComponent {
            val property = delegate(object : Empty() {
                override val priority: Int
                    get() = fragment.priority
            }, change)
            val component = FragmentManagerEnvironmentComponent.from(fragment)
            return DelegateEnvironmentPropertyWithComponent(component, property)
        }

        fun create(
            activity: FragmentActivity,
            change: EnvironmentPropertyBuilder.() -> Unit = {}
        ): EnvironmentPropertyWithComponent {
            val property = delegate(object : Empty() {
                override val priority: Int
                    get() = activity.priority
            }, change)
            val component = FragmentManagerEnvironmentComponent.from(activity)
            return DelegateEnvironmentPropertyWithComponent(component, property)
        }


    }

}

interface IEnvironment : EnvironmentProperty, FragmentManagerEnvironmentComponent

private class DelegateIEnvironment(
    property: EnvironmentProperty,
    component: FragmentManagerEnvironmentComponent
) : IEnvironment, FragmentManagerEnvironmentComponent by component, EnvironmentProperty by property

interface ID {
    val id: String
}

private class DelegateEnvironment(delegate: IEnvironment, override val base: LifecycleOwner) :
    Environment(), IEnvironment by delegate

abstract class Environment internal constructor() : IEnvironment, Comparator<Environment>,
    Comparable<Environment> {
    abstract val base: LifecycleOwner


    internal companion object {
        fun create(delegate: IEnvironment, base: LifecycleOwner): Environment {
            return DelegateEnvironment(delegate, base)
        }
    }

    override fun compare(o1: Environment, o2: Environment): Int {
        return o1.compareTo(o2)
    }

    override fun compareTo(other: Environment): Int {
        val priorityResult = priority.compareTo(other.priority)
        return if (priorityResult == 0) timestamp.compareTo(other.timestamp) else priorityResult
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Environment

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    internal fun copy(builderAction: EnvironmentPropertyBuilder.() -> Unit = {}): Environment {
        val builder = DelegateEnvironmentPropertyBuilder(this)
        builderAction(builder)
        return builder.withComponent(this).attach(base)
    }

}


private val FragmentActivity.priority: Int
    get() {
        return 0
    }

private val Fragment.priority: Int
    get() {
        var result = requireActivity().priority
        var temp: Fragment? = this
        while (temp != null) {
            result++
            temp = temp.parentFragment
        }
        return result
    }

/**
 * 用于存储dialog信息
 */
interface IDialogStore {
    companion object {
        internal val empty by lazy {
            object : IDialogStore {
                override fun show(environment: IEnvironment, factory: DialogFactory): DialogHandle {
                    return DialogHandle.Empty
                }

                override fun cancel(layerChannel: LayerChannel?, tag: String?) {

                }

                override fun pause(layerChannel: LayerChannel?) {

                }

                override fun resume(layerChannel: LayerChannel?) {

                }

                override fun destroy() {

                }

            }
        }
    }

    fun show(
        environment: IEnvironment,
        factory: DialogFactory,
    ): DialogHandle

    /**
     * @param layerChannel 管道
     * @param tag 标签
     *
     * 如果管道和tag同时为null，则整个store全部cancel
     * 如果管道为null而tag不为null，则cancel掉store中所有标签为tag值的弹窗
     * 如果管道不为null而tag为null，则cancel整个管道
     * 如果两者都不为null，则cancel掉指定管道指定tag的dialog
     *
     *
     */
    fun cancel(layerChannel: LayerChannel? = null, tag: String? = null)


    /**
     * 暂停
     *
     */
    fun pause(layerChannel: LayerChannel? = null)

    /**
     * 恢复
     */
    fun resume(layerChannel: LayerChannel? = null)


    fun destroy()

}


/**
 * 图层，单一图层具有唯一队列
 */
interface LayerChannel {
    val channelId: Int


    companion object {
        val Default by lazy {
            create(0)
        }
        val Loading by lazy {
            create(1)
        }

        fun create(id: Int): LayerChannel {
            return object : LayerChannel {
                override val channelId: Int
                    get() = id

            }
        }
    }

}


/**
 * 用于操作dialog
 */
interface DialogHandle {
    val id: String
    fun dismiss()
    fun isShowing(): Boolean

    /**
     * 表示已经取消，但有可能还未消失
     */
    fun isCanceled(): Boolean
    suspend fun awaitShow()


    companion object Empty : DialogHandle {
        override val id: String
            get() = UUID.randomUUID().toString()

        override fun dismiss() {

        }

        override fun isShowing(): Boolean {
            return false
        }

        override fun isCanceled(): Boolean {
            return true
        }

        override suspend fun awaitShow() {

        }

    }
}


interface DialogFactory {
    suspend fun createDialog(
        environment: Environment,
        handle: DialogHandle
    ): IDialog

    companion object {
        fun create(builder: suspend FactoryParams.() -> IDialog): DialogFactory {
            return object : DialogFactory {
                override suspend fun createDialog(
                    environment: Environment,
                    handle: DialogHandle
                ): IDialog {
                    return builder(FactoryParams(environment, handle))
                }

            }
        }
    }
}


fun Dialog.asIDialog(environment: Environment, handle: DialogHandle): IDialog =
    SystemDialog(this, environment, handle)

fun DialogFragment.asIDialog(environment: Environment, handle: DialogHandle): IDialog =
    FragmentIDialog(this, environment)

fun Layer.asIDialog(environment: Environment, handle: DialogHandle): IDialog =
    AnyLayerDialog(this, environment, handle = handle)

fun DialogFragment.asDialogFactory(): DialogFactory =
    DialogFactory.create { asIDialog(environment, handle) }

fun Dialog.asDialogFactory(): DialogFactory =
    DialogFactory.create { asIDialog(environment, handle) }

fun Layer.asDialogFactory(): DialogFactory = DialogFactory.create { asIDialog(environment, handle) }

interface IDialog {
    val id: String
    fun show()
    fun dismiss()
    fun isShowing(): Boolean

    //dismiss Checker
}

private class FragmentIDialog(
    private val dialog: DialogFragment,
    private val environment: Environment
) : IDialog {
    override val id: String
        get() = environment.id

    private var isShowing = false
    override fun show() {
        val manager = environment.fragmentManager
        if (manager == null) {
            isShowing = true
        } else {
            dialog.show(manager, id)
        }

    }

    override fun dismiss() {
        dialog.dismiss()
    }

    override fun isShowing(): Boolean {
        if (environment.fragmentManager == null) {
            return isShowing
        }
        return dialog.dialog?.isShowing == true
    }

}


private class SystemDialog(
    private val dialog: Dialog,
    private val environment: Environment,
    private val handle: DialogHandle
) : IDialog, DefaultLifecycleObserver {


    override val id: String
        get() = environment.id

    override fun show() {
        return dialog.show()
    }

    override fun dismiss() {
        dialog.dismiss()
    }

    override fun isShowing(): Boolean {
        return dialog.isShowing
    }


}

private class AnyLayerDialog(
    val layer: Layer,
    val environment: Environment,
    val handle: DialogHandle
) : IDialog, DefaultLifecycleObserver, ID by environment {


    override fun show() {
        layer.show()
    }

    override fun dismiss() {
        layer.dismiss()
    }

    override fun isShowing(): Boolean {
        return layer.isShown
    }

}


interface Priority {
    fun priority(): Int
}

private class ReferenceDelegateDialogHandle(delegate: DialogHandle) : DialogHandle {
    val reference = WeakReference(delegate)
    override val id: String
        get() = reference.get()?.id.toString()

    override fun dismiss() {
        reference.get()?.dismiss()
    }

    override fun isShowing(): Boolean {
        return reference.get()?.isShowing() == true
    }

    override fun isCanceled(): Boolean {
        return reference.get()?.isCanceled() == true
    }

    override suspend fun awaitShow() {
        reference.get()?.awaitShow()
    }


}


private class LayerDialogStore constructor(store: DialogStore, val priority: Int) :
    DefaultLifecycleObserver {
    private val queue = PriorityBlockingQueue<LifecycleDialog>()
    private val pauseStores = PauseStore()
    private val actionChannel = Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var isDestroyed = false

    private class PauseStore {

        //        private val timestampMap = arrayMapOf<String, Long>()
        private val map = arrayMapOf<String, LifecycleDialog>()
        fun add(lifecycleDialog: LifecycleDialog) {
            val id = lifecycleDialog.id
            map[id] = lifecycleDialog
//            timestampMap[id] = getTimestamp()
        }


        fun remove(lifecycleDialog: LifecycleDialog) {
            val id = lifecycleDialog.id
//            timestampMap.remove(id)
            map.remove(id)
        }

        fun forKeys(action: (String) -> Unit) {
            map.keys.forEach {
                action(it)
            }
        }

        fun removeTag(tag: String) {
            val tagList = map.values.filter { it.environment.tag == tag }
            tagList.forEach {
                remove(it)
            }
        }

        fun get(id: String): LifecycleDialog? {
            val last = map[id] ?: return null
            return LifecycleDialog(last.factory, last.environment.copy {
                if (last.showTime > 0) {
                    //已经展示过了
                    if (last.environment.displayTime > 0) {
                        val time = getTimestamp() - last.showTime
                        if (last.environment.displayTime < time) {
                            this.displayTime = 0
                        } else {
                            this.displayTime = time
                        }
                    }
                }

            }, last.store)
        }

        fun clear() {
//            timestampMap.clear()
            map.clear()
        }
    }


    init {
        store.pushStore(this)
    }

    fun isTop(lifecycleDialog: LifecycleDialog): Boolean {
        return queue.peek() === lifecycleDialog
    }


    fun contains(lifecycleDialog: LifecycleDialog): Boolean {
        return queue.contains(lifecycleDialog)
    }

    @Synchronized
    fun cancel(tag: String) {
        invokeOnMain {
            queue.forEach {
                it.cancel(tag)
            }
            pauseStores.removeTag(tag)
        }

    }

    suspend fun loop() {
        for (action in actionChannel) {
            next()
        }
    }

    suspend fun next() {
        queue.peek()?.run()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        destroyInternal()
    }

    private fun destroyInternal() {
        isDestroyed = true
        actionChannel.close()
        queue.clear()
        pauseStores.clear()
    }

    @Synchronized
    fun destroy() {
        destroyInternal()
    }

    fun enqueue(
        factory: DialogFactory,
        environment: Environment
    ): DialogHandle {
        if (isDestroyed) return DialogHandle.Empty
        return enqueue(LifecycleDialog(factory, environment, this))
    }

    suspend fun createDialog(
        factory: DialogFactory,
        environment: Environment,
        handle: DialogHandle
    ): IDialog {
        return factory.createDialog(environment, handle)
    }

    fun enqueue(lifecycleDialog: LifecycleDialog): DialogHandle {
        if (isDestroyed) return DialogHandle.Empty
        if (queue.contains(lifecycleDialog)) {
            queue.remove(lifecycleDialog)
        }
        queue.offer(lifecycleDialog)
        actionChannel.offer(Unit)
        return ReferenceDelegateDialogHandle(lifecycleDialog)
    }

    fun remove(lifecycleDialog: LifecycleDialog) {
        queue.remove(lifecycleDialog)
        pauseStores.remove(lifecycleDialog)
    }


    fun setPause(lifecycleDialog: LifecycleDialog) {
        if (lifecycleDialog.isCanceled() || isDestroyed) return
        queue.remove(lifecycleDialog)
        pauseStores.add(lifecycleDialog)
    }

    fun setResume(lifecycleDialog: LifecycleDialog) {
        if (lifecycleDialog.isCanceled() || isDestroyed) {
            pauseStores.remove(lifecycleDialog)
            return
        }
        pauseStores.get(lifecycleDialog.id)?.run {
            enqueue(this)
            pauseStores.remove(this)
        }
    }

}

private class DialogStore(val parentLifecycleOwner: LifecycleOwner) :
    IDialogStore, DefaultLifecycleObserver {
    private val layerStores = SparseArrayCompat<LayerDialogStore>()
    private val channel = Channel<LayerDialogStore>(capacity = Channel.BUFFERED)

    private var job: Job? = null

    private var isDestroyed = false
    override fun destroy() {
        invokeOnMain {
            isDestroyed = true
            map.remove(parentLifecycleOwner)
            layerStores.clear()
            channel.close()
            job?.cancel()
            parentLifecycleOwner.lifecycle.removeObserver(this@DialogStore)
        }

    }

    @Synchronized
    private fun getLayerStore(layerChannel: LayerChannel): LayerDialogStore {
        val layerPriority = layerChannel.channelId
        return layerStores[layerPriority] ?: LayerDialogStore(this, layerChannel.channelId)
    }

    fun pushStore(layerDialogStore: LayerDialogStore) {
        if (isDestroyed) return
        channel.offer(layerDialogStore)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        isDestroyed = true
        map.remove(owner)
        layerStores.clear()
        channel.close()
        job?.cancel()
    }

    init {
        if (parentLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            map[parentLifecycleOwner] = this
            parentLifecycleOwner.lifecycle.addObserver(this@DialogStore)
            job = parentLifecycleOwner.lifecycleScope.launch {
                for (store in channel) {
                    createLayerJob(store)
                }
            }
        }


    }

    private suspend fun createLayerJob(layerDialogStore: LayerDialogStore) {
        parentLifecycleOwner.lifecycle.addObserver(layerDialogStore)
        val scope = CoroutineScope(currentCoroutineContext()) + SupervisorJob()
        scope.launch {
            layerDialogStore.loop()
        }
        layerStores[layerDialogStore.priority] = layerDialogStore
    }

    companion object {
        private val map = mutableMapOf<LifecycleOwner, DialogStore>()

        @Synchronized
        fun get(owner: LifecycleOwner): IDialogStore {
            return map[owner] ?: invokeOnMain {
                if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                    DialogStore(owner)
                } else {
                    IDialogStore.empty
                }
            }
        }
    }

    @Synchronized
    override fun show(
        environment: IEnvironment,
        factory: DialogFactory,
    ): DialogHandle {
        return getLayerStore(environment).enqueue(
            factory,
            environment.attach(parentLifecycleOwner)
        )
    }

    private fun cancelTag(tag: String) {
        layerStores.valueIterator().forEach {
            it.cancel(tag)
        }
    }

    private fun removeLayer(store: LayerDialogStore) {
        layerStores.remove(store.priority)
        parentLifecycleOwner.lifecycle.removeObserver(store)
    }

    @Synchronized
    override fun cancel(layerChannel: LayerChannel?, tag: String?) {
        invokeOnMain {
            if (layerChannel != null && tag != null) {
                getLayerStore(layerChannel).cancel(tag)
            } else if (layerChannel == null && tag == null) {
                destroy()
            } else if (layerChannel != null && tag == null) {
                val store = getLayerStore(layerChannel)
                store.destroy()
                removeLayer(store)
            } else if (layerChannel == null && tag != null) {
                cancelTag(tag)
            }
        }


    }

    private val pauseTag = "a30aa3b4-602f-46ae-a22f-2e7d8d4b27eb"


    private fun createPause(store: LayerDialogStore): LifecycleDialog {
        return LifecycleDialog(
            object : DialogFactory {
                override suspend fun createDialog(
                    environment: Environment,
                    handle: DialogHandle
                ): IDialog {
                    return SignalDialog()
                }
            },
            FragmentManagerEnvironmentComponent.application().withProperty {
                priority = Int.MIN_VALUE
                tag = pauseTag
            }.attach(ProcessLifecycleOwner.get()),
            store
        )
    }

    @Synchronized
    override fun pause(layerChannel: LayerChannel?) {
        invokeOnMain {
            if (layerChannel != null) {
                getLayerStore(layerChannel).apply {
                    enqueue(createPause(this))
                }
            } else {
                layerStores.valueIterator().forEach {
                    it.enqueue(createPause(it))
                }
            }
        }
    }

    @Synchronized
    override fun resume(layerChannel: LayerChannel?) {
        invokeOnMain {
            if (layerChannel != null) {
                getLayerStore(layerChannel).apply {
                    cancel(pauseTag)
                }
            } else {
                layerStores.valueIterator().forEach {
                    it.cancel(pauseTag)
                }
            }
        }
    }

}


private class SignalDialog : IDialog {
    private var isShowing = false
    override val id: String
        get() = UUID.randomUUID().toString()

    override fun show() {
        isShowing = true
    }

    override fun dismiss() {
        isShowing = false
    }

    override fun isShowing(): Boolean {
        return isShowing
    }

}

private class LifecycleDialog(
    val factory: DialogFactory,
    val environment: Environment,
    val store: LayerDialogStore
) :
    DefaultLifecycleObserver,
    Comparable<LifecycleDialog>,
    Comparator<LifecycleDialog>,
    DialogHandle {

    private var dialog: IDialog? = null
    private var completableDeferred = CompletableDeferred<Unit>()
    private var isCanceled = false

    var showTime = -1L

    init {
        with(environment.lifecycleOwner) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                lifecycleScope.launch {
                    lifecycle.addObserver(this@LifecycleDialog)
                }
            }
        }

    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        destroy()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        store.setPause(this)

    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (isCanceled) return
        store.setResume(this)
    }


    override fun isShowing(): Boolean {
        return dialog?.isShowing() == true
    }

    override fun isCanceled(): Boolean {
        return isCanceled
    }

    override suspend fun awaitShow() {
        if (!isShowing()) {
            completableDeferred.await()
        }
    }


    fun isShowTimeOut(startTime: Long): Boolean {
        if (environment.displayTime < 0) return false
        return System.currentTimeMillis() - startTime >= environment.displayTime
    }

    private fun isTimeOut(): Boolean {
        return environment.timeout > 0 && System.currentTimeMillis() - environment.timestamp > environment.timeout
    }

    private fun resetCompleteDeferred() {
        completableDeferred.cancel()
        completableDeferred = CompletableDeferred()
    }

    suspend fun run() {
        with(environment.lifecycleOwner) {
            val scope = CoroutineScope(currentCoroutineContext() + lifecycleScope.coroutineContext)
            scope.launch {
                if (isTimeOut() || isCanceled || environment.displayTime == 0L) {
                    remove()
                    resetCompleteDeferred()
                    return@launch
                }
                if (checkIsCurrent { createAndShow() } == true) {
                    val startTime = System.currentTimeMillis()
                    while (true) {
                        //不在队列top位置
                        if (!isCurrentInTop()) {
                            if (isShowing())
                                dialog?.dismiss()
                            resetCompleteDeferred()
                            checkDialogDismiss()
                            break
                        }
                        //显示时间超过限定展示时间
                        if (isShowTimeOut(startTime)) {
                            if (isShowing())
                                dialog?.dismiss()
                            resetCompleteDeferred()
                            checkDialogDismiss()
                            isCanceled = true
                            remove()
                            lifecycle.removeObserver(this@LifecycleDialog)
                            break
                        }
                        //使用者主动取消
                        if (!isShowing() && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            resetCompleteDeferred()
                            remove()
                            isCanceled = true
                            lifecycle.removeObserver(this@LifecycleDialog)
                            break
                        }
                        //用户通过cancel方式取消
                        if (isCanceled) {
                            resetCompleteDeferred()
                            remove()
                            if (isShowing()) dialog?.dismiss()
                            checkDialogDismiss()
                            lifecycle.removeObserver(this@LifecycleDialog)
                            break
                        }
                        delay(200)
                    }
                    dialog = null
                } else {
                    remove()
                    resetCompleteDeferred()
                }
            }.run {
                invokeOnCompletion {
                    if (isShowing()) dismiss()
                }
                join()
            }
        }
        store.next()
    }


    private fun remove() {
        store.remove(this)
    }

    fun cancel(tag: String) {
        if (environment.tag == tag) isCanceled = true
    }

    private suspend fun <R> checkIsCurrent(
        action: suspend LifecycleDialog.() -> R
    ): R? {
        return if (store.isTop(this)) {
            action(this)
        } else null
    }

    private suspend fun checkDialogDismiss() {
        while (true) {
            if (!isShowing()) break
            delay(200)
        }
    }

    private fun isCurrentInTop(): Boolean {
        return store.isTop(this)
    }

    override val id: String
        get() = environment.id


    override fun dismiss() {
        destroy()
    }


    private fun destroy() {
        invokeOnMain {
            destroyInternal()
            resetCompleteDeferred()
        }
    }

    @MainThread
    private fun destroyInternal() {
        if (isShowing()) {
            dialog?.dismiss()
        }
        remove()
        environment.lifecycleOwner.lifecycle.removeObserver(this)
        dialog = null
        isCanceled = true
    }


    private suspend fun createAndShow(): Boolean {
        if (environment.displayTime == 0L) return false
        return withTimeout(2000) {
            with(factory) {
                val dialog = store.createDialog(
                    this,
                    environment,
                    ReferenceDelegateDialogHandle(this@LifecycleDialog)
                )
                this@LifecycleDialog.dialog = dialog
                dialog.show()
                while (!dialog.isShowing()) {
                    delay(200)
                }
                showTime = getTimestamp()
                completableDeferred.complete(Unit)
                true
            }
        }

    }


    override fun compareTo(other: LifecycleDialog): Int {
        return environment.compareTo(other.environment)
    }

    override fun compare(o1: LifecycleDialog, o2: LifecycleDialog): Int {
        return o1.compareTo(o2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LifecycleDialog

        if (environment != other.environment) return false

        return true
    }

    override fun hashCode(): Int {
        return environment.hashCode()
    }

}

abstract class EnvironmentPropertyBuilder internal constructor() : EnvironmentProperty {
    abstract override var tag: String

    abstract override var displayTime: Long

    abstract override var timeout: Long

    override var timestamp: Long = getTimestamp()
        internal set

    abstract override var priority: Int
    abstract override var channelId: Int
    abstract override var id: String

}

var EnvironmentPropertyBuilder.layerChannel: LayerChannel
    get() {
        return LayerChannel.create(channelId)
    }
    set(value) {
        channelId = value.channelId
    }

fun interface PropertyChangeListener {
    fun onPropertyChanged()
}


private fun <T, R> T.listenProperty(
    defaultValue: R, listener: PropertyChangeListener = object : PropertyChangeListener {
        override fun onPropertyChanged() {

        }
    }
): ReadWriteProperty<T, R> {
    return ListenableReadWriteProperty(defaultValue, listener)
}

private fun <R : PropertyChangeListener, T : Any?> R.listenProperty(defaultValue: T) =
    listenProperty(defaultValue, this)

private class ListenableReadWriteProperty<T, R>(
    val defaultValue: R,
    val onPropertyChangeListener: PropertyChangeListener
) : ReadWriteProperty<T, R> {
    private var result = defaultValue
    override fun setValue(thisRef: T, property: KProperty<*>, value: R) {
        result = value
        onPropertyChangeListener.onPropertyChanged()
    }

    override fun getValue(thisRef: T, property: KProperty<*>): R {
        return result
    }

}

internal open class DelegateEnvironmentPropertyBuilder(val property: EnvironmentProperty) :
    EnvironmentPropertyBuilder(), PropertyChangeListener {
    private var changed = false
    override var tag: String by listenProperty(property.tag)
    override var displayTime: Long by listenProperty(property.displayTime)
    override var timeout: Long by listenProperty(property.timeout)
    override var priority: Int by listenProperty(property.priority)
    override var id: String by listenProperty(property.id)
    override var channelId: Int by listenProperty(property.channelId)
    override fun onPropertyChanged() {
        changed = true
    }

    fun copyIfChanged(target: EnvironmentPropertyBuilder) {
        if (!changed) return
        if (tag != property.tag) target.tag = tag
        if (displayTime != property.displayTime) target.displayTime = displayTime
        if (timeout != property.timeout) target.timeout = timeout
        if (priority != property.priority) target.priority = priority
        if (id != property.id) target.id = id
        if (channelId != property.channelId) target.channelId = channelId
    }

    override fun toString(): String {
        return "DelegateEnvironmentPropertyBuilder(changed=$changed, tag='$tag', displayTime=$displayTime, timeout=$timeout, priority=$priority, id='$id', channelId=$channelId)"
    }


}

private class EmptyEnvironmentPropertyBuilder :
    DelegateEnvironmentPropertyBuilder(EnvironmentProperty.create()) {

}

abstract class FragmentManagerEnvironmentComponentBuilder internal constructor() :
    FragmentManagerEnvironmentComponent {
    abstract override var lifecycleOwner: LifecycleOwner
    abstract override var context: Context
    abstract override var fragmentManager: FragmentManager?
}

internal class DelegateEnvironmentComponentBuilder internal constructor(component: FragmentManagerEnvironmentComponent) :
    FragmentManagerEnvironmentComponentBuilder(), PropertyChangeListener {
    override var lifecycleOwner: LifecycleOwner by listenProperty(component.lifecycleOwner)

    override var context: Context by listenProperty(component.context)

    override var fragmentManager: FragmentManager? by listenProperty(component.fragmentManager)
    private var changed = false


    override fun onPropertyChanged() {
        changed = true
    }

}

private fun Fragment.asEnvironmentComponentBuilder(): FragmentManagerEnvironmentComponentBuilder {
    return FragmentManagerEnvironmentComponent.from(this).asEnvironmentComponentBuilder()
}

private fun FragmentActivity.asEnvironmentComponentBuilder(): FragmentManagerEnvironmentComponentBuilder {
    return FragmentManagerEnvironmentComponent.from(this).asEnvironmentComponentBuilder()
}

private fun FragmentManagerEnvironmentComponent.asEnvironmentComponentBuilder(): FragmentManagerEnvironmentComponentBuilder {
    var mLifecycleOwner: LifecycleOwner? = null
    var mContext: Context? = null
    var mFragmentManager: FragmentManager? = null
    return object : FragmentManagerEnvironmentComponentBuilder() {
        override var lifecycleOwner: LifecycleOwner
            get() = mLifecycleOwner ?: this@asEnvironmentComponentBuilder.lifecycleOwner
            set(value) {
                mLifecycleOwner = value
            }
        override var context: Context
            get() = mContext ?: this@asEnvironmentComponentBuilder.context
            set(value) {
                mContext = value
            }
        override var fragmentManager: FragmentManager?
            get() = mFragmentManager ?: this@asEnvironmentComponentBuilder.fragmentManager
            set(value) {
                mFragmentManager = value
            }
    }
}


class FactoryParams internal constructor(val environment: Environment, val handle: DialogHandle) {
    fun dismiss() = handle.dismiss()

    fun isShowing() = handle.isShowing()

    fun isCanceled() = handle.isCanceled()

    suspend fun awaitShow() = handle.awaitShow()

    suspend fun addLifecycleObserver(observer: LifecycleObserver) =
        withContext(Dispatchers.Main.immediate) {
            environment.lifecycleOwner.lifecycle.addObserver(observer)
        }

    fun Dialog.asIDialog(): IDialog = asIDialog(environment, handle)
    fun Layer.asIDialog(): IDialog = asIDialog(environment, handle)
    fun DialogFragment.asIDialog(): IDialog = asIDialog(environment, handle)


}

abstract class EnvironmentBuilder internal constructor() : StoreEnvironmentBuilder() {
    abstract fun scope(lifecycleOwner: LifecycleOwner): StoreEnvironmentBuilder
}

fun EnvironmentBuilder.scope(scope: StoreViewModelScope): StoreEnvironmentBuilder {
    return scope(scope.environment.lifecycleOwner)
}

abstract class ContentBuilder internal constructor() {
    abstract fun content(dialogBuilder: suspend FactoryParams.() -> IDialog)
}

abstract class StoreEnvironmentBuilder internal constructor() : ContentBuilder() {
    abstract fun component(
        component: FragmentManagerEnvironmentComponent? = null,
        componentBuilder: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
    )

    abstract fun property(
        property: EnvironmentProperty? = null,
        propertyBuilder: EnvironmentPropertyBuilder.() -> Unit = {}
    )
}


fun StoreEnvironmentBuilder.dialog(dialogBuilder: suspend FactoryParams.() -> Dialog) {
    content {
        dialogBuilder().asIDialog(environment, handle)
    }
}

fun StoreEnvironmentBuilder.layer(dialogBuilder: suspend FactoryParams.() -> Layer) {
    content {
        dialogBuilder().asIDialog(environment, handle)
    }
}

fun StoreEnvironmentBuilder.fragment(dialogBuilder: suspend FactoryParams.() -> DialogFragment) {
    content {
        dialogBuilder().asIDialog(environment, handle)
    }
}


fun StoreEnvironmentBuilder.component(
    fragment: Fragment,
    componentBuilder: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
) {
    component(FragmentManagerEnvironmentComponent.from(fragment), componentBuilder)
    property {
        priority = fragment.priority
    }
}

fun StoreEnvironmentBuilder.environment(environment: IEnvironment) {
    component(environment)
    property(environment)
}

fun StoreEnvironmentBuilder.component(
    activity: FragmentActivity,
    componentBuilder: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
) {
    component(FragmentManagerEnvironmentComponent.from(activity), componentBuilder)
    property {
        priority = activity.priority
    }
}

fun StoreEnvironmentBuilder.component(
    scope: StoreViewModelScope,
    componentBuilder: FragmentManagerEnvironmentComponentBuilder.() -> Unit = {}
) {
    if (scope.environment.isFragment) {
        component(FragmentManagerEnvironmentComponent.from(scope.environment.asFragment()), componentBuilder)
        property {
            priority = scope.environment.asFragment().priority
        }
    } else if (scope.environment.isActivity) {
        val activity = scope.environment.hostActivity as? FragmentActivity ?: return
        component(
            FragmentManagerEnvironmentComponent.from(activity),
            componentBuilder
        )
        property {
            priority = activity.priority
        }
    }
}

fun StoreEnvironmentBuilder.currentActivityComponent() {
    component {
        val activity = currentActivity as? FragmentActivity
        if (activity != null) {
            lifecycleOwner = activity
            context = activity
            fragmentManager = activity.supportFragmentManager
        }
    }
}

fun StoreEnvironmentBuilder.applicationComponent() {
    component {
        lifecycleOwner = ProcessLifecycleOwner.get()
        context = application
        fragmentManager = null

    }
}

private class EmptyEnvironmentComponentBuilder : FragmentManagerEnvironmentComponentBuilder() {
    var mLifecycleOwner: LifecycleOwner? = null
    var mContext: Context? = null
    var mFragmentManager: FragmentManager? = null

    override var lifecycleOwner: LifecycleOwner
        get() = mLifecycleOwner ?: throw RuntimeException("组件LifecycleOwner不能为空")
        set(value) {
            mLifecycleOwner = value
        }
    override var context: Context
        get() = mContext ?: throw RuntimeException("组件Context不能为空")
        set(value) {
            mContext = value
        }
    override var fragmentManager: FragmentManager?
        get() = mFragmentManager
        set(value) {
            mFragmentManager = value
        }


    fun setEnvironmentComponent(environmentComponent: FragmentManagerEnvironmentComponent) {
        mLifecycleOwner = environmentComponent.lifecycleOwner
        mContext = environmentComponent.context
        mFragmentManager = environmentComponent.fragmentManager
    }

}

private class InternalEnvironmentBuilder : EnvironmentBuilder() {
    var mComponentBuilder: EmptyEnvironmentComponentBuilder? = null
    private val mTimestamp = getTimestamp()
    var mFactory: DialogFactory? = null
    var scopeLifecycleOwner: LifecycleOwner? = null
    var mPropertyBuilder: EnvironmentPropertyBuilder = EmptyEnvironmentPropertyBuilder()


    private fun getOrCreateComponentBuilder(): EmptyEnvironmentComponentBuilder {
        return mComponentBuilder ?: run {
            EmptyEnvironmentComponentBuilder().also { mComponentBuilder = it }
        }
    }


    override fun component(
        component: FragmentManagerEnvironmentComponent?,
        componentBuilder: FragmentManagerEnvironmentComponentBuilder.() -> Unit
    ) {
        getOrCreateComponentBuilder().apply {
            if (component != null) setEnvironmentComponent(component)
            componentBuilder()
        }
    }

    override fun property(
        property: EnvironmentProperty?,
        propertyBuilder: EnvironmentPropertyBuilder.() -> Unit
    ) {
        if (property != null) {
            val builder = DelegateEnvironmentPropertyBuilder(property)
            (mPropertyBuilder as? DelegateEnvironmentPropertyBuilder)?.apply {
                copyIfChanged(builder)
            }
            mPropertyBuilder = builder
        }
        propertyBuilder(mPropertyBuilder)
    }

    override fun scope(lifecycleOwner: LifecycleOwner): StoreEnvironmentBuilder {
        scopeLifecycleOwner = lifecycleOwner
        return this
    }


    fun show(): DialogHandle {
        val lifecycleOwner = scopeLifecycleOwner ?: run {
            return DialogHandle.Empty
        }
        val component = mComponentBuilder ?: run {
            return DialogHandle.Empty
        }
        val factory = mFactory ?: run {
            return DialogHandle.Empty
        }
        return DialogManager.requestStore(lifecycleOwner)
            .show(mPropertyBuilder.withComponent(component), factory)
    }

    fun show(store: IDialogStore): DialogHandle {
        val component = mComponentBuilder ?: return DialogHandle.Empty
        val factory = mFactory ?: return DialogHandle.Empty
        return store.show(mPropertyBuilder.withComponent(component), factory)
    }

    override fun content(dialogBuilder: suspend FactoryParams.() -> IDialog) {
        mFactory = DialogFactory.create(dialogBuilder)
    }

}

@Volatile
private var lastTime = System.currentTimeMillis()

@Synchronized
private fun getTimestamp(): Long {
    var current = System.currentTimeMillis()
    if (current <= lastTime) {
        current = lastTime + 1
        lastTime = current
    }
    return current
}







