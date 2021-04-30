package com.x930073498.compoacture.component

import com.x930073498.compoacture.databinder.GlideDataBinder
import com.x930073498.compoacture.databinder.TextBinder

interface DataBinderProvider {
    fun onHandle(handle: BinderAgent)
}

open class DefaultDataBinderProvider : DataBinderProvider {
    override fun onHandle(handle: BinderAgent) {
        handle.addBinder(TextBinder)
        handle.addBinder(GlideDataBinder)
    }
}


