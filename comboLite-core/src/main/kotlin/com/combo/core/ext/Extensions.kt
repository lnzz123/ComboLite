/*
 * Copyright (c) 2025, 贵州君城网络科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package com.combo.core.ext

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.combo.core.base.BaseHostProvider.Companion.KEY_TARGET_URI
import com.combo.core.ext.ExtConstant.PLUGIN_ACTIVITY_CLASS_NAME
import com.combo.core.ext.ExtConstant.PLUGIN_SERVICE_CLASS_NAME
import com.combo.core.ext.ExtConstant.PLUGIN_SERVICE_INSTANCE_ID
import com.combo.core.interfaces.IPluginActivity
import com.combo.core.interfaces.IPluginService
import com.combo.core.manager.PluginManager
import timber.log.Timber
import java.net.URLEncoder

internal object ExtConstant {
    const val PLUGIN_ACTIVITY_CLASS_NAME = "plugin_activity_class_name"
    const val PLUGIN_SERVICE_CLASS_NAME = "plugin_service_class_name"
    const val PLUGIN_SERVICE_INSTANCE_ID = "plugin_service_instance_id"
}

private fun generateInstanceId(className: String, instanceId: String?): String {
    return if (instanceId.isNullOrEmpty()) className else "$className:$instanceId"
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
 * @param instanceId 可选的字符串，用于标识插件Service的实例。
 * @param block 一个可选的 Lambda 表达式，用于对 Intent 进行额外配置。
 */
fun Context.startPluginService(
    cls: Class<out IPluginService>,
    instanceId: String? = null, // 新增参数
    block: (Intent.() -> Unit)? = null
) {
    val instanceIdentifier = generateInstanceId(cls.name, instanceId)
    val hostServiceClass = PluginManager.proxyManager.acquireServiceProxy(instanceIdentifier)
    if (hostServiceClass == null) {
        Timber.e("启动失败 [$instanceIdentifier]：Service服务繁忙或未找到类。")
        return
    }
    val intent = Intent(this, hostServiceClass).apply {
        putExtra(PLUGIN_SERVICE_CLASS_NAME, cls.name)
        putExtra(PLUGIN_SERVICE_INSTANCE_ID, instanceIdentifier)
        block?.invoke(this)
    }
    startService(intent)
}

/**
 * 绑定到一个插件Service
 * @param cls 插件Service的Class
 * @param instanceId 可选的字符串，用于标识插件Service的实例。
 * @param connection ServiceConnection回调
 * @param flags 绑定标志
 * @param block 一个可选的 Lambda 表达式，用于对 Intent 进行额外配置。
 */
fun Context.bindPluginService(
    cls: Class<out IPluginService>,
    instanceId: String? = null,
    connection: ServiceConnection,
    flags: Int,
    block: (Intent.() -> Unit)? = null
): Boolean {
    val instanceIdentifier = generateInstanceId(cls.name, instanceId)
    val hostServiceClass = PluginManager.proxyManager.acquireServiceProxy(instanceIdentifier)
    if (hostServiceClass == null) {
        Timber.e("绑定失败 [${cls.name}]：Service服务繁忙或未找到类。")
        return false
    }
    val intent = Intent(this, hostServiceClass).apply {
        putExtra(PLUGIN_SERVICE_CLASS_NAME, cls.name)
        putExtra(PLUGIN_SERVICE_INSTANCE_ID, instanceIdentifier)
        block?.invoke(this)
    }
    return bindService(intent, connection, flags)
}

/**
 * 停止一个插件Service
 * @param cls 插件Service的Class
 * @param instanceId 可选的字符串，用于标识插件Service的实例。
 * @param block 一个可选的 Lambda 表达式，用于对 Intent 进行额外配置。
 */
fun Context.stopPluginService(
    cls: Class<out IPluginService>,
    instanceId: String? = null,
    block: (Intent.() -> Unit)? = null
): Boolean {
    val instanceIdentifier = generateInstanceId(cls.name, instanceId)
    val hostServiceClass = PluginManager.proxyManager.getServiceProxyFor(instanceIdentifier)
    if (hostServiceClass == null) {
        Timber.w("插件服务未在运行: ${cls.name}")
        return false
    }
    val intent = Intent(this, hostServiceClass).apply {
        putExtra(PLUGIN_SERVICE_CLASS_NAME, cls.name)
        putExtra(PLUGIN_SERVICE_INSTANCE_ID, instanceIdentifier)
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

/**
 * 构建插件 Provider 的代理 URI。
 * @param pluginUri 插件的原始 Uri，例如 `BookProvider.CONTENT_URI`。
 * @return 可以在宿主 ContentResolver 中使用的代理 Uri。
 */
fun buildProxyUri(pluginUri: Uri): Uri {
    val hostAuthority = PluginManager.proxyManager.getHostProviderAuthority()
        ?: throw IllegalStateException("HostProvider authority has not been configured in PluginManager.")

    val pluginAuthority = pluginUri.authority
        ?: throw IllegalArgumentException("输入的 URI 没有 Authority: $pluginUri")

    PluginManager.proxyManager.findProviderInfoByAuthority(pluginAuthority)
        ?: throw IllegalArgumentException("Authority [$pluginAuthority] 未被注册为插件 Provider。")

    val encodedPluginAuthority = URLEncoder.encode(pluginAuthority, "UTF-8")

    val path = pluginUri.path
    val proxyPath =
        if (path.isNullOrEmpty() || path == "/") encodedPluginAuthority else "$encodedPluginAuthority$path"

    return pluginUri.buildUpon()
        .authority(hostAuthority)
        .encodedPath(proxyPath)
        .build()
}

/**
 * 查询一个插件 ContentProvider。
 * @param uri 插件的原始 Uri，例如 `BookProvider.CONTENT_URI`。
 * @param projection 要返回的列。
 * @param selection 筛选条件。
 * @param selectionArgs 筛选条件的参数。
 * @param sortOrder 排序顺序。
 * @return 一个 Cursor 对象，包含查询结果。
 */
fun ContentResolver.queryPlugin(
    uri: Uri,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?
): Cursor? {
    val proxyUri = buildProxyUri(uri)
    return this.query(proxyUri, projection, selection, selectionArgs, sortOrder)
}

/**
 * 向一个插件 ContentProvider 插入数据。
 * @param uri 插件的原始 Uri，例如 `BookProvider.CONTENT_URI`。
 * @param values 要插入的键值对。
 * @return 新插入数据的 Uri。
 */
fun ContentResolver.insertPlugin(
    uri: Uri,
    values: ContentValues?
): Uri? {
    val proxyUri = buildProxyUri(uri)
    return this.insert(proxyUri, values)
}

/**
 * 从一个插件 ContentProvider 删除数据。
 * @param uri 插件的原始 Uri，例如 `BookProvider.CONTENT_URI`。
 * @param selection 筛选条件。
 * @param selectionArgs 筛选条件的参数。
 * @return 删除的行数。
 */
fun ContentResolver.deletePlugin(
    uri: Uri,
    selection: String?,
    selectionArgs: Array<String>?
): Int {
    val proxyUri = buildProxyUri(uri)
    return this.delete(proxyUri, selection, selectionArgs)
}

/**
 * 更新一个插件 ContentProvider 中的数据。
 * @param uri 插件的原始 Uri，例如 `BookProvider.CONTENT_URI`。
 * @param values 要更新的键值对。
 * @param selection 筛选条件。
 * @param selectionArgs 筛选条件的参数。
 * @return 更新的行数。
 */
fun ContentResolver.updatePlugin(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<String>?
): Int {
    val proxyUri = buildProxyUri(uri)
    return this.update(proxyUri, values, selection, selectionArgs)
}

/**
 * 调用一个插件 ContentProvider 的方法。
 * @param uri 插件的原始 Uri，例如 `BookProvider.CONTENT_URI`。
 * @param method 要调用的方法名。
 * @param arg 方法的参数。
 * @param extras 额外的参数。
 * @return 方法调用的结果。
 */
fun ContentResolver.callPlugin(
    uri: Uri,
    method: String,
    arg: String? = null,
    extras: Bundle? = null
): Bundle? {
    val proxyUri = buildProxyUri(uri)

    val finalExtras = (extras ?: Bundle()).apply {
        putParcelable(KEY_TARGET_URI, uri)
    }

    return this.call(proxyUri, method, arg, finalExtras)
}

/**
 * 为一个插件 ContentProvider 安全地注册一个内容观察者。
 * @param uri 插件的原始 Uri，例如 `BookProvider.CONTENT_URI`。
 * @param notifyForDescendants 是否通知后代 Uri。
 * @param observer 要注册的内容观察者。
 */
fun ContentResolver.registerPluginObserver(
    uri: Uri,
    notifyForDescendants: Boolean,
    observer: ContentObserver
) {
    val proxyUri = buildProxyUri(uri)
    this.registerContentObserver(proxyUri, notifyForDescendants, observer)
}

/**
 * 注销内容观察者。
 * @param observer 要注销的内容观察者。
 */
fun ContentResolver.unregisterPluginObserver(observer: ContentObserver) {
    this.unregisterContentObserver(observer)
}