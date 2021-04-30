@file:Suppress("SpellCheckingInspection")

package com.x930073498.compoacture

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.didi.drouter.api.DRouter
import com.tencent.mmkv.MMKV
import com.x930073498.compoacture.component.BinderAgent
import com.x930073498.compoacture.component.DataBinder
import com.x930073498.compoacture.component.DataBinderProvider
import com.x930073498.compoacture.component.DefaultDataBinderProvider

val isMainthread: Boolean
    get() {
        return Looper.getMainLooper() == Looper.myLooper()
    }

val mainHandler by lazy {
    Handler(Looper.getMainLooper())
}
var mmkv: (name: String) -> MMKV = { MMKV.mmkvWithID(it)!! }

var isDebug = false

var debugTag = "ComponentLog"

var defaultMMKV = MMKV.defaultMMKV()

val defaultDataBinderHandle: BinderAgent by lazy {
    val provider = DRouter.build(DataBinderProvider::class.java)
        .setDefaultIfEmpty(DefaultDataBinderProvider()).getService()
    val handle = BinderAgent()
    provider.onHandle(handle)
    handle
}

val currentContext: Context
    get() {
        return ActivityLifecycleCallbacks.currentActivity ?: application
    }

var defaultDataBinderFeature = DataBinder.Feature

val currentActivity: Activity?
    get() {
        return ActivityLifecycleCallbacks.currentActivity
    }

lateinit var application: Application
    internal set
