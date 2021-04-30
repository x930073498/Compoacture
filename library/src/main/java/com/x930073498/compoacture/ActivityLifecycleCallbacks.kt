package com.x930073498.compoacture

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.x930073498.compoacture.mmkv.MmkvSavedStateStore


internal object ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    private var hasStarted = false
    private val activities = arrayListOf<Activity>()
    internal val currentActivity: Activity?
        get() {
            return if (activities.isEmpty())null else activities[activities.size - 1]
        }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (!hasStarted) {
            if (savedInstanceState == null) {
                MmkvSavedStateStore.clear()
            }
        }
        hasStarted = true
        activities.add(activity)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        activities.remove(activity)
    }
}