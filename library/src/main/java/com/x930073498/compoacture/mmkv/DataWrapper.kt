@file:Suppress("UNCHECKED_CAST")

package com.x930073498.compoacture.mmkv

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.tencent.mmkv.MMKV
import com.x930073498.compoacture.utils.logE

fun <T> MMKV.putData(key: String, data: T?) {
    encode(key, DataWrapper(data))
}

fun <T : Any> MMKV.getData(key: String): T? {
    val wrapper =
        runCatching { decodeParcelable(key, DataWrapper::class.java) }.getOrNull() ?: return null
    return wrapper.data as? T
}

@Keep
data class DataWrapper<T>(
    @Keep
    val data: T?
) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.run {
        runCatching { readValue(DataWrapper::class.java.classLoader) as? T }.onFailure {
            logE("数据读出错误", it)
        }.getOrNull()
    })

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        runCatching { parcel.writeValue(data) }.onFailure {
            logE("数据写入错误,data={$data}", it)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DataWrapper<*>> {
        override fun createFromParcel(parcel: Parcel): DataWrapper<*> {
            return DataWrapper<Any>(parcel)
        }

        override fun newArray(size: Int): Array<DataWrapper<*>?> {
            return arrayOfNulls(size)
        }
    }
}