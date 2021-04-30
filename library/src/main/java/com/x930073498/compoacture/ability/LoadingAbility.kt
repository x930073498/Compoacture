package com.x930073498.compoacture.ability

import com.didi.drouter.api.DRouter
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.x930073498.compoacture.R
import com.x930073498.compoacture.component.*
import com.x930073498.compoacture.dialog.*
import com.x930073498.compoacture.ui.Loading
import com.x930073498.compoacture.ui.SmartRefreshLoading
import per.goweii.anylayer.AnyLayer
import per.goweii.anylayer.ktx.setCancelableOnClickKeyBack
import per.goweii.anylayer.ktx.setCancelableOnTouchOutside
import java.lang.ref.WeakReference

class DefaultLoading internal constructor(private val scope: StoreViewModelScope) :
    Loading {
    override fun show(
        maxTime: Long,
        cancelable: Boolean,
    ) {
        DialogManager.show {
            scope(scope)
            layer {
                AnyLayer.dialog(environment.context)
                    .contentView(R.layout.default_loading)
                    .setCancelableOnClickKeyBack(cancelable)
                    .setCancelableOnTouchOutside(cancelable)

            }
            component(scope)
            property {
                displayTime = maxTime
                layerChannel = LayerChannel.Loading
                tag = scope.fromViewModel { this.storeId }
            }
        }

    }

    override fun dispose() {
        scope.hideLoading()
    }

    override fun hide() {
        val provider = scope
        DialogManager.requestStore(provider)
            .cancel(tag = provider.fromViewModel { this.storeId })
    }


}

class LoadingAction(private val maxTime: Long, private val cancelable: Boolean) : Action<Unit> {
    var ref = WeakReference<StoreViewModelScope>(null)
    override fun action(storeViewModel: IStoreViewModel, scope: StoreViewModelScope) {
        storeViewModel.putCache("3765edd8-2435-41be-b671-83748053498b",this)
        ref = WeakReference(scope)
        scope.loading.show(maxTime, cancelable)
    }

    override fun dispose() {
        ref.get()?.loading?.hide()
    }

}

class HideLoadingAction : Action<Unit> {
    override fun action(storeViewModel: IStoreViewModel, scope: StoreViewModelScope) {
      scope.loading.hide()
    }

}


fun IStoreViewModel.showLoading(
    maxTime: Long = -1, cancelable: Boolean = true
) {
    pushAction(LoadingAction(maxTime, cancelable))
}

fun StoreViewModelScope.showLoading(
    maxTime: Long = -1, cancelable: Boolean = true,
) {
    storeViewModel.showLoading(maxTime, cancelable)
}

fun IStoreViewModel.hideLoading() {
    pushAction(HideLoadingAction())
}

fun StoreViewModelScope.hideLoading() {
    storeViewModel.hideLoading()
}

var StoreViewModelScope.loading: Loading
    get() {
        return fromViewModel {
            getOrCreate("a8f37bab-23c3-472c-89e2-bad044bb3a7c") {
                DRouter.build(Loading::class.java)
                    .setDefaultIfEmpty(DefaultLoading(this@loading))
                    .getService(this@loading)
            }
        }
    }
    set(value) {
        fromViewModel { putCache("a8f37bab-23c3-472c-89e2-bad044bb3a7c", value) }
    }


fun StoreViewModelScope.replaceLoading(refresh: SmartRefreshLayout) {
    loading= SmartRefreshLoading(refresh)
}
