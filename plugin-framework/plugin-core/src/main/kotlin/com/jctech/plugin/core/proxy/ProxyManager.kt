package com.jctech.plugin.core.proxy

import com.jctech.plugin.core.base.BaseHostActivity
import com.jctech.plugin.core.base.BaseHostService
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 插件四大组件代理管理器
 *
 * 负责统一管理和调度 Activity、Service 等组件的代理资源。
 * 它被设计为一个可配置的管理器，通过独立的 set 方法来注册不同组件的代理。
 * - **Activity**: 采用单一宿主模式，整个应用共享一个代理 Activity 类。
 * - **Service**: 采用代理池模式，通过一个预设的代理 Service 池来支持多个插件 Service 并发运行。
 */
class ProxyManager {
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
     * Key: 插件 Service 的完整类名。
     * Value: 正在代理它的 HostService 的 Class 对象。
     */
    private val activeServiceProxies = ConcurrentHashMap<String, Class<out BaseHostService>>()

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
     * @param pluginServiceClassName 需要启动的插件 Service 的完整类名。
     * @return 一个可用的代理 Service 的 Class 对象；如果池已耗尽，则返回 null。
     */
    fun acquireServiceProxy(pluginServiceClassName: String): Class<out BaseHostService>? {
        synchronized(this.activeServiceProxies) {
            val existingProxy = activeServiceProxies[pluginServiceClassName]
            if (existingProxy != null) {
                Timber.d("插件 [$pluginServiceClassName] 已在运行，复用代理 [${existingProxy.simpleName}]")
                return existingProxy
            }

            val acquiredProxy = availableServiceProxies.poll()
            if (acquiredProxy != null) {
                activeServiceProxies[pluginServiceClassName] = acquiredProxy
                Timber.i("为插件 [$pluginServiceClassName] 分配了代理 [${acquiredProxy.simpleName}]。")
                return acquiredProxy
            } else {
                Timber.e("无法为插件 [$pluginServiceClassName] 分配代理，代理池已耗尽！")
                return null
            }
        }
    }

    /**
     * 释放一个被插件 Service 占用的代理，使其返回到可用池中。
     * 此方法应该在代理 Service 的 onDestroy() 生命周期中被调用。此方法是线程安全的。
     *
     * @param pluginServiceClassName 之前占用代理的插件 Service 的完整类名。
     */
    fun releaseServiceProxy(pluginServiceClassName: String) {
        synchronized(this.activeServiceProxies) {
            val releasedProxy = activeServiceProxies.remove(pluginServiceClassName)
            if (releasedProxy != null) {
                availableServiceProxies.add(releasedProxy)
                Timber.i("代理 [${releasedProxy.simpleName}] 已被插件 [$pluginServiceClassName] 释放，返回池中。")
            }
        }
    }

    /**
     * 根据插件 Service 的类名，查询其当前正在使用的代理 Service。
     * 主要用于 stopService 和 bindService 等需要目标 Intent 的操作。
     *
     * @param pluginServiceClassName 插件 Service 的完整类名。
     * @return 正在代理它的 HostService 的 Class 对象；如果该插件未运行，则返回 null。
     */
    fun getServiceProxyFor(pluginServiceClassName: String): Class<out BaseHostService>? {
        return activeServiceProxies[pluginServiceClassName]
    }
}