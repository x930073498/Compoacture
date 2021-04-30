package com.x930073498.compoacture.ability

import android.view.Gravity
import android.widget.Toast
import com.didi.drouter.api.DRouter
import com.x930073498.compoacture.component.*
import com.x930073498.compoacture.ui.Toaster
import com.x930073498.compoacture.utils.DefaultToaster

object EmptyToaster : Toaster {
    override fun toast(msg: CharSequence?, duration: Int, gravity: Int) {

    }

    override fun dispose() {
    }
}


class ToastAction(
    private val msg: CharSequence?,
    private val duration: Int = Toast.LENGTH_SHORT,
    private val gravity: Int = Gravity.CENTER
) : UnitAction {
    override fun action(storeViewModel: IStoreViewModel, scope: StoreViewModelScope) {
        scope.toaster.toast(msg, duration, gravity)
    }

}

fun IStoreViewModel.toast(
    msg: CharSequence?,
    duration: Int = Toast.LENGTH_SHORT,
    gravity: Int = Gravity.CENTER
) {
    pushAction(ToastAction(msg, duration, gravity))
}

fun StoreViewModelScope.toast(
    msg: CharSequence?,
    duration: Int = Toast.LENGTH_SHORT,
    gravity: Int = Gravity.CENTER
) {
    storeViewModel.toast(msg, duration, gravity)
}

var StoreViewModelScope.toaster: Toaster
    get() {
        return fromViewModel {
            getOrCreate<Toaster>("d9e46913-cf0e-447b-9900-ae17b72faa0a") {
                EmptyToaster
            }
        }
    }
    set(value) {
        fromViewModel { putCache("d9e46913-cf0e-447b-9900-ae17b72faa0a", value) }
    }


fun StoreViewModelScope.bindToaster(
    toaster: Toaster = DRouter.build(Toaster::class.java)
        .setDefaultIfEmpty(DefaultToaster())
        .getService(this)
) {
    this.toaster = toaster
}
