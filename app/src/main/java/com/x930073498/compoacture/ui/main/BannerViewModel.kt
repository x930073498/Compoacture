package com.x930073498.compoacture.ui.main

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.x930073498.compoacture.ability.hideLoading
import com.x930073498.compoacture.ability.onViewModel
import com.x930073498.compoacture.ability.showLoading
import com.x930073498.compoacture.ability.toast
import com.x930073498.compoacture.component.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay


data class ImageAction(val image: String, val action: String)

class BannerViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    StoreViewModel(application, savedStateHandle) {
    val textLiveData by liveDataProperty<String>()
    var text by saveStateProperty("")
    val imageUrl by property("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fattach.bbs.miui.com%2Fforum%2F201308%2F23%2F220651x9b0h4kru904ozre.jpg&refer=http%3A%2F%2Fattach.bbs.miui.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1622253604&t=4f309d37b0a485b8760015604ee78798")
//    var text =""

    override fun onAttach(scope: StoreViewModelScope) {
        textLiveData.postValue("进入attach")
    }


    fun setText() {
        asLiveData(BannerViewModel::text)
        launch(Dispatchers.IO) {
            showLoading()
            delay(2000)
            hideLoading()
            toast("完成")
            text = onViewModel(TestViewModel::class) {
                this::getText.invoke()
            }
        }
    }


}