package com.x930073498.compoacture

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.tencent.mmkv.MMKV
import com.x930073498.compoacture.utils.isDebug

class StartUp : Initializer<Unit> {
    override fun create(context: Context) {
        init(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return arrayListOf()
    }

    companion object {
        private var init = false
        @Synchronized
        fun init(context: Context) {
            if (init) return
            MMKV.initialize(context)
            val applicationContext=context.applicationContext
            if (applicationContext is Application) {
                applicationContext.registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks)
                application = applicationContext
                isDebug = context.isDebug
            }
            init = true
        }
    }
}