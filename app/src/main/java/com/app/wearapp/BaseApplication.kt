package com.app.wearapp

import android.app.Application
import android.content.Context

class BaseApplication: Application() {

    companion object {
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}