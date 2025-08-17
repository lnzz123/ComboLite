/*
 *
 *  * Copyright (c) 2025, 贵州君城网络科技有限公司
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.combo.core.base

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Process
import com.combo.core.manager.PluginManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

/**
 * 宿主端的 ContentProvider 统一代理。
 *
 * 这个 Provider 在宿主的 AndroidManifest.xml 中注册，拥有一个固定的 Authority。
 * 它负责接收发往插件 Provider 的所有请求，然后将请求动态转发给实际的插件 Provider。
 *
 * ### URI 转发约定：
 * 为了让 HostProvider 知道要将请求转发给哪个插件 Provider，客户端需要使用一个特殊的 URI 格式：
 * `content://[HOST_AUTHORITY]/[插件Provider的完整类名]/[原始路径]`
 *
 * 例如，如果宿主 Authority 是 `com.jctech.plugin.sample.proxy.provider`，
 * 插件 Provider 类名是 `com.example.plugin.MyPluginProvider`，
 * 原始 URI 是 `content://com.example.plugin.provider/items/1`，
 * 那么客户端应该构建并使用的 URI 是：
 * `content://com.jctech.plugin.sample.proxy.provider/com.example.plugin.MyPluginProvider/items/1`
 */
open class BaseHostProvider : ContentProvider() {
    private val providerCache = ConcurrentHashMap<String, ContentProvider>()

    /**
     * 此方法在应用启动的早期被调用。
     * 我们在这里执行插件框架的初始化，以确保 PluginManager 在被使用前已准备就绪。
     */
    override fun onCreate(): Boolean {
        if (!PluginManager.isInitialized) {
            val appContext = context?.applicationContext
            if (appContext == null) {
                Timber.e("无法获取 Application Context，插件框架初始化失败！")
                return false
            }
            val application = appContext as Application
            PluginManager.initialize(application) {
                CoroutineScope(Dispatchers.Default).launch {
                    PluginManager.loadEnabledPlugins()
                }
            }
        }

        return true
    }

    /**
     * 核心辅助方法：根据代理 URI 获取目标插件 Provider 的实例。
     */
    private fun getTargetProvider(uri: Uri): ContentProvider? {
        // 从 URI 的第一个路径段中解析出目标 Provider 的类名
        val className =
            uri.pathSegments.getOrNull(0) ?: run {
                Timber.e("无法从URI中解析出插件Provider类名: $uri")
                return null
            }
        val decodedClassName = URLDecoder.decode(className, "UTF-8")

        providerCache[decodedClassName]?.let { return it }

        return try {
            val instance = PluginManager.getInterface(ContentProvider::class.java, decodedClassName)
            if (instance != null) {
                instance.attachInfo(context, null)
                instance.onCreate()
                providerCache[decodedClassName] = instance
                Timber.d("已创建并缓存插件 Provider 实例: $decodedClassName")
            } else {
                Timber.e("无法创建插件 Provider 实例: $decodedClassName")
            }
            instance
        } catch (e: Exception) {
            Timber.e(e, "创建插件 Provider 实例时发生严重错误: $decodedClassName")
            null
        }
    }

    /**
     * 核心辅助方法：将代理 URI 重写为插件 Provider 能识别的原始 URI。
     */
    private fun rewriteUri(proxyUri: Uri): Uri? {
        val className = proxyUri.pathSegments.getOrNull(0) ?: return null
        val decodedClassName = URLDecoder.decode(className, "UTF-8")

        // 从 ProxyManager 中查找该 Provider 的注册信息，以获取其原始 Authority
        val providerInfo = PluginManager.proxyManager.findProviderInfoByClassName(decodedClassName)
        if (providerInfo == null || providerInfo.authorities.isEmpty()) {
            Timber.e("无法找到 $decodedClassName 的注册信息或 Authority。")
            return null
        }
        // 默认使用第一个 Authority
        val originalAuthority = providerInfo.authorities[0]

        // 开始重构 URI
        val builder =
            Uri
                .Builder()
                .scheme(proxyUri.scheme)
                .authority(originalAuthority)

        // 拼接 URI 路径
        for (i in 1 until proxyUri.pathSegments.size) {
            builder.appendPath(proxyUri.pathSegments[i])
        }

        // 复制查询参数和片段
        if (proxyUri.query != null) {
            builder.encodedQuery(proxyUri.encodedQuery)
        }
        if (proxyUri.fragment != null) {
            builder.encodedFragment(proxyUri.encodedFragment)
        }

        return builder.build()
    }

    /**
     * 高阶函数，封装请求转发的重复逻辑，并增加安全检查。
     */
    private inline fun <T> withForwardedRequest(
        uri: Uri,
        block: (provider: ContentProvider, rewrittenUri: Uri) -> T?,
    ): T? {
        // 1. 解析出目标 Provider 的类名
        val className = uri.pathSegments.getOrNull(0)?.let { URLDecoder.decode(it, "UTF-8") }
        if (className == null) {
            Timber.e("无法从URI中解析出插件Provider类名: $uri")
            return null
        }

        // 2. 从ProxyManager获取其完整的注册信息
        val providerInfo = PluginManager.proxyManager.findProviderInfoByClassName(className)
        if (providerInfo == null) {
            throw SecurityException("拦截：目标 Provider [$className] 未注册。")
        }

        // 3. 核心安全检查：检查 exported 属性
        if (!providerInfo.exported) {
            val callingUid = Binder.getCallingUid()
            val myUid = Process.myUid()
            if (callingUid != myUid) {
                throw SecurityException("权限拒绝：Provider ${providerInfo.className} 未导出，无法被外部应用访问。")
            }
        }

        // --- 安全检查结束，后续流程不变 ---

        val targetProvider = getTargetProvider(uri) ?: return null
        val rewrittenUri = rewriteUri(uri) ?: return null
        return block(targetProvider, rewrittenUri)
    }

    // --- 将所有 ContentProvider 的方法转发给目标 Provider ---

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? =
        withForwardedRequest(uri) { provider, rewritten ->
            provider.query(rewritten, projection, selection, selectionArgs, sortOrder)
        }

    override fun getType(uri: Uri): String? =
        withForwardedRequest(uri) { provider, rewritten ->
            provider.getType(rewritten)
        }

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? =
        withForwardedRequest(uri) { provider, rewritten ->
            provider.insert(rewritten, values)
        }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int =
        withForwardedRequest(uri) { provider, rewritten ->
            provider.delete(rewritten, selection, selectionArgs)
        } ?: 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int =
        withForwardedRequest(uri) { provider, rewritten ->
            provider.update(rewritten, values, selection, selectionArgs)
        } ?: 0
}
