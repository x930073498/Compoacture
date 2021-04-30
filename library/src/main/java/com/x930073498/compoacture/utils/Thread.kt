package com.x930073498.compoacture.utils

import android.os.Looper
import com.x930073498.compoacture.mainHandler
import java.util.concurrent.CountDownLatch


fun <R> invokeOnMain(transform: () -> R): R {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        return transform()
    }
    val countDownLatch = CountDownLatch(1);
    var result: R? = null
    mainHandler.post {
        result = transform()
        countDownLatch.countDown()
    }
    countDownLatch.await()
    return result!!
}