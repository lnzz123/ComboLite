package com.jctech.plugin.core.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jctech.plugin.core.interfaces.IPluginReceiver
import com.jctech.plugin.core.manager.PluginManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 宿主端静态广播的统一代理。
 *
 * 这个 Receiver 在宿主的 AndroidManifest.xml 中注册，用于接收所有插件的系统广播。
 * 它会在接收到广播后，查询 PluginManager 的注册表，
 * 然后将广播事件分发给一个或多个匹配的插件 IPluginReceiver。
 */
open class BaseHostReceiver : BroadcastReceiver() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * 系统调用此方法来处理广播。
     * @param context 上下文
     * @param intent 包含广播动作和数据的Intent
     */
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action ?: return
        Timber.d("BaseHostReceiver 接收到广播: $action")

        val pendingResult = goAsync()

        coroutineScope.launch {
            try {
                if (! PluginManager.isInitialized) {
                    Timber.w("PluginManager 尚未初始化，无法分发广播。")
                    return@launch
                }

                val targetReceivers = PluginManager.proxyManager.findReceiversForAction(action)

                if (targetReceivers.isEmpty()) {
                    Timber.d("没有找到处理 [$action] 的插件接收器。")
                    return@launch
                }

                targetReceivers.forEach { receiverInfo ->
                    Timber.d("准备分发广播 [$action] 到插件 [${receiverInfo.pluginId}] 的 [${receiverInfo.className}]")
                    try {
                        val pluginReceiver = PluginManager.getInterface(
                            IPluginReceiver::class.java,
                            receiverInfo.className
                        )

                        pluginReceiver?.onReceive(context, intent)
                    } catch (e: Exception) {
                        Timber.e(e, "分发广播到 [${receiverInfo.className}] 时发生错误。")
                    }
                }
            } finally {
                pendingResult.finish()
                Timber.d("广播 [$action] 异步处理完成。")
            }
        }
    }
}
