package com.x930073498.compoacture

import androidx.multidex.MultiDexApplication

class App:MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        isDebug=true

    }
}