package com.x930073498.compoacture.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import com.bumptech.glide.Glide
import com.x930073498.compoacture.R
import com.x930073498.compoacture.component.*
import com.x930073498.compoacture.databinder.*
import com.x930073498.compoacture.databinding.FragmentMainBinding
import com.x930073498.compoacture.extentions.viewBinding
import com.x930073498.compoacture.utils.logE
import com.x930073498.compoacture.utils.parentStoreViewModel
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowViaChannel
import java.util.*

class MainFragment : Fragment(R.layout.fragment_main), StoreViewModelScope {
    private val binding: FragmentMainBinding by viewBinding()
    private val bannerViewModel: BannerViewModel by parentStoreViewModel()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener {
            bannerViewModel.text = UUID.randomUUID().toString()
        }
        doOnBackPressedCallback(true) {
            parentFragmentManager.popBackStack()
        }
//
//        bindViewLifecycleViewModelProperty(bannerViewModel, BannerViewModel::text) {
//            binding.tv.text = this
//        }

        bindViewLifecycleViewModelProperty(
            bannerViewModel,
            BannerViewModel::text,
            TextBinder(binding.tv) ,
            EditTextReverseBinder(binding.et)
        )

        bindViewLifecycleViewModelProperty(
            bannerViewModel,
            BannerViewModel::imageUrl,
            GlideBinder(binding.image)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        logE("enter this line MainFragment Destroyed")
    }


}