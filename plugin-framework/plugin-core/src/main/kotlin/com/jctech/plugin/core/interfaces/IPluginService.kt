package com.jctech.plugin.core.interfaces

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder

/**
 * 插件Service接口
 * 定义插件Service的基本行为和生命周期
 */
interface IPluginService {
    /**
     * 【必须】将代理Service注入插件，建立连接。这是插件生命周期的第一步。
     * 通过持有的 proxyService 引用，插件可以调用所有 Context 的方法。
     * @param proxyService 正在运行的代理Service实例。
     */
    fun onAttach(proxyService: Service)

    // 标准生命周期
    fun onCreate()

    /**
     * 对于启动状态的Service（startService），此方法被调用。
     * @return 返回值决定了Service在被系统杀死后的行为，同原生Service。
     */
    fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int

    /**
     * 对于绑定状态的Service（bindService），此方法被调用。
     * @return 返回一个IBinder对象，用于客户端与Service的交互。
     */
    fun onBind(intent: Intent?): IBinder?

    /**
     * 当所有客户端都与Service解绑时调用。
     */
    fun onUnbind(intent: Intent?): Boolean

    fun onRebind(intent: Intent?)

    fun onDestroy()

    fun onConfigurationChanged(newConfig: Configuration)

    fun onLowMemory()

    fun onTrimMemory(level: Int)
}