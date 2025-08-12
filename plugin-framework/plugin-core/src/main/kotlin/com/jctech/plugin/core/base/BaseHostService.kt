package com.jctech.plugin.core.base

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Compose插件框架Service基类
 * 定义插件Service的基本行为和生命周期
 */
open class BaseHostService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
