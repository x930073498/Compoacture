package com.x930073498.compoacture.databinder

import android.widget.TextView
import com.x930073498.compoacture.component.StoreViewModelScope
import com.x930073498.compoacture.component.ViewDataReverseBinder

object TextBinder : ViewDataReverseBinder<CharSequence, TextView> {


    override fun bind(
        value: CharSequence?,
        target: TextView,
        scope: StoreViewModelScope
    ) {
        target.text = value
    }

    override fun value(target: TextView, scope: StoreViewModelScope): CharSequence? {
        return target.text
    }
}