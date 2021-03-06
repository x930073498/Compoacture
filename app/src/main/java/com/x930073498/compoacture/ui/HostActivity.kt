package com.x930073498.compoacture.ui

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit
import com.gyf.immersionbar.ktx.immersionBar
import com.x930073498.compoacture.R
import com.x930073498.compoacture.ability.bindLoading
import com.x930073498.compoacture.ability.bindToaster
import com.x930073498.compoacture.component.withViewModel
import com.x930073498.compoacture.databinding.ActivityHostBinding
import com.x930073498.compoacture.extentions.viewBinding
import com.x930073498.compoacture.ui.main.BannerViewModel
import com.x930073498.compoacture.ui.main.MainFragment
import com.x930073498.compoacture.utils.logE
import com.x930073498.compoacture.utils.withStoreViewModelProvider

class HostActivity : AppCompatActivity() {
    private val binding: ActivityHostBinding by viewBinding(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        immersionBar {
            transparentStatusBar()
        }
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.fragmentContainer, MainFragment())
                addToBackStack(null)
            }
            withStoreViewModelProvider {
                bindLoading()
                bindToaster()
                withViewModel(BannerViewModel::class) {
                    setText()
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        logE("enter this line OnDestroy")

    }


}