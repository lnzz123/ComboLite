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

package com.combo.core.manager

import android.app.Application
import android.os.Build
import com.combo.core.installer.InstallerManager
import com.combo.core.installer.XmlManager
import com.combo.core.interfaces.IPluginEntryClass
import com.combo.core.loader.DependencyManager
import com.combo.core.loader.IPluginStateProvider
import com.combo.core.loader.PluginClassLoader
import com.combo.core.model.PluginContext
import com.combo.core.model.PluginInfo
import com.combo.core.proxy.ProxyManager
import com.combo.core.resources.PluginResourcesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 插件框架核心管理器 (PluginManager)
 *
 * 这是一个单例对象，作为整个插件框架的中枢，负责管理插件的全生命周期。
 * 它整合了安装、加载、运行、卸载和更新等所有核心功能，为上层业务提供统一、简洁的接口。
 *
 * 主要职责:
 * - **初始化**: 配置并启动所有底层服务（安装器、资源管理器、代理管理器等）。
 * - **插件加载**: 动态加载插件的 APK 文件，创建独立的 ClassLoader 和资源。
 * - **生命周期管理**: 控制插件的启动 (`launchPlugin`)、卸载 (`unloadPlugin`) 和批量加载 (`loadEnabledPlugins`)。
 * - **状态维护**: 通过 StateFlow 实时暴露插件的加载状态和初始化状态。
 * - **跨插件通信**: 提供 `getInterface` 方法，利用全局类索引实现高效的跨插件接口调用。
 * - **依赖管理**: 借助 `DependencyManager` 解析和管理插件间的依赖关系，实现安全的链式重启和卸载。
 */
object PluginManager : IPluginStateProvider {

    private const val TAG = "PluginManager"
    private const val CLASS_INDEX_TAG = "ClassIndex"

    /**
     * 插件管理器的初始化状态。
     */
    enum class InitState { NOT_INITIALIZED, INITIALIZING, INITIALIZED }

    private val _initState = MutableStateFlow(InitState.NOT_INITIALIZED)

    /**
     * 可供外部观察的初始化状态 Flow。
     */
    val initStateFlow: StateFlow<InitState> = _initState.asStateFlow()

    /**
     * 判断插件管理器是否已完成初始化。
     */
    val isInitialized: Boolean get() = _initState.value == InitState.INITIALIZED

    /**
     * 包含已加载插件的详细运行时信息。
     * @property pluginInfo 插件的静态描述信息。
     * @property classLoader 插件专属的类加载器。
     */
    data class LoadedPluginInfo(
        val pluginInfo: PluginInfo,
        val classLoader: PluginClassLoader,
    )

    private val _loadedPlugins = MutableStateFlow<Map<String, LoadedPluginInfo>>(emptyMap())

    /**
     * 可供外部观察的已加载插件信息 Flow (Key: pluginId, Value: LoadedPluginInfo)。
     */
    val loadedPluginsFlow: StateFlow<Map<String, LoadedPluginInfo>> = _loadedPlugins.asStateFlow()

    private val _pluginInstances = MutableStateFlow<Map<String, IPluginEntryClass>>(emptyMap())

    /**
     * 可供外部观察的已实例化插件入口类 Flow (Key: pluginId, Value: IPluginEntryClass instance)。
     */
    val pluginInstancesFlow: StateFlow<Map<String, IPluginEntryClass>> =
        _pluginInstances.asStateFlow()

    /** 插件安装管理器 */
    lateinit var installerManager: InstallerManager
        private set

    /** 插件资源管理器 */
    lateinit var resourcesManager: PluginResourcesManager
        private set

    /** 安卓四大组件代理管理器 */
    lateinit var proxyManager: ProxyManager
        private set

    private lateinit var context: Application
    private lateinit var xmlManager: XmlManager
    private lateinit var dependencyManager: DependencyManager

    /**
     * PluginManager 专用的协程作用域，用于执行后台 IO 密集型任务。
     */
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 全局类索引，用于 O(1) 复杂度的跨插件类查找。
     * Key: 完整类名 (e.g., "com.example.MyClass")
     * Value: 插件ID (e.g., "plugin-a")
     */
    private val classIndex = ConcurrentHashMap<String, String>()

    /**
     * 初始化插件管理器。
     * 这是一个同步方法，用于设置好所有必要的子管理器。
     * 真正的插件加载逻辑在 `pluginLoader` lambda 中异步执行。
     *
     * @param context 应用程序上下文。
     * @param pluginLoader 一个可选的 suspend lambda，在核心组件初始化后异步执行，通常用于加载插件。
     */
    fun initialize(
        context: Application,
        pluginLoader: (suspend () -> Unit)? = null
    ) {
        if (_initState.value != InitState.NOT_INITIALIZED) {
            Timber.tag(TAG).w("PluginManager 正在初始化或已完成，跳过重复操作。")
            return
        }

        synchronized(this) {
            if (_initState.value != InitState.NOT_INITIALIZED) return
            _initState.value = InitState.INITIALIZING
        }

        try {
            Timber.tag(TAG).i("开始初始化 PluginManager 核心组件...")
            startKoin { androidContext(context) }
            this.context = context
            this.xmlManager = XmlManager(this.context)
            this.installerManager = InstallerManager(this.context, xmlManager)
            this.resourcesManager = PluginResourcesManager(this.context)
            this.proxyManager = ProxyManager(this.context)
            this.dependencyManager = DependencyManager(this)
            Timber.tag(TAG).i("核心组件初始化完成。")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "PluginManager 初始化失败: ${e.message}")
            _initState.value = InitState.NOT_INITIALIZED
            throw e
        }

        managerScope.launch {
            try {
                pluginLoader?.invoke()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "插件加载代码块执行失败。")
            } finally {
                _initState.value = InitState.INITIALIZED
                Timber.tag(TAG).i("PluginManager 已就绪。")
            }
        }
    }

    /**
     * 等待插件管理器初始化完成。
     * 如果初始化已完成，此方法会立即返回。
     */
    suspend fun awaitInitialization() {
        if (isInitialized) return
        _initState.first { it == InitState.INITIALIZED }
    }

    /**
     * 启动或重启指定插件。
     * - **首次启动**: 如果插件未加载，则执行完整的加载、实例化和启动流程。
     * - **链式重启**: 如果插件已加载，则执行安全重启。它会先卸载该插件及其所有【依赖方】，然后重新启动它们。
     *
     * @param pluginId 要启动的插件ID。
     * @return 操作是否成功。
     */
    suspend fun launchPlugin(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            when {
                isPluginLoaded(pluginId) -> {
                    Timber.tag(TAG).i("插件 [$pluginId] 已加载，执行链式重启...")
                    reloadPluginWithDependents(pluginId)
                }

                else -> {
                    Timber.tag(TAG).i("插件 [$pluginId] 未加载，执行首次启动...")
                    launchSinglePlugin(pluginId)
                }
            }
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "启动或重启插件 [$pluginId] 期间发生严重错误。")
            if (isPluginLoaded(pluginId)) {
                unloadPlugin(pluginId)
            }
            false
        }
    }

    /**
     * 卸载指定插件，并清理其所有相关资源和依赖记录。
     *
     * @param pluginId 要卸载的插件ID。
     */
    suspend fun unloadPlugin(pluginId: String) = withContext(Dispatchers.IO) {
        if (!isPluginLoaded(pluginId)) {
            Timber.tag(TAG).w("尝试卸载一个未加载的插件: $pluginId")
            return@withContext
        }

        Timber.tag(TAG).i("开始卸载插件: $pluginId")

        _pluginInstances.value[pluginId]?.let { instance ->
            executeOnUnload(pluginId, instance)
            unloadKoinModules(pluginId, instance)
        }

        // 从状态中原子地移除实例和加载信息
        _loadedPlugins.update { it - pluginId }
        _pluginInstances.update { it - pluginId }

        // 清理其他资源和记录
        proxyManager.unregisterStaticReceivers(pluginId)
        proxyManager.unregisterProviders(pluginId)
        dependencyManager.clearDependenciesFor(pluginId)
        resourcesManager.removePluginResources(pluginId)
        removePluginFromIndex(pluginId)

        Timber.tag(TAG).i("插件 [$pluginId] 卸载完成。")
    }


    /**
     * 加载所有在 `plugins.xml` 中被标记为启用的插件。
     * 该方法会跳过已经加载的插件。
     *
     * @return 成功加载的插件数量。
     */
    suspend fun loadEnabledPlugins(): Int = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("开始异步初始化所有已启用的插件。")
        val enabledPlugins = getEnabledPlugins().filter { !isPluginLoaded(it.pluginId) }

        if (enabledPlugins.isEmpty()) {
            Timber.tag(TAG).i("没有新的已启用插件需要加载。")
            return@withContext 0
        }

        Timber.tag(TAG).i("找到 ${enabledPlugins.size} 个新的已启用插件，开始加载...")
        if (loadAndInstantiatePlugins(enabledPlugins)) {
            enabledPlugins.size
        } else {
            0
        }
    }

    /**
     * 从所有已加载的插件中，获取指定接口的实现实例。
     * 这是一个便捷的顶层API，它利用全局类索引自动查找并加载类，无需调用者关心该类具体属于哪个插件。
     *
     * @param T 接口类型泛型。
     * @param interfaceClass 接口的 Class 对象。
     * @param className 实现该接口的完整类名。
     * @return 接口实现的实例；如果类未找到、插件未加载或实例化失败，则返回 null。
     */
    fun <T : Any> getInterface(interfaceClass: Class<T>, className: String): T? {
        try {
            val targetPluginId = classIndex[className]
            if (targetPluginId == null) {
                // 如果在插件索引中找不到，尝试从宿主ClassLoader加载
                getInterfaceFromHost(interfaceClass, className)?.let { return it }
                Timber.tag(TAG).w("无法找到类 '$className' 的宿主插件，类索引中不存在该条目。")
                return null
            }

            val loadedPlugin = _loadedPlugins.value[targetPluginId]
            if (loadedPlugin == null) {
                Timber.tag(TAG)
                    .e("类索引不一致：类 '$className' 指向插件 '$targetPluginId'，但该插件当前未加载。")
                return null
            }

            Timber.tag(TAG)
                .d("正在从插件 '$targetPluginId' 中获取接口 '${interfaceClass.simpleName}' 的实现 '$className'...")
            return loadedPlugin.classLoader.getInterface(interfaceClass, className)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "通过 PluginManager 获取接口 '$className' 的实例时发生未知错误。")
            return null
        }
    }


    /**
     * 获取插件的入口类实例。
     *
     * @param pluginId 插件ID。
     * @return 插件实例；如果未找到则返回 null。
     */
    fun getPluginInstance(pluginId: String): IPluginEntryClass? = _pluginInstances.value[pluginId]

    /**
     * 获取已加载插件的运行时信息。
     *
     * @param pluginId 插件ID。
     * @return 插件的 `LoadedPluginInfo`；如果未加载则返回 null。
     */
    fun getPluginInfo(pluginId: String): LoadedPluginInfo? = _loadedPlugins.value[pluginId]

    /**
     * 检查插件是否已加载。
     *
     * @param pluginId 插件ID。
     * @return 如果插件已加载并注册，返回 true。
     */
    fun isPluginLoaded(pluginId: String): Boolean = _loadedPlugins.value.containsKey(pluginId)

    /**
     * 获取所有已加载插件的ID列表。
     *
     * @return 包含所有插件ID的列表。
     */
    fun getAllPluginIds(): List<String> = _loadedPlugins.value.keys.toList()

    /**
     * 获取所有已实例化的插件入口类。
     *
     * @return 一个 Map，Key 为插件ID，Value 为插件实例。
     */
    fun getAllPluginInstances(): Map<String, IPluginEntryClass> = _pluginInstances.value

    /**
     * 获取所有已安装的插件信息（无论是否启用或加载）。
     *
     * @return 包含所有 `PluginInfo` 的列表。
     */
    fun getAllInstallPlugins(): List<PluginInfo> = xmlManager.getAllPlugins()

    /**
     * 设置插件是否自动启动（即 enabled 状态）。
     * 此操作会修改 `plugins.xml` 配置文件。
     *
     * @param pluginId 插件ID。
     * @param enabled 是否启用。
     * @return 设置是否成功。
     */
    fun setPluginEnabled(pluginId: String, enabled: Boolean): Boolean {
        return try {
            Timber.tag(TAG).i("设置插件 '$pluginId' 的启用状态为: $enabled")
            val pluginInfo = xmlManager.getPluginById(pluginId) ?: run {
                Timber.tag(TAG).e("插件不存在: $pluginId")
                return false
            }

            if (pluginInfo.enabled == enabled) {
                Timber.tag(TAG).d("插件 '$pluginId' 的状态无需更改。")
                return true
            }

            val updatedPluginInfo = pluginInfo.copy(enabled = enabled)
            xmlManager.updatePlugin(updatedPluginInfo)
            xmlManager.flushToDisk()
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "设置插件 '$pluginId' 状态时出错。")
            false
        }
    }

    /**
     * 获取指定插件的完整【依赖方】链（查找“谁依赖我”）。
     *
     * 此方法回答：“如果我改变或卸载 `pluginId`，会影响到哪些上游插件？”
     *
     * @sample
     * // 依赖关系: C -> B -> A (C依赖B, B依赖A)
     * // getPluginDependentsChain("A") 返回 ["B", "C"]
     *
     * @param pluginId 要查询的插件ID（被依赖的插件）。
     * @return 一个包含所有依赖此插件（即依赖方）的ID列表。
     */
    fun getPluginDependentsChain(pluginId: String): List<String> {
        return dependencyManager.findDependentsRecursive(pluginId)
    }

    /**
     * 获取指定插件的完整【依赖项】链（查找“我依赖谁”）。
     *
     * 此方法回答：“要让 `pluginId` 正常运行，它需要哪些下游插件？”
     *
     * @sample
     * // 依赖关系: A -> B -> C (A依赖B, B依赖C)
     * // getPluginDependenciesChain("A") 返回 ["B", "C"]
     *
     * @param pluginId 要查询的插件ID（发起依赖的插件）。
     * @return 一个包含此插件所有依赖项的ID列表。
     */
    fun getPluginDependenciesChain(pluginId: String): List<String> {
        return dependencyManager.findDependenciesRecursive(pluginId)
    }

    override fun getClassIndex(): Map<String, String> = this.classIndex
    override fun getLoadedPlugins(): Map<String, LoadedPluginInfo> = this._loadedPlugins.value

    /**
     * 执行单个插件的首次加载和启动流程。
     */
    private suspend fun launchSinglePlugin(pluginId: String): Boolean {
        val pluginInfo = xmlManager.getPluginById(pluginId) ?: run {
            Timber.tag(TAG).e("插件信息未找到: $pluginId")
            return false
        }

        val loadedPlugin = loadPlugin(pluginInfo) ?: run {
            Timber.tag(TAG).e("插件加载失败: $pluginId")
            return false
        }
        _loadedPlugins.update { it + (pluginId to loadedPlugin) }

        val instance = instantiatePlugin(loadedPlugin) ?: run {
            Timber.tag(TAG).e("插件实例化失败: $pluginId，执行回滚...")
            unloadPlugin(pluginId)
            return false
        }
        _pluginInstances.update { it + (pluginId to instance) }

        Timber.tag(TAG).i("插件 [$pluginId] 首次启动成功。")
        return true
    }

    /**
     * 执行插件及其所有依赖方的链式重启。
     */
    private suspend fun reloadPluginWithDependents(pluginId: String): Boolean {
        val dependents = dependencyManager.findDependentsRecursive(pluginId)
        val pluginsToReloadIds = listOf(pluginId) + dependents
        Timber.tag(TAG).i("链式重启计划：将重启以下插件: $pluginsToReloadIds")

        // 按依赖反向顺序卸载所有受影响的插件
        pluginsToReloadIds.reversed().forEach { id ->
            if (isPluginLoaded(id)) unloadPlugin(id)
        }
        Timber.tag(TAG).i("所有相关插件已卸载，准备重新加载...")

        val pluginInfosToReload = pluginsToReloadIds.mapNotNull { xmlManager.getPluginById(it) }
        if (pluginInfosToReload.size != pluginsToReloadIds.size) {
            Timber.tag(TAG).e("无法获取部分要重启的插件信息，操作中止。")
            return false
        }

        // 使用通用逻辑函数重新加载并实例化它们
        return loadAndInstantiatePlugins(pluginInfosToReload)
    }

    /**
     * 批量加载并实例化一组插件。
     * 这是一个原子操作，如果任何一个插件失败，所有插件都将被回滚到卸载状态。
     *
     * @param pluginsToLoad 需要加载的插件信息列表。
     * @return 所有插件是否都成功加载并实例化。
     */
    private suspend fun loadAndInstantiatePlugins(pluginsToLoad: List<PluginInfo>): Boolean =
        coroutineScope {
            if (pluginsToLoad.isEmpty()) return@coroutineScope true
            var loadedPluginsList: List<LoadedPluginInfo>? = null

            try {
                // 1. 并发加载（ClassLoader, Resources, Indexing）
                val loadJobs = pluginsToLoad.map { async(Dispatchers.IO) { loadPlugin(it) } }
                loadedPluginsList = loadJobs.awaitAll().filterNotNull()
                if (loadedPluginsList.size != pluginsToLoad.size) {
                    Timber.tag(TAG).w("部分插件加载失败，操作中止。")
                    loadedPluginsList.forEach { unloadPlugin(it.pluginInfo.pluginId) } // 回滚已加载的
                    return@coroutineScope false
                }

                // 2. 统一注册加载信息
                _loadedPlugins.update { it + loadedPluginsList.associateBy { p -> p.pluginInfo.pluginId } }
                Timber.tag(TAG).d("成功注册 ${loadedPluginsList.size} 个插件的加载信息。")

                // 3. 并发实例化入口类
                val instantiateJobs = loadedPluginsList.map { loadedPlugin ->
                    async(Dispatchers.Default) {
                        instantiatePlugin(loadedPlugin)?.let { it1 -> loadedPlugin.pluginInfo.pluginId to it1 }
                    }
                }
                val successfulInstances = instantiateJobs.awaitAll().filterNotNull().toMap()
                if (successfulInstances.size != pluginsToLoad.size) {
                    Timber.tag(TAG).e("部分插件实例化失败，执行回滚...")
                    loadedPluginsList.forEach { unloadPlugin(it.pluginInfo.pluginId) } // 完全回滚
                    return@coroutineScope false
                }

                // 4. 统一注册实例
                _pluginInstances.update { it + successfulInstances }
                Timber.tag(TAG).i("批量操作成功，共加载并实例化 ${successfulInstances.size} 个插件。")
                true
            } catch (e: Throwable) {
                Timber.tag(TAG).e(e, "批量加载插件时发生严重错误，执行回滚...")
                loadedPluginsList?.forEach { unloadPlugin(it.pluginInfo.pluginId) }
                false
            }
        }

    /**
     * 加载单个插件，创建 ClassLoader、加载资源并建立类索引。
     */
    private suspend fun loadPlugin(plugin: PluginInfo): LoadedPluginInfo? =
        withContext(Dispatchers.IO) {
            try {
                val pluginApkFile = File(plugin.path)
                if (!pluginApkFile.exists()) {
                    Timber.tag(TAG).e("插件 APK 文件不存在: ${plugin.path}")
                    return@withContext null
                }

                indexPluginClasses(plugin.pluginId, pluginApkFile)
                proxyManager.registerStaticReceivers(
                    plugin.pluginId,
                    plugin.staticReceivers.filter { it.enabled })
                proxyManager.registerProviders(
                    plugin.pluginId,
                    plugin.providers.filter { it.enabled })

                val pluginInstallDir = installerManager.getPluginDirectory(plugin.pluginId)
                val abi = Build.SUPPORTED_ABIS[0]
                val nativeLibDir =
                    File(pluginInstallDir, "${InstallerManager.NATIVE_LIBS_DIR_NAME}/$abi")
                val nativeLibraryPath =
                    if (nativeLibDir.exists()) nativeLibDir.absolutePath else null
                val optimizedDirectory =
                    installerManager.getOptimizedDirectory(plugin.pluginId)?.absolutePath

                val classLoader = PluginClassLoader(
                    pluginId = plugin.pluginId,
                    pluginFile = pluginApkFile,
                    parent = context.classLoader,
                    optimizedDirectory = optimizedDirectory,
                    librarySearchPath = nativeLibraryPath,
                    pluginFinder = this@PluginManager.dependencyManager,
                )

                resourcesManager.loadPluginResources(plugin.pluginId, pluginApkFile)
                LoadedPluginInfo(pluginInfo = plugin, classLoader = classLoader)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "加载插件 ${plugin.pluginId} 失败: ${e.message}")
                null
            }
        }

    /**
     * 实例化单个插件的入口类，并执行其 onLoad 和 Koin 模块加载。
     */
    private fun instantiatePlugin(loadedPlugin: LoadedPluginInfo): IPluginEntryClass? {
        val plugin = loadedPlugin.pluginInfo
        return try {
            val instance = loadedPlugin.classLoader.getInterface(
                IPluginEntryClass::class.java,
                plugin.entryClass
            )
            if (instance != null) {
                Timber.tag(TAG)
                    .d("插件入口类实例化成功: ${plugin.pluginId} -> ${plugin.entryClass}")
                loadKoinModules(plugin.pluginId, instance)
                executeOnLoad(plugin, instance)
                instance
            } else {
                Timber.tag(TAG)
                    .e("插件入口类实例化失败: ${plugin.pluginId} -> ${plugin.entryClass}")
                null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "实例化插件 ${plugin.pluginId} 入口类失败: ${e.message}")
            null
        }
    }

    /**
     * 从 `plugins.xml` 中获取所有被标记为启用的插件信息。
     */
    private fun getEnabledPlugins(): List<PluginInfo> {
        return try {
            xmlManager.getAllPlugins().filter { it.enabled }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "读取已启用插件信息失败。")
            emptyList()
        }
    }

    private fun executeOnLoad(plugin: PluginInfo, instance: IPluginEntryClass) {
        try {
            val pluginContext = PluginContext(application = this.context, pluginInfo = plugin)
            instance.onLoad(pluginContext)
            Timber.tag(TAG).d("插件 [${plugin.pluginId}] onLoad() 执行成功。")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "插件 [${plugin.pluginId}] onLoad() 执行失败。")
        }
    }

    private fun executeOnUnload(pluginId: String, instance: IPluginEntryClass) {
        try {
            instance.onUnload()
            Timber.tag(TAG).d("插件 [$pluginId] onUnload() 执行成功。")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "插件 [$pluginId] onUnload() 执行失败。")
        }
    }

    private fun loadKoinModules(pluginId: String, instance: IPluginEntryClass) {
        try {
            val modules = instance.pluginModule
            if (modules.isNotEmpty()) {
                GlobalContext.get().loadModules(modules)
                Timber.tag(TAG).d("插件 [$pluginId] 的 ${modules.size} 个 Koin 模块加载成功。")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "加载插件 [$pluginId] 的 Koin 模块失败。")
        }
    }

    private fun unloadKoinModules(pluginId: String, instance: IPluginEntryClass) {
        try {
            val modules = instance.pluginModule
            if (modules.isNotEmpty()) {
                GlobalContext.get().unloadModules(modules)
                Timber.tag(TAG).d("插件 [$pluginId] 的 ${modules.size} 个 Koin 模块卸载成功。")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "卸载插件 [$pluginId] 的 Koin 模块失败。")
        }
    }

    private fun indexPluginClasses(pluginId: String, pluginFile: File) {
        var indexedCount = 0
        try {
            DexFileFactory.loadDexFile(pluginFile, Opcodes.forApi(Build.VERSION.SDK_INT))
                .classes.forEach { classDef ->
                    val className = convertDexTypeToClassName(classDef.type)
                    if (classIndex.putIfAbsent(className, pluginId) == null) {
                        indexedCount++
                    }
                }
            Timber.tag(CLASS_INDEX_TAG).d("为插件 [$pluginId] 建立 $indexedCount 个类索引。")
        } catch (e: Exception) {
            Timber.tag(CLASS_INDEX_TAG).e(e, "为插件 [$pluginId] 建立类索引失败。")
        }
    }

    private fun removePluginFromIndex(pluginId: String) {
        var removedCount = 0
        val iterator = classIndex.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value == pluginId) {
                iterator.remove()
                removedCount++
            }
        }
        Timber.tag(CLASS_INDEX_TAG).d("从索引中移除了插件 [$pluginId] 的 $removedCount 个类。")
    }


    private fun convertDexTypeToClassName(dexType: String): String {
        return if (dexType.startsWith("L") && dexType.endsWith(";")) {
            dexType.substring(1, dexType.length - 1).replace('/', '.')
        } else {
            dexType.replace('/', '.')
        }
    }

    private fun <T : Any> getInterfaceFromHost(interfaceClass: Class<T>, className: String): T? {
        return try {
            val clazz = context.classLoader.loadClass(className)
            val instance = clazz.getDeclaredConstructor().newInstance()
            if (interfaceClass.isInstance(instance)) {
                @Suppress("UNCHECKED_CAST")
                instance as T
            } else {
                Timber.tag(TAG)
                    .e("类型不匹配：宿主类 '$className' 未实现接口 '${interfaceClass.simpleName}'")
                null
            }
        } catch (_: ClassNotFoundException) {
            null
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "从宿主实例化 '$className' 时发生错误。")
            null
        }
    }
}