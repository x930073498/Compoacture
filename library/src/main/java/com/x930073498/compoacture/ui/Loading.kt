package com.x930073498.compoacture.ui

import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.x930073498.compoacture.component.Disposable


interface Loading : Disposable {
    fun show(maxTime: Long = -1, cancelable: Boolean = true)

    fun hide()
}

class SmartRefreshLoading(private val refresh: SmartRefreshLayout) : Loading {
    override fun show(maxTime: Long, cancelable: Boolean) {
        refresh.autoRefresh()
    }

    override fun hide() {
        refresh.finishLoadMore()
        refresh.finishRefresh()
    }

    override fun dispose() {
        hide()
    }

}