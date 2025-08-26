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

@file:Suppress("KDocUnresolvedReference")

package com.combo.core.base

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Process
import com.combo.core.manager.PluginManager
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
 * `content://[HOST_AUTHORITY]/[PLUGIN_AUTHORITY]/[原始路径]`
 *
 * 例如，如果宿主 Authority 是 `com.plugin.sample.proxy.provider`，
 * 插件 Provider Authority 是 `com.example.plugin.provider`，
 * 原始 URI 是 `content://com.example.plugin.provider/items/1`，
 * 那么客户端应该构建并使用的 URI 是：
 * `content://com.plugin.sample.proxy.provider/com.example.plugin.provider/items/1`
 */
open class BaseHostProvider : ContentProvider() {
    companion object {
        const val KEY_TARGET_URI = "com.combo.core.base.BaseHostProvider.TARGET_URI"
    }

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
                PluginManager.loadEnabledPlugins()
            }
        }

        return true
    }

    private fun getTargetProvider(className: String): ContentProvider? {
        providerCache[className]?.let { return it }

        return try {
            val instance = PluginManager.getInterface(ContentProvider::class.java, className)
            if (instance != null) {
                instance.attachInfo(context, null)
                providerCache[className] = instance
                Timber.d("已创建并缓存插件 Provider 实例: $className")
            } else {
                Timber.e("无法创建插件 Provider 实例: $className")
            }
            instance
        } catch (e: Exception) {
            Timber.e(e, "创建插件 Provider 实例时发生严重错误: $className")
            null
        }
    }

    /**
     * 将代理 URI "还原" 回插件的原生 URI
     */
    private fun rewriteUri(proxyUri: Uri, providerInfo: com.combo.core.model.ProviderInfo): Uri {
        val pathSegments = proxyUri.pathSegments
        val originalAuthority = providerInfo.authorities.first()

        val originalPath = pathSegments.drop(1).joinToString("/")

        return proxyUri.buildUpon()
            .authority(originalAuthority)
            .path(originalPath)
            .clearQuery()
            .fragment(null)
            .build()
    }

    /**
     * 高阶函数，封装请求转发的重复逻辑
     */
    private inline fun <T> withForwardedRequest(
        uri: Uri,
        block: (provider: ContentProvider, rewrittenUri: Uri) -> T?,
    ): T? {
        val pluginAuthority = uri.pathSegments.getOrNull(0)?.let { URLDecoder.decode(it, "UTF-8") }
            ?: throw IllegalArgumentException("无法从 URI 中解析出插件 Authority: $uri")

        val providerInfo = PluginManager.proxyManager.findProviderInfoByAuthority(pluginAuthority)
            ?: throw SecurityException("拦截：目标 Provider Authority [$pluginAuthority] 未在 PluginManager 中注册。")

        val className = providerInfo.className

        if (!providerInfo.exported && Binder.getCallingUid() != Process.myUid()) {
            throw SecurityException("权限拒绝：Provider ${providerInfo.className} 未导出。")
        }

        val targetProvider = getTargetProvider(className)
            ?: throw IllegalStateException("无法创建或获取 Provider 实例: $className")

        val rewrittenUri = rewriteUri(uri, providerInfo)

        return block(targetProvider, rewrittenUri)
    }

    // --- CRUD 方法保持不变，因为它们都依赖 withForwardedRequest ---

    override fun query(
        uri: Uri,
        p: Array<String>?,
        s: String?,
        sa: Array<String>?,
        so: String?
    ): Cursor? =
        withForwardedRequest(uri) { provider, rewritten -> provider.query(rewritten, p, s, sa, so) }

    override fun getType(uri: Uri): String? =
        withForwardedRequest(uri) { provider, rewritten -> provider.getType(rewritten) }

    // [推荐修改] 优化 insert 的返回值处理，使其更健壮
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        var result: Uri? = null
        withForwardedRequest(uri) { provider, rewritten ->
            provider.insert(rewritten, values)?.also { originalResultUri ->
                // 使用原始代理 URI 通知观察者
                context?.contentResolver?.notifyChange(uri, null)

                // [修改] 采用更健壮的方式将插件返回的原始 URI 转换回代理 URI
                // originalResultUri: content://plugin.authority/path/id
                // 我们需要构建: content://host.authority/plugin.authority/path/id
                val pluginAuthority = originalResultUri.authority
                val hostAuthority = PluginManager.proxyManager.getHostProviderAuthority()

                result = originalResultUri.buildUpon()
                    .authority(hostAuthority)
                    .path("/$pluginAuthority${originalResultUri.path}")
                    .build()
            }
        }
        return result
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
        withForwardedRequest(uri) { provider, rewritten ->
            provider.delete(rewritten, selection, selectionArgs).also { deletedCount ->
                if (deletedCount > 0) {
                    context?.contentResolver?.notifyChange(uri, null)
                }
            }
        } ?: 0

    override fun update(uri: Uri, v: ContentValues?, s: String?, sa: Array<String>?): Int =
        withForwardedRequest(uri) { provider, rewritten ->
            provider.update(rewritten, v, s, sa).also { updatedCount ->
                if (updatedCount > 0) {
                    context?.contentResolver?.notifyChange(uri, null)
                }
            }
        } ?: 0

    @Suppress("DEPRECATION")
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras?.getParcelable(KEY_TARGET_URI, Uri::class.java)
        } else {
            extras?.getParcelable(KEY_TARGET_URI)
        }
            ?: throw IllegalArgumentException("无法处理 call 请求：extras 中缺少目标 Uri (KEY_TARGET_URI)")

        extras?.remove(KEY_TARGET_URI)

        return withForwardedRequest(targetUri) { provider, _ ->
            provider.call(method, arg, extras)
        }
    }
}