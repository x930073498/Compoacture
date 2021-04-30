package com.x930073498.compoacture.utils

import android.view.Gravity
import android.widget.Toast
import com.didi.drouter.api.DRouter
import com.x930073498.compoacture.dialog.*
import com.x930073498.compoacture.ui.Toaster
import per.goweii.anylayer.AnyLayer

class DefaultToaster : Toaster {
    private val toastTag = "0959fce6-e591-4279-853b-4d1f80ab5998"

    override fun toast(msg: CharSequence?, duration: Int, gravity: Int) {
        if (msg.isNullOrEmpty()) return
        DialogManager.requestApplicationStore().cancel(tag = toastTag)
        DialogManager.requestApplicationStore().show {
            layer {
                AnyLayer.toast(environment.context)
                    .gravity(gravity)
                    .alpha(0.7f)
                    .duration(-1)
                    .backgroundColorRes(android.R.color.black)
                    .textColorRes(android.R.color.white)
                    .message(msg)
            }
            property {
                displayTime =
                    if (duration == Toast.LENGTH_SHORT) 2000 else if (duration == Toast.LENGTH_LONG) 3500 else duration.toLong()
                tag = toastTag
                layerChannel = LayerChannel.create(3)
            }
            currentActivityComponent()
        }
    }

    override fun dispose() {
        DialogManager.requestApplicationStore().cancel(tag = toastTag)
    }

}

val toaster: Toaster
    get() {
        return DRouter.build(Toaster::class.java)
            .setDefaultIfEmpty(DefaultToaster())
            .getService()
    }

