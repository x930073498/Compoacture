package com.x930073498.compoacture.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity

val Context.isDebug: Boolean
    get() {
        return applicationInfo != null && (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
    }
val Context.activity: Activity?
    get() {
        return findActivity()
    }
val Context.componentActivity: ComponentActivity?
    get() {
        return activity as? ComponentActivity
    }

private fun Context.findActivity(): Activity? {
    var target: Context? = this;
    while (target != null) {
        if (target is Activity) return target
        target = if (target is ContextWrapper) {
            target.baseContext
        } else {
            null
        }
    }
    return null
}