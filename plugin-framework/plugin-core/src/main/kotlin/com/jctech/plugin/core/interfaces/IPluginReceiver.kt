package com.jctech.plugin.core.interfaces

import android.content.Context
import android.content.Intent

/**
 * 插件广播接收器接口。
 */
interface IPluginReceiver {

    /**
     * 当广播到达时，由宿主代理调用此方法。
     *
     * @param context 宿主应用的上下文。插件可以使用此上下文来执行各种操作，
     * 例如启动服务、发送通知等。
     * @param intent  包含广播动作和数据的原始 Intent 对象。
     */
    fun onReceive(context: Context, intent: Intent)
}