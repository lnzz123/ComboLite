/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利。
 *
 * 本软件（包括但不限于代码、文档、资源文件等）受《中华人民共和国著作权法》及相关法律法规保护。
 * 未经本公司书面授权，任何单位或个人不得：
 * 1. 以任何形式复制、传播、修改、分发本软件的全部或部分内容；
 * 2. 将本软件用于商业目的或未经授权的第三方项目；
 * 3. 删除或篡改本软件中的版权声明、商标标识及技术标识。
 *
 * 违反上述条款者，本公司将依法追究其民事及刑事责任，并有权要求赔偿因此造成的全部经济损失。
 *
 * 授权许可请联系：贵州君城网络科技有限公司法律事务部
 * 邮箱：1755858138@qq.com
 * 电话：+86-175-85074415
 */

package com.combo.core.ext

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.combo.core.ext.ExtConstant.PLUGIN_ACTIVITY_CLASS_NAME
import com.combo.core.ext.ExtConstant.PLUGIN_SERVICE_CLASS_NAME
import com.combo.core.interfaces.IPluginActivity
import com.combo.core.interfaces.IPluginService
import com.combo.core.manager.PluginManager
import timber.log.Timber

internal object ExtConstant {
    const val PLUGIN_ACTIVITY_CLASS_NAME = "plugin_activity_class_name"
    const val PLUGIN_SERVICE_CLASS_NAME = "plugin_service_class_name"
}

/**
 * 从Intent中获取插件Activity
 * @return IPluginActivity的实例，如果找不到则返回null。
 */
fun Intent.getPluginActivity(): IPluginActivity? =
    getStringExtra(PLUGIN_ACTIVITY_CLASS_NAME)?.let {
        PluginManager.getInterface(IPluginActivity::class.java, it)
    }


/**
 * 跳转插件Activity
 *
 * @param cls 插件Activity的Class
 * @param options 可选的 Bundle，用于 Activity 启动动画等高级选项。
 * @param block 一个可选的 Lambda 表达式，用于在启动前对 Intent 进行额外配置。
 */
fun Context.startPluginActivity(
    cls: Class<out IPluginActivity>,
    options: Bundle? = null,
    block: (Intent.() -> Unit)? = null
) {

    val hostActivityClass = PluginManager.proxyManager.getHostActivity()
    if (hostActivityClass == null) {
        Timber.e("跳转失败：未在PluginManager中配置宿主Activity。")
        return
    }
    val intent = Intent(this, hostActivityClass).apply {
        putExtra(PLUGIN_ACTIVITY_CLASS_NAME, cls.name)
        block?.invoke(this)
    }
    startActivity(intent, options)
}

/**
 * 从Intent中获取插件Service的实例。
 * @return IPluginService的实例，如果找不到则返回null。
 */
fun Intent.getPluginService(): IPluginService? =
    getStringExtra(PLUGIN_SERVICE_CLASS_NAME)?.let {
        PluginManager.getInterface(IPluginService::class.java, it)
    }

/**
 * 启动一个插件Service
 * @param cls 插件Service的Class
 * @param block 一个可选的 Lambda 表达式，用于对 Intent 进行额外配置。
 */
fun Context.startPluginService(
    cls: Class<out IPluginService>,
    block: (Intent.() -> Unit)? = null
) {
    val hostServiceClass = PluginManager.proxyManager.acquireServiceProxy(cls.name)
    if (hostServiceClass == null) {
        Timber.e("启动失败 [${cls.name}]：Service服务繁忙或未找到类。")
        return
    }
    val intent = Intent(this, hostServiceClass).apply {
        putExtra(PLUGIN_SERVICE_CLASS_NAME, cls.name)
        block?.invoke(this)
    }
    startService(intent)
}

/**
 * 绑定到一个插件Service
 * @param cls 插件Service的Class
 * @param connection ServiceConnection回调
 * @param flags 绑定标志
 * @param block 一个可选的 Lambda 表达式，用于对 Intent 进行额外配置。
 */
fun Context.bindPluginService(
    cls: Class<out IPluginService>,
    connection: ServiceConnection,
    flags: Int,
    block: (Intent.() -> Unit)? = null
): Boolean {
    val hostServiceClass = PluginManager.proxyManager.acquireServiceProxy(cls.name)
    if (hostServiceClass == null) {
        Timber.e("绑定失败 [${cls.name}]：Service服务繁忙或未找到类。")
        return false
    }
    val intent = Intent(this, hostServiceClass).apply {
        putExtra(PLUGIN_SERVICE_CLASS_NAME, cls.name)
        block?.invoke(this)
    }
    return bindService(intent, connection, flags)
}

/**
 * 停止一个插件Service
 * @param cls 插件Service的Class
 * @param block 一个可选的 Lambda 表达式，用于对 Intent 进行额外配置。
 */
fun Context.stopPluginService(
    cls: Class<out IPluginService>,
    block: (Intent.() -> Unit)? = null
): Boolean {
    val hostServiceClass = PluginManager.proxyManager.getServiceProxyFor(cls.name)
    if (hostServiceClass == null) {
        Timber.w("插件服务未在运行: ${cls.name}")
        return false
    }
    val intent = Intent(this, hostServiceClass).apply {
        putExtra(PLUGIN_SERVICE_CLASS_NAME, cls.name)
        block?.invoke(this)
    }
    return stopService(intent)
}

/**
 * 发送一个应用内广播 (Internal Broadcast)
 *
 * 这个扩展函数会自动为 Intent 添加宿主的包名，确保它是一个显式的内部广播。
 * 框架内的所有广播发送都应使用此方法，以配合 ProxyManager 的 exported 安全检查。
 *
 * @param intent 要发送的广播 Intent.
 */
fun Context.sendInternalBroadcast(intent: Intent) {
    intent.setPackage(this.packageName)
    sendBroadcast(intent)
    Timber.d("已发送内部广播: ${intent.action}")
}

/**
 * 发送一个应用内广播 (Internal Broadcast)
 *
 * @param action 广播的Action字符串.
 * @param block 一个可选的 Lambda 表达式，用于对 Intent 进行额外配置。
 */
fun Context.sendInternalBroadcast(
    action: String,
    block: (Intent.() -> Unit)? = null
) {
    val intent = Intent(action).apply {
        block?.invoke(this)
    }
    sendInternalBroadcast(intent)
}