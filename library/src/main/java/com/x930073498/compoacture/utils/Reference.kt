package com.x930073498.compoacture.utils

import java.lang.ref.WeakReference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


fun <T> weakRef(): ReadWriteProperty<Any?, T?> {

    return object : ReadWriteProperty<Any?, T?> {
        var ref = WeakReference<T>(null)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            ref = WeakReference(value)
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            return ref.get()
        }

    }
}