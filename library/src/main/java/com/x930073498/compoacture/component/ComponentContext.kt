package com.x930073498.compoacture.component

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

interface ComponentContext {

    val current: Any

    val hostActivity: ComponentActivity

}
fun ComponentContext.asFragment():Fragment{
    return current as Fragment
}


val ComponentContext.fragmentManager: FragmentManager?
    get() {
        val target = current
        if (target is Fragment) {
            return target.childFragmentManager
        }
        if (target is FragmentActivity) {
            return target.supportFragmentManager
        }
        return null
    }

val ComponentContext.isActivity: Boolean
    get() {
        return current is FragmentActivity
    }

val ComponentContext.isFragment: Boolean
    get() {
        return current is Fragment
    }