package com.x930073498.compoacture.mmkv

import com.tencent.mmkv.MMKV
import com.x930073498.compoacture.defaultMMKV
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun preferenceFrom(
    name: String,
    mode: Int = MMKV.SINGLE_PROCESS_MODE,
    cryptKey: String? = null,
    rootPath: String? = null
): Preference {
    return Preference.from(MMKV.mmkvWithID(name, mode, cryptKey, rootPath))
}


class Preference private constructor(private val mmkv: MMKV?) {
    companion object {
         fun from(mmkv: MMKV?): Preference {
            return Preference(mmkv)
        }
    }

    fun <T> putData(key: String, data: T?) {
        mmkv?.putData(key, data)
    }

    fun <T> getData(key: String): T? {
        return mmkv?.getData(key)
    }
    operator fun<T> get(key: String): T? {
        return getData<T>(key)
    }

    operator fun<T> set(key: String, value: T) {
        putData(key, value)
    }


}

fun <T> Preference.persist(key: String, defaultValue: () -> T): ReadWriteProperty<Any, T> {
    return object : ReadWriteProperty<Any, T> {
        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            putData(key, value)
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return getData<T>(key) ?: defaultValue()
        }

        operator fun get(key: String): T {
            return getData<T>(key) ?: defaultValue()
        }


        operator fun set(key: String, value: T) {
            putData(key, value)
        }
    }
}

fun <T> Preference.persist(key: String, defaultValue: T) = persist(key) { defaultValue }
fun <T> Preference.persist(key: String) = persist<T?>(key) { null }

fun <T> defaultPersist(key: String) = Preference.from(defaultMMKV).persist<T>(key)
fun <T> defaultPersist(key: String, defaultValue: T) =
    Preference.from(defaultMMKV).persist(key, defaultValue)

fun <T> defaultPersist(key: String, defaultValue: () -> T) =
    Preference.from(defaultMMKV).persist(key, defaultValue)