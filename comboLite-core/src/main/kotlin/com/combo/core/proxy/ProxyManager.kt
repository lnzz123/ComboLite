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

package com.combo.core.proxy

import android.app.Application
import android.content.Intent
import com.combo.core.base.BaseHostActivity
import com.combo.core.base.BaseHostService
import com.combo.core.model.ProviderInfo
import com.combo.core.model.StaticReceiverInfo
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 插件四大组件代理管理器
 *
 * 负责统一管理和调度 Activity、Service 等组件的代理资源。
 * 它被设计为一个可配置的管理器，通过独立的 set 方法来注册不同组件的代理。
 * - **Activity**: 采用单一宿主模式，整个应用共享一个代理 Activity 类。
 * - **Service**: 采用代理池模式，通过一个预设的代理 Service 池来支持多个插件 Service 并发运行。
 * - **BroadcastReceiver**: 采用解析注册模式。
 *
 * 该类的设计目标是提供一种灵活、可扩展的组件代理机制，
 * 以支持插件化架构中的组件通信和生命周期管理。
 */
class ProxyManager(
    private val context: Application,
) {
    /**
     * 存储已注册的、用于代理所有插件 Activity 的单一宿主 Activity 类。
     */
    private var hostActivityClass: Class<out BaseHostActivity>? = null

    /**
     * 存储当前可用的代理 Service。这是一个线程安全的队列，用于快速分配。
     */
    private val availableServiceProxies = ConcurrentLinkedQueue<Class<out BaseHostService>>()

    /**
     * 存储正在运行的插件 Service 与其占用的代理 Service 之间的映射关系。
     * Key: 插件 Service 的唯一实例标识符 (例如："com.example.MyService:task1")
     * Value: 正在代理它的 HostService 的 Class 对象。
     */
    private val activeServiceProxies = ConcurrentHashMap<String, Class<out BaseHostService>>()

    /**
     * 用于分发静态广播的注册表。
     * 直接存储完整的 StaticReceiverInfo，以便进行精确的 Intent 匹配。
     * 使用 CopyOnWriteArrayList 保证遍历时的线程安全。
     */
    private val staticReceiverRegistry = CopyOnWriteArrayList<Pair<String, StaticReceiverInfo>>()

    /**
     * 用于存储 ContentProvider 的注册表。
     * Key: 插件 Provider 的完整类名。
     * Value: 一个配对，包含 PluginId 和完整的 ProviderInfo 对象。
     */
    private val providerRegistry = ConcurrentHashMap<String, Pair<String, ProviderInfo>>()

    /**
     * 用于从 Authority 快速映射到 Provider 的类名。
     */
    private val authorityToProviderMap = ConcurrentHashMap<String, String>()

    /**
     * 设置 Activity 组件的代理宿主。
     *
     * @param hostActivity 用于代理所有插件 Activity 的宿主 Activity 的 Class 对象。
     */
    fun setHostActivity(hostActivity: Class<out BaseHostActivity>) {
        this.hostActivityClass = hostActivity
        Timber.i("Activity 代理宿主已配置: ${hostActivity.simpleName}")
    }

    /**
     * 设置 Service 组件的代理池。
     *
     * @param serviceProxyPool 一个包含多个预定义代理 Service 的列表。这些 Service 必须在 Manifest 中注册。
     */
    fun setServicePool(serviceProxyPool: List<Class<out BaseHostService>>) {
        // 清理旧状态，确保重入安全
        availableServiceProxies.clear()
        activeServiceProxies.clear()
        // 加载新的代理池
        availableServiceProxies.addAll(serviceProxyPool)
        Timber.i("Service 代理池已配置，共加载 ${serviceProxyPool.size} 个可用代理。")
    }

    /**
     * 获取已配置的 Activity 代理宿主。
     *
     * @return 宿主 Activity 的 Class 对象；如果尚未配置，则返回 null。
     */
    fun getHostActivity(): Class<out BaseHostActivity>? {
        if (hostActivityClass == null) {
            Timber.e("严重错误：尝试获取 Activity 宿主，但尚未配置！")
        }
        return hostActivityClass
    }

    /**
     * 为一个插件 Service 请求一个可用的代理 Service。
     * 这是启动或绑定插件 Service 的第一步。此方法是线程安全的。
     *
     * @param instanceIdentifier 插件 Service 的实例标识符。
     * @return 一个可用的代理 Service 的 Class 对象；如果池已耗尽，则返回 null。
     */
    fun acquireServiceProxy(instanceIdentifier: String): Class<out BaseHostService>? {
        synchronized(this.activeServiceProxies) {
            val existingProxy = activeServiceProxies[instanceIdentifier]
            if (existingProxy != null) {
                Timber.d("插件实例 [$instanceIdentifier] 已在运行，复用代理 [${existingProxy.simpleName}]")
                return existingProxy
            }

            val acquiredProxy = availableServiceProxies.poll()
            if (acquiredProxy != null) {
                activeServiceProxies[instanceIdentifier] = acquiredProxy
                Timber.i("为插件实例 [$instanceIdentifier] 分配了代理 [${acquiredProxy.simpleName}]。")
                return acquiredProxy
            } else {
                Timber.e("无法为插件实例 [$instanceIdentifier] 分配代理，代理池已耗尽！")
                return null
            }
        }
    }

    /**
     * 释放一个被插件 Service 占用的代理，使其返回到可用池中。
     * 此方法应该在代理 Service 的 onDestroy() 生命周期中被调用。此方法是线程安全的。
     *
     * @param instanceIdentifier 插件 Service 的实例标识符。
     */
    fun releaseServiceProxy(instanceIdentifier: String) {
        synchronized(this.activeServiceProxies) {
            val releasedProxy = activeServiceProxies.remove(instanceIdentifier)
            if (releasedProxy != null) {
                availableServiceProxies.add(releasedProxy)
                Timber.i("代理 [${releasedProxy.simpleName}] 已被插件实例 [$instanceIdentifier] 释放，返回池中。")
            }
        }
    }

    /**
     * 获取正在代理它的 HostService 的 Class 对象。
     * 主要用于 stopService 和 bindService 等需要目标 Intent 的操作。
     *
     * @param instanceIdentifier 插件 Service 的实例标识符。
     * @return 正在代理它的 HostService 的 Class 对象；如果该插件未运行，则返回 null。
     */
    fun getServiceProxyFor(instanceIdentifier: String): Class<out BaseHostService>? =
        activeServiceProxies[instanceIdentifier]

    /**
     * 根据 Service 的类名，获取其所有正在运行的实例标识符。
     *
     * 这是解决 UI 状态与服务实际状态同步问题的关键。
     * UI 可以在初始化时调用此方法，来恢复当前所有正在运行的服务实例的列表。
     *
     * @param serviceClassName 插件 Service 的完整类名。
     * @return 一个包含所有正在运行的实例标识符的列表 (e.g., ["com.example.MyService:task1", "com.example.MyService:task2"])
     */
    fun getRunningInstancesFor(serviceClassName: String): List<String> {
        return activeServiceProxies.keys.filter { instanceIdentifier ->
            instanceIdentifier.startsWith(serviceClassName)
        }
    }

    /**
     * 注册一个插件的所有静态广播
     * @param pluginId 插件的ID
     * @param receivers 该插件包含的静态广播列表
     */
    fun registerStaticReceivers(
        pluginId: String,
        receivers: List<StaticReceiverInfo>,
    ) {
        if (receivers.isEmpty()) return

        receivers.forEach { receiverInfo ->
            if (receiverInfo.enabled) {
                staticReceiverRegistry.add(Pair(pluginId, receiverInfo))
                Timber.d("注册静态广播: ${receiverInfo.className} (来自插件 $pluginId)")
            } else {
                Timber.d("跳过注册被禁用的静态广播: ${receiverInfo.className}")
            }
        }
    }

    /**
     * 卸载一个插件的所有静态广播
     * @param pluginId 要卸载的插件ID
     */
    fun unregisterStaticReceivers(pluginId: String) {
        val beforeCount = staticReceiverRegistry.size
        staticReceiverRegistry.removeAll { (pId, _) -> pId == pluginId }
        val afterCount = staticReceiverRegistry.size
        if (beforeCount > afterCount) {
            Timber.i("已注销插件 [$pluginId] 的 ${beforeCount - afterCount} 个静态广播。")
        }
    }

    /**
     * 根据完整的 Intent 查找所有匹配的插件接收器。
     * 这是广播分发的核心匹配逻辑。
     * @param intent 接收到的广播 Intent
     * @return 匹配的接收器信息列表
     */
    fun findReceiversForIntent(intent: Intent): List<Pair<String, StaticReceiverInfo>> {
        val matchedReceivers = mutableListOf<Pair<String, StaticReceiverInfo>>()
        Timber.d("查找静态广播接收器: ${intent.action}, staticReceiverRegistry:$staticReceiverRegistry")
        val action = intent.action ?: return emptyList()

        // 如果 intent 的 package 与宿主包名相同，我们视其为内部广播。
        val isInternalBroadcast = (context.packageName == intent.getPackage())

        for (pair in staticReceiverRegistry) {
            val receiverInfo = pair.second

            if (!receiverInfo.exported && !isInternalBroadcast) {
                continue
            }

            // 遍历该 Receiver 的所有 IntentFilter
            for (filter in receiverInfo.intentFilters) {
                val actionMatch = filter.actions.contains(action)
                if (!actionMatch) continue

                val categories = intent.categories
                val categoryMatch = categories == null || filter.categories.containsAll(categories)
                if (!categoryMatch) continue

                val scheme = intent.data?.scheme
                val schemeMatch =
                    scheme == null || filter.schemes.isEmpty() || filter.schemes.contains(scheme)
                if (!schemeMatch) continue

                // 如果所有条件都满足，则认为匹配成功
                matchedReceivers.add(pair)
                Timber.d(
                    "Intent action [$action] 匹配成功 -> ${receiverInfo.className} (exported=${receiverInfo.exported}, isInternal=$isInternalBroadcast)",
                )
                break
            }
        }
        return matchedReceivers
    }

    /**
     * 注册一个插件的所有 ContentProvider。
     * @param pluginId 插件的 ID。
     * @param providers 插件中包含的、完整的 ProviderInfo 列表。
     */
    fun registerProviders(
        pluginId: String,
        providers: List<ProviderInfo>,
    ) {
        if (providers.isEmpty()) return

        providers.forEach { provider ->
            if (provider.enabled) {
                // 存储一个包含 pluginId 和 ProviderInfo 的配对
                providerRegistry[provider.className] = Pair(pluginId, provider)
                provider.authorities.forEach { authority ->
                    authorityToProviderMap[authority] = provider.className
                    Timber.d("注册 Provider Authority: [$authority] -> ${provider.className}")
                }
            } else {
                Timber.d("跳过注册被禁用的 Provider: ${provider.className}")
            }
        }
    }

    /**
     * 注销一个插件的所有 ContentProvider。
     */
    fun unregisterProviders(pluginId: String) {
        val providersToRemove = providerRegistry.filter { it.value.first == pluginId }

        if (providersToRemove.isEmpty()) return

        providersToRemove.forEach { (className, pair) ->
            val info = pair.second
            providerRegistry.remove(className)
            info.authorities.forEach { authority ->
                authorityToProviderMap.remove(authority)
                Timber.d("注销 Provider Authority: [$authority]")
            }
        }
        Timber.i("已完成插件 [$pluginId] 的所有 Provider 的注销。")
    }

    /**
     * 根据插件 Provider 的类名查找其注册信息。
     * @return 返回纯粹的 ProviderInfo 对象，对调用者隐藏内部的 Pair 结构。
     */
    fun findProviderInfoByClassName(className: String): ProviderInfo? =
        providerRegistry[className]?.second
}
