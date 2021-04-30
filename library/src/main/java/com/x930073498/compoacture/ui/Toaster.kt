package com.x930073498.compoacture.ui

import android.view.Gravity
import android.widget.Toast
import com.x930073498.compoacture.component.Disposable

interface Toaster:Disposable {
    fun toast(msg: CharSequence?, duration: Int = Toast.LENGTH_SHORT, gravity: Int = Gravity.CENTER)
}