package com.x930073498.compoacture.extentions

import android.view.View
import androidx.annotation.IdRes

fun<T:View> View.requireViewWithId(@IdRes id:Int):T{
    return findViewById(id)
}