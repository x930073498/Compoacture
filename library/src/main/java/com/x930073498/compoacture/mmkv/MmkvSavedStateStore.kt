@file:Suppress("UNCHECKED_CAST")

package com.x930073498.compoacture.mmkv

import com.x930073498.compoacture.mmkv
import com.x930073498.compoacture.component.AbstractSavedStateStore

@Suppress("UNCHECKED_CAST")
class MmkvSavedStateStore(id: String) : AbstractSavedStateStore(id) {

    companion object {
        private val preference = mmkv("savedState")
        fun clear() {
            preference.clearAll()
        }
    }

    private fun realKey(key: String): String {
        return "${this.id}_$key"
    }

    override fun contains(key: String): Boolean {
        return preference.contains(realKey(key))
    }

    override fun saveState(key: String, value: Any?) {
        preference.putData(realKey(key), value)
    }

    override fun <T> getSavedState(key: String): T? {
        return preference.getData(realKey(key))
    }

    override fun removeSaveState(key: String) {
        preference.remove(realKey(key))
    }

}



