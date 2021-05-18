package com.x930073498.compoacture.databinder

import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.bumptech.glide.Glide

fun interface DataBinder<VALUE> {
    fun bind(value: VALUE)
}

fun interface DataChangeListener<VALUE> {
    fun onDataChanged(value: VALUE)
}

interface DataReverseBinder<VALUE> : DataBinder<VALUE> {
    fun register(listener: DataChangeListener<VALUE>)
}

class TextBinder(private val tv: TextView) : DataBinder<CharSequence> {
    override fun bind(value: CharSequence) {
        tv.text = value
    }
}

class GlideBinder(private val image: ImageView) : DataBinder<Any> {
    override fun bind(value: Any) {
        Glide.with(image)
            .load(value)
            .into(image)
    }

}

open class EditTextBinder(private val et: EditText) : DataBinder<CharSequence> {
    override fun bind(value: CharSequence) {
        et.setText(value)
        et.setSelection(value.length)
    }
}

class EditTextReverseBinder(private val et: EditText) : EditTextBinder(et),
    DataReverseBinder<CharSequence> {
    private var current: CharSequence? = null
    override fun bind(value: CharSequence) {
        if (current != value) {
            current = value
            if (et.text.toString() != value) {
                et.setText(value)
                et.setSelection(value.length)
            }
        }
    }

    override fun register(listener: DataChangeListener<CharSequence>) {
        et.doAfterTextChanged {
            val value = it.toString()
            if (current != value) {
                listener.onDataChanged(value)
            }
        }
    }


}
