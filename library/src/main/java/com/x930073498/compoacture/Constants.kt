@file:Suppress("SpellCheckingInspection")

package com.x930073498.compoacture

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.tencent.mmkv.MMKV

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



val currentContext: Context
    get() {
        return ActivityLifecycleCallbacks.currentActivity ?: application
    }


val currentActivity: Activity?
    get() {
        return ActivityLifecycleCallbacks.currentActivity
    }

lateinit var application: Application
    internal set
