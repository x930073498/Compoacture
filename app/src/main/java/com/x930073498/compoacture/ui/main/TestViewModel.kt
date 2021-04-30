package com.x930073498.compoacture.ui.main

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.x930073498.compoacture.component.StoreViewModel
import java.util.*

class TestViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    StoreViewModel(application, savedStateHandle) {

        fun getText():String{
            return UUID.randomUUID().toString()
        }

}