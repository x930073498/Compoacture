package com.x930073498.compoacture.extentions

import android.app.Activity
import android.view.View
import android.view.Window
import androidx.annotation.IdRes
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding


fun Activity.windowInsetsControl(
    view: (Activity) -> View = { findViewById(Window.ID_ANDROID_CONTENT) },
    action: WindowInsetsControllerCompat.() -> Unit
) {
    val controllerCompat = WindowInsetsControllerCompat(window, view(this))
    action(controllerCompat)
}

fun Activity.windowInsetsControl(
    view: View,
    action: WindowInsetsControllerCompat.() -> Unit
) {
    windowInsetsControl({ view }, action)
}

fun Activity.windowInsetsControl(
    @IdRes id: Int,
    action: WindowInsetsControllerCompat.() -> Unit
) {
    windowInsetsControl(findViewById<View>(id), action)
}

fun Activity.windowInsetsControl(
    binding: ViewBinding,
    action: WindowInsetsControllerCompat.() -> Unit
) {
    windowInsetsControl(binding.root, action)
}

fun Fragment.windowInsetsControl(action: WindowInsetsControllerCompat.() -> Unit) {
    val controllerCompat = WindowInsetsControllerCompat(requireActivity().window, requireView())
    action(controllerCompat)
}