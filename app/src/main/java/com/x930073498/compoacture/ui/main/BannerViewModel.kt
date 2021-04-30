package com.x930073498.compoacture.ui.main

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.x930073498.compoacture.component.*
import com.x930073498.compoacture.ability.hideLoading
import com.x930073498.compoacture.ability.onViewModel
import com.x930073498.compoacture.ability.showLoading
import com.x930073498.compoacture.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay


data class ImageAction(val image: String, val action: String)

class BannerViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    StoreViewModel(application, savedStateHandle) {
    val textLiveData by liveDataProperty<String>()
    var text by saveStateProperty("1514")

    override fun onAttach(scope: StoreViewModelScope) {
        textLiveData.postValue("进入attach")
    }


    fun setText() {
        launch(Dispatchers.IO) {
            showLoading()
            delay(2000)
            hideLoading()
            toast("完成")
            text = onViewModel(TestViewModel::class){
                this::getText.invoke()
            }
        }
    }


}