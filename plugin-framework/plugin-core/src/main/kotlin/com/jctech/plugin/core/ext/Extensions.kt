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

package com.jctech.plugin.core.ext

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import com.jctech.plugin.core.ext.ExtConstant.PLUGIN_ACTIVITY_CLASS_NAME
import com.jctech.plugin.core.interfaces.IPluginActivity
import com.jctech.plugin.core.interfaces.IPluginService
import com.jctech.plugin.core.manager.PluginManager
import timber.log.Timber

internal object ExtConstant {
    const val PLUGIN_ACTIVITY_CLASS_NAME = "plugin_activity_class_name"
    const val PLUGIN_SERVICE_CLASS_NAME = "plugin_service_class_name"
}

/**
 * 从Intent中获取插件Activity
 * @return IPluginActivity的实例，如果找不到则返回null。
 */
fun Intent.getPluginActivity(): IPluginActivity? {
    return getStringExtra(PLUGIN_ACTIVITY_CLASS_NAME)?.let {
        PluginManager.getInterface(IPluginActivity::class.java, it)
    }
}

/**
 * 跳转插件Activity
 * @param cls 插件Activity的Class
 */
fun Context.startPluginActivity(cls: Class<out IPluginActivity>) {
    val hostActivity = PluginManager.proxyManager.getHostActivity()
    if (hostActivity == null) {
        Timber.e("跳转失败：未在PluginManager中配置宿主Activity。")
        return
    }
    val intent = Intent(this, hostActivity)
    intent.putExtra(PLUGIN_ACTIVITY_CLASS_NAME, cls.name)
    startActivity(intent)
}

/**
 * 从Intent中获取插件Service的实例。
 * @return IPluginService的实例，如果找不到则返回null。
 */
fun Intent.getPluginService(): IPluginService? {
    return getStringExtra(ExtConstant.PLUGIN_SERVICE_CLASS_NAME)?.let {
        PluginManager.getInterface(IPluginService::class.java, it)
    }
}

/**
 * 启动一个插件Service。
 */
fun Context.startPluginService(cls: Class<out IPluginService>) {
    val hostServiceClass = PluginManager.proxyManager.acquireServiceProxy(cls.name)
    if (hostServiceClass == null) {
        Timber.e("启动失败 [${cls.name}]：Service服务繁忙。")
        return
    }
    val intent = Intent(this, hostServiceClass)
    intent.putExtra(ExtConstant.PLUGIN_SERVICE_CLASS_NAME, cls.name)
    startService(intent)
}

/**
 * 绑定到一个插件Service。
 */
fun Context.bindPluginService(cls: Class<out IPluginService>, connection: ServiceConnection, flags: Int): Boolean {
    val hostServiceClass = PluginManager.proxyManager.acquireServiceProxy(cls.name)
    if (hostServiceClass == null) {
        Timber.e("绑定失败 [${cls.name}]：Service服务繁忙。")
        return false
    }
    val intent = Intent(this, hostServiceClass)
    intent.putExtra(ExtConstant.PLUGIN_SERVICE_CLASS_NAME, cls.name)
    return bindService(intent, connection, flags)
}

/**
 * 停止一个插件Service。
 */
fun Context.stopPluginService(cls: Class<out IPluginService>): Boolean {
    val hostServiceClass = PluginManager.proxyManager.getServiceProxyFor(cls.name)
    if (hostServiceClass == null) {
        Timber.w("插件服务未在运行: ${cls.name}")
        return false
    }
    val intent = Intent(this, hostServiceClass)
    intent.putExtra(ExtConstant.PLUGIN_SERVICE_CLASS_NAME, cls.name)
    return stopService(intent)
}

