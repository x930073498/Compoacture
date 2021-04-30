package com.x930073498.compoacture.utils

import android.util.Log
import com.x930073498.compoacture.debugTag
import com.x930073498.compoacture.isDebug

fun logE(msg: String?, throwable: Throwable? = null) {
    if (isDebug) {
        Log.e(debugTag, msg, throwable)
    }
}

fun logI(msg: String?, throwable: Throwable? = null) {
    if (isDebug) {
        Log.i(debugTag, msg, throwable)
    }
}

fun logV(msg: String?, throwable: Throwable? = null) {
    if (isDebug) {
        Log.v(debugTag, msg, throwable)
    }
}
