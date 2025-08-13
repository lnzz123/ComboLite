package com.jctech.plugin.sample.screen

import android.widget.Toast
import com.jctech.plugin.core.base.BasePluginService
import timber.log.Timber

class TestPluginService : BasePluginService() {
    override fun onCreate() {
        super.onCreate()
        Timber.d("TestPluginService onCreate")
        Toast.makeText(proxyService, "插件服务已经启动", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("TestPluginService onDestroy")
        Toast.makeText(proxyService, "插件服务已经关闭", Toast.LENGTH_SHORT).show()
    }
}