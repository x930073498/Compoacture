package com.x930073498.compoacture.databinder

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.x930073498.compoacture.R
import com.x930073498.compoacture.component.*

object GlideFeature : DataBinder.Feature

object GlideDataBinder : ViewDataReverseBinder<Any, ImageView> {
    override val features: List<DataBinder.Feature>
        get() = super.features + GlideFeature

    override fun bind(value: Any?, target: ImageView, scope: StoreViewModelScope) {
        if (scope.environment.isActivity) {
            Glide.with(scope.environment.hostActivity).load(value).into(target)
            target.setTag(R.id.data_binder_reverse_tag, value)
            return
        }
        if (scope.environment.isFragment) {
            Glide.with(scope.environment.asFragment()).load(value).into(target)
            target.setTag(R.id.data_binder_reverse_tag, value)
        }

    }

    override fun value(target: ImageView, scope: StoreViewModelScope): Any? {
        return target.getTag(R.id.data_binder_reverse_tag)?:target.drawable
    }
}