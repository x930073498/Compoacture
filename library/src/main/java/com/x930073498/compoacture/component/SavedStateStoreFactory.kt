package com.x930073498.compoacture.component

import com.x930073498.compoacture.mmkv.MmkvSavedStateStore

fun interface SavedStateStoreFactory {

    fun create(id:String): SavedStateStore

}
class DefaultSavedStateStoreFactory: SavedStateStoreFactory {
    override fun create(id: String): SavedStateStore {
        return MmkvSavedStateStore(id)
    }

}
