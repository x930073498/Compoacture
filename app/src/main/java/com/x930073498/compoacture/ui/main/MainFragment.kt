package com.x930073498.compoacture.ui.main

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.x930073498.compoacture.R
import com.x930073498.compoacture.databinding.FragmentMainBinding
import com.x930073498.compoacture.component.*
import com.x930073498.compoacture.databinder.GlideDataBinder
import com.x930073498.compoacture.databinder.TextBinder
import com.x930073498.compoacture.extentions.viewBinding
import com.x930073498.compoacture.utils.parentStoreViewModel
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
        resetDataBinder()
        addDataBinder(TextBinder)
        addDataBinder(GlideDataBinder)
        bindViewLifecycleProperty(bannerViewModel, BannerViewModel::text) {
            binding.tv.text = this
        }
        setData(
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fattach.bbs.miui.com%2Fforum%2F201308%2F23%2F220651x9b0h4kru904ozre.jpg&refer=http%3A%2F%2Fattach.bbs.miui.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1622253604&t=4f309d37b0a485b8760015604ee78798",
            binding.image
        )

        // binding.tv.text = bannerViewModel.text
    }


}