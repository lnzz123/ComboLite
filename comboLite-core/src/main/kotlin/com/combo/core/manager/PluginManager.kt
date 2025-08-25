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

@file:Suppress("unused")

package com.combo.core.manager

import android.app.Application
import android.content.Context
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
 * 插件管理器
 *
 * 负责插件的安装、运行和更新管理
 * 支持插件的动态加载、资源管理和依赖注入
 * 整合了原PluginLauncher的功能，提供统一的插件生命周期管理
 */
object PluginManager : IPluginStateProvider {

    private const val TAG = "PluginManager"
    private const val CLASS_INDEX_TAG = "ClassIndex"
    private const val DEX_OPTIMIZED_DIR_NAME = "dex_opt"
    private const val PLUGIN_FILE_EXTENSION = ".apk"

    private lateinit var context: Application
    private lateinit var xmlManager: XmlManager
    private lateinit var dependencyManager: DependencyManager

    // 子管理器实例，对外部只读
    lateinit var installerManager: InstallerManager
        private set
    lateinit var resourcesManager: PluginResourcesManager
        private set
    lateinit var proxyManager: ProxyManager
        private set

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 插件初始化状态 */
    enum class InitState { NOT_INITIALIZED, INITIALIZING, INITIALIZED }
    private val _initState = MutableStateFlow(InitState.NOT_INITIALIZED)
    val initStateFlow: StateFlow<InitState> = _initState.asStateFlow()
    val isInitialized: Boolean get() = _initState.value == InitState.INITIALIZED

    /** 已加载插件的运行时信息 */
    data class LoadedPluginInfo(
        val pluginInfo: PluginInfo,
        val classLoader: PluginClassLoader,
    )
    private val _loadedPlugins = MutableStateFlow<Map<String, LoadedPluginInfo>>(emptyMap())
    val loadedPluginsFlow: StateFlow<Map<String, LoadedPluginInfo>> = _loadedPlugins.asStateFlow()

    /** 已加载并实例化的插件入口类实例 */
    private val _pluginInstances = MutableStateFlow<Map<String, IPluginEntryClass>>(emptyMap())
    val pluginInstancesFlow: StateFlow<Map<String, IPluginEntryClass>> = _pluginInstances.asStateFlow()

    /** 全局类索引，用于 O(1) 复杂度的跨插件类查找 */
    private val classIndex = ConcurrentHashMap<String, String>()

    override fun getClassIndex(): Map<String, String> = this.classIndex
    override fun getLoadedPlugins(): Map<String, LoadedPluginInfo> = this._loadedPlugins.value

    /**
     * 等待插件管理器初始化完成
     */
    suspend fun awaitInitialization() {
        if (_initState.value == InitState.INITIALIZED) {
            return
        }
        _initState.first { it == InitState.INITIALIZED }
    }

    /**
     * 初始化插件管理器
     *
     * @param context 应用上下文
     * @param
     */
    fun initialize(
        context: Application,
        pluginLoader: (suspend () -> Unit)? = null
    ) {
        if (_initState.value != InitState.NOT_INITIALIZED) {
            Timber.tag(TAG).w("PluginManager 正在初始化或已完成，跳过重复操作")
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
            clearDexOptCache()

            Timber.tag(TAG).i("核心组件初始化完成。")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "PluginManager 初始化失败: ${e.message}")
            _initState.value = InitState.NOT_INITIALIZED
            throw e
        }

        managerScope.launch {
            try {
                if (pluginLoader != null) {
                    Timber.tag(TAG).i("开始执行加载任务...")
                    pluginLoader.invoke()
                    Timber.tag(TAG).i("插件加载任务执行完毕。")
                } else {
                    Timber.tag(TAG).i("未提供加载任务，初始化完成。")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "插件加载代码块执行失败")
            } finally {
                _initState.value = InitState.INITIALIZED
                Timber.tag(TAG).i("PluginManager 已就绪。")
            }
        }
    }

    /**
     * 异步加载所有已启用的插件
     *
     * 该方法会按顺序执行以下操作：
     * 1. 从 xmlManager 读取所有已启用的插件信息
     * 2. 并发加载所有插件的 dex 和资源文件
     * 3. 并发实例化所有插件的入口类对象
     * 4. 原子地更新插件和实例的缓存
     * 5. 加载所有插件的 Koin 模块
     *
     * @return 成功加载的插件数量
     */
    suspend fun loadEnabledPlugins(): Int {
        var loadedPluginsList: List<LoadedPluginInfo>? = null

        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).i("开始异步初始化所有已启用的插件")

                val enabledPlugins = getEnabledPlugins().filter { !isPluginLoaded(it.pluginId) }
                if (enabledPlugins.isEmpty()) {
                    return@withContext 0
                }
                Timber.tag(TAG).i("找到 ${enabledPlugins.size} 个已启用的插件，开始加载")

                // 加载阶段
                val loadJobs = enabledPlugins.map { plugin -> async { loadPlugin(plugin) } }
                loadedPluginsList = loadJobs.awaitAll().filterNotNull()

                if (loadedPluginsList.isEmpty()) {
                    Timber.tag(TAG).w("所有插件均加载失败")
                    return@withContext 0
                }

                // 注册阶段
                val successfulLoadedInfo = loadedPluginsList.associateBy { it.pluginInfo.pluginId }
                _loadedPlugins.update { it + successfulLoadedInfo }
                Timber.tag(TAG)
                    .i("插件加载阶段完成，成功注册 ${loadedPluginsList.size} 个插件到 loadedPlugins。")

                // 实例化阶段
                val instantiateJobs = loadedPluginsList.map { loadedPlugin ->
                    async(Dispatchers.Default) {
                        instantiatePlugin(loadedPlugin)?.let { instance ->
                            loadedPlugin.pluginInfo.pluginId to instance
                        }
                    }
                }
                val successfulInstances = instantiateJobs.awaitAll().filterNotNull().toMap()

                if (successfulInstances.isEmpty()) {
                    Timber.tag(TAG).e("所有已加载的插件均实例化失败，执行回滚...")
                    loadedPluginsList.forEach { unloadPlugin(it.pluginInfo.pluginId) }
                    return@withContext 0
                }

                _pluginInstances.update { it + successfulInstances }

                val successCount = successfulInstances.size
                Timber.tag(TAG).i("插件异步实例化完成，成功实例化 $successCount 个插件")
                successCount
            } catch (e: Throwable) {
                Timber.tag(TAG).e(e, "批量加载插件时发生严重错误，执行回滚...")

                loadedPluginsList?.let {
                    Timber.tag(TAG).d("开始回滚 ${it.size} 个已部分加载的插件...")
                    it.forEach { loadedInfo ->
                        unloadPlugin(loadedInfo.pluginInfo.pluginId)
                    }
                    Timber.tag(TAG).d("回滚操作完成。")
                }
                0
            }
        }
    }

    /**
     * 异步启动或重启指定插件。
     * - 如果插件未加载，则执行首次启动。
     * - 如果插件已加载，则执行【链式重启】：卸载该插件及其所有依赖方，然后重新启动它们。
     *
     * @param pluginId 插件ID
     * @return 操作是否成功
     */
    suspend fun launchPlugin(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isPluginLoaded(pluginId)) {
                Timber.tag(TAG).i("插件 [$pluginId] 已加载，执行链式重启...")
                reloadPluginWithDependents(pluginId)
            } else {
                Timber.tag(TAG).i("插件 [$pluginId] 未加载，执行首次启动...")
                launchSinglePlugin(pluginId)
            }
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "启动或重启插件 [$pluginId] 期间发生严重错误")
            // 尝试回滚，确保插件处于卸载状态
            if (isPluginLoaded(pluginId)) {
                unloadPlugin(pluginId)
            }
            false
        }
    }

    /**
     * 【私有】执行单个插件的首次加载和启动流程。
     *
     * @param pluginId 插件ID
     * @return 是否启动成功
     */
    private suspend fun launchSinglePlugin(pluginId: String): Boolean {
        val pluginInfo = xmlManager.getPluginById(pluginId)
        if (pluginInfo == null) {
            Timber.tag(TAG).e("插件信息未找到: $pluginId")
            return false
        }

        // 1. 加载插件（创建ClassLoader、索引类、加载资源等）
        val loadedPlugin = loadPlugin(pluginInfo) ?: run {
            Timber.tag(TAG).e("插件加载失败: $pluginId")
            return false
        }

        // 2. 注册已加载的插件信息
        _loadedPlugins.update { it + (pluginId to loadedPlugin) }
        Timber.tag(TAG).d("插件 [$pluginId] 已完成加载并注册，准备实例化。")

        // 3. 实例化入口类
        val instance = instantiatePlugin(loadedPlugin) ?: run {
            Timber.tag(TAG).e("插件实例化失败: $pluginId，执行回滚...")
            unloadPlugin(pluginId)
            return false
        }

        // 4. 注册插件实例
        _pluginInstances.update { it + (pluginId to instance) }

        Timber.tag(TAG).i("插件 [$pluginId] 首次启动成功。")
        return true
    }

    /**
     * 【私有】执行插件及其所有依赖方的链式重启。
     *
     * @param pluginId 被主动重启的插件ID
     * @return 是否重启成功
     */
    private suspend fun reloadPluginWithDependents(pluginId: String): Boolean {
        // 1. 确定需要重启的所有插件
        val dependents = dependencyManager.findDependentsRecursive(pluginId)
        val pluginsToReload = listOf(pluginId) + dependents
        Timber.tag(TAG).i("链式重启计划：将重启以下插件: $pluginsToReload")

        // 2. 按顺序卸载所有受影响的插件
        pluginsToReload.reversed().forEach { id ->
            if (isPluginLoaded(id)) {
                unloadPlugin(id)
            }
        }
        Timber.tag(TAG).i("所有相关插件已卸载，准备重新加载...")

        // 3. 批量重新加载并启动这些插件
        val pluginInfosToReload = pluginsToReload
            .mapNotNull { xmlManager.getPluginById(it) }

        if (pluginInfosToReload.size != pluginsToReload.size) {
            Timber.tag(TAG).e("无法获取部分要重启的插件信息，操作中止。")
            return false
        }

        val isSuccess = coroutineScope {
            // --- 加载阶段 ---
            val loadJobs = pluginInfosToReload.map { async { loadPlugin(it) } }
            val loadedPluginsList = loadJobs.awaitAll().filterNotNull()

            if (loadedPluginsList.size != pluginsToReload.size) {
                Timber.tag(TAG).w("部分插件在重启加载阶段失败，操作中止。")
                // 清理已成功加载的部分
                loadedPluginsList.forEach { unloadPlugin(it.pluginInfo.pluginId) }
                return@coroutineScope false
            }

            // --- 注册阶段 ---
            val successfulLoadedInfo = loadedPluginsList.associateBy { it.pluginInfo.pluginId }
            _loadedPlugins.update { it + successfulLoadedInfo }
            Timber.tag(TAG).d("重启插件已全部重新加载并注册。")

            // --- 实例化阶段 ---
            val instantiateJobs = loadedPluginsList.map { loadedPlugin ->
                async(Dispatchers.Default) {
                    instantiatePlugin(loadedPlugin)?.let { instance ->
                        loadedPlugin.pluginInfo.pluginId to instance
                    }
                }
            }
            val successfulInstances = instantiateJobs.awaitAll().filterNotNull().toMap()

            if (successfulInstances.size != pluginsToReload.size) {
                Timber.tag(TAG).e("部分插件在重启实例化阶段失败，执行回滚...")
                loadedPluginsList.forEach { unloadPlugin(it.pluginInfo.pluginId) }
                return@coroutineScope false
            }
            _pluginInstances.update { it + successfulInstances }

            Timber.tag(TAG).i("链式重启成功完成，共重启 ${pluginsToReload.size} 个插件。")
            return@coroutineScope true
        }

        return isSuccess
    }

    /**
     * 获取插件实例（直接从本地缓存获取）
     *
     * @param pluginId 插件ID
     * @return 插件实例，如果未找到则返回null
     */
    fun getPluginInstance(pluginId: String): IPluginEntryClass? {
        return try {
            // 直接从本地缓存获取插件实例
            val instance = _pluginInstances.value[pluginId]
            if (instance != null) {
                Timber.tag(TAG).d("成功获取插件实例: $pluginId")
                return instance
            }

            // 检查插件是否已加载但未实例化
            if (_pluginInstances.value.containsKey(pluginId)) {
                Timber.tag(TAG).w("插件 $pluginId 已加载但未实例化，尝试重新实例化")
                val loadedPlugin = _loadedPlugins.value[pluginId]
                if (loadedPlugin != null) {
                    val newInstance = instantiatePlugin(loadedPlugin)
                    if (newInstance != null) {
                        return newInstance
                    }
                }
            }

            Timber.tag(TAG).w("插件实例未找到: $pluginId，请确保插件已正确加载")
            null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "获取插件实例 $pluginId 时发生错误: ${e.message}")
            null
        }
    }

    /**
     * 获取已加载插件信息
     *
     * @param pluginId 插件ID
     * @return 插件信息，如果未找到则返回null
     */
    fun getPluginInfo(pluginId: String): LoadedPluginInfo? = _loadedPlugins.value[pluginId]

    /**
     * 检查插件是否已加载
     *
     * @param pluginId 插件ID
     * @return 是否已加载
     */
    fun isPluginLoaded(pluginId: String): Boolean = _loadedPlugins.value.containsKey(pluginId)

    /**
     * 获取所有已加载的插件ID列表
     *
     * @return 插件ID列表
     */
    fun getAllPluginIds(): List<String> = _loadedPlugins.value.keys.toList()

    /**
     * 获取所有插件实例
     *
     * @return 插件实例映射表
     */
    fun getAllPluginInstances(): Map<String, IPluginEntryClass> = _pluginInstances.value.toMap()

    /**
     * 获取所有已安装的插件信息
     *
     * @return 插件信息列表
     */
    fun getAllInstallPlugins(): List<PluginInfo> = xmlManager.getAllPlugins()

    // ==================== 私有方法 ====================

    /**
     * 获取所有已启用的插件信息
     */
    private fun getEnabledPlugins(): List<PluginInfo> =
        try {
            val allPlugins = xmlManager.getAllPlugins()
            val enabledPlugins = allPlugins.filter { it.enabled }

            Timber
                .tag(TAG)
                .d("从plugins.xml读取到 ${allPlugins.size} 个插件，其中 ${enabledPlugins.size} 个已启用")

            enabledPlugins
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "读取插件信息失败: ${e.message}")
            emptyList()
        }

    /**
     * 异步加载单个插件的资源和类（使用运行时缓存）
     */
    private suspend fun loadPlugin(plugin: PluginInfo): LoadedPluginInfo? =
        withContext(Dispatchers.IO) {
            try {
                // 创建运行时缓存文件（内部会检查原文件存在性）
                val pluginApkFile = File(plugin.path)
                if (!pluginApkFile.exists()) {
                    Timber.tag(TAG).e("插件 APK 文件不存在: ${plugin.path}")
                    return@withContext null
                }

                // 索引插件类（使用缓存文件）
                indexPluginClasses(plugin.pluginId, pluginApkFile)

                // 过滤出所有 enabled 的静态广播和内容提供者
                val enabledReceivers = plugin.staticReceivers.filter { it.enabled }
                val enabledProviders = plugin.providers.filter { it.enabled }

                // 过滤后的列表注册到 ProxyManager
                proxyManager.registerStaticReceivers(plugin.pluginId, enabledReceivers)
                proxyManager.registerProviders(plugin.pluginId, enabledProviders)

                // 构建 so 库的搜索路径
                val pluginInstallDir = installerManager.getPluginDirectory(plugin.pluginId)
                val abi = Build.SUPPORTED_ABIS[0]
                val nativeLibDir = File(pluginInstallDir, "${InstallerManager.NATIVE_LIBS_DIR_NAME}/$abi")
                val nativeLibraryPath = if (nativeLibDir.exists()) nativeLibDir.absolutePath else null

                // 创建配置完备的 PluginClassLoader
                val classLoader = PluginClassLoader(
                    pluginId = plugin.pluginId,
                    pluginFile = pluginApkFile,
                    parent = context.classLoader,
                    optimizedDirectory = getOptimizedDirectory(context, plugin.pluginId).absolutePath,
                    librarySearchPath = nativeLibraryPath,
                    pluginFinder = this@PluginManager.dependencyManager,
                )

                // 加载插件资源
                try {
                    // 使用 PluginResourcesManager 加载资源（支持所有Android版本）
                    val success =
                        resourcesManager.loadPluginResources(plugin.pluginId, pluginApkFile)

                    if (success) {
                        Timber.tag(TAG).d("插件 ${plugin.pluginId} 资源已加载到资源管理器")
                    } else {
                        Timber.tag(TAG).w("插件 ${plugin.pluginId} 资源加载失败")
                    }

                    success
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "插件 ${plugin.pluginId} 资源加载失败")
                    false
                }

                LoadedPluginInfo(
                    pluginInfo = plugin,
                    classLoader = classLoader
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "加载插件 ${plugin.pluginId} 失败: ${e.message}")
                null
            }
        }

    /**
     * 实例化单个插件的入口类
     */
    private fun instantiatePlugin(loadedPlugin: LoadedPluginInfo): IPluginEntryClass? {
        return try {
            val plugin = loadedPlugin.pluginInfo
            val classLoader = loadedPlugin.classLoader

            val instance =
                classLoader.getInterface(IPluginEntryClass::class.java, plugin.entryClass)

            if (instance != null) {
                Timber
                    .tag(TAG)
                    .d("插件入口类实例化成功: ${plugin.pluginId} -> ${plugin.entryClass}")
                loadKoinModules(plugin.pluginId, instance)
                executeOnLoad(plugin, instance)
                instance
            } else {
                Timber
                    .tag(TAG)
                    .e("插件入口类实例化失败: ${plugin.pluginId} -> ${plugin.entryClass}")
                null
            }
        } catch (e: Exception) {
            Timber
                .tag(TAG)
                .e(e, "实例化插件 ${loadedPlugin.pluginInfo.pluginId} 入口类失败: ${e.message}")
            null
        }
    }

    /**
     * 加载插件的Koin模块
     */
    private fun loadKoinModules(
        pluginId: String,
        instance: IPluginEntryClass,
    ) {
        try {
            val modules = instance.pluginModule
            if (modules.isNotEmpty()) {
                GlobalContext.get().loadModules(modules)
                Timber.tag(TAG).d("插件 $pluginId 的 ${modules.size} 个Koin模块加载成功")
            } else {
                Timber.tag(TAG).d("插件 $pluginId 没有提供Koin模块")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "加载插件 $pluginId 的Koin模块失败: ${e.message}")
        }
    }

    /**
     * 卸载插件的 Koin 模块
     *
     * @param pluginId 插件ID
     * @param instance 插件实例
     */
    private fun unloadKoinModules(
        pluginId: String,
        instance: IPluginEntryClass,
    ) {
        try {
            val modules = instance.pluginModule
            if (modules.isNotEmpty()) {
                GlobalContext.get().unloadModules(modules)
                Timber.tag(TAG).d("插件 $pluginId 的 ${modules.size} 个Koin模块卸载成功")
            } else {
                Timber.tag(TAG).d("插件 $pluginId 没有提供Koin模块，无需卸载")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "卸载插件 $pluginId 的Koin模块失败: ${e.message}")
        }
    }

    /**
     * 执行插件的 onLoad 方法
     *
     * @param plugin 插件信息
     * @param instance 插件实例
     */
    private fun executeOnLoad(plugin: PluginInfo, instance: IPluginEntryClass) {
        try {
            val pluginContext = PluginContext(
                application = this.context,
                pluginInfo = plugin
            )
            instance.onLoad(pluginContext)
            Timber.tag(TAG).d("插件 [${plugin.pluginId}] onLoad() Success。")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "插件 [${plugin.pluginId}] onLoad() Failed。")
        }
    }

    /**
     * 执行插件的 onUnload 方法
     *
     * @param pluginId 插件ID
     * @param instance 插件实例
     */
    private fun executeOnUnload(pluginId: String, instance: IPluginEntryClass) {
        try {
            instance.onUnload()
            Timber.tag(TAG).d("插件 [${pluginId}] onUnload() Success。")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "插件 [${pluginId}] onUnload() Failed。")
        }
    }

    /**
     * 卸载指定插件，并清理其所有相关资源和依赖记录。
     *
     * @param pluginId 插件ID
     */
    suspend fun unloadPlugin(pluginId: String) = withContext(Dispatchers.IO) {
        if (!isPluginLoaded(pluginId)) {
            Timber.tag(TAG).w("尝试卸载一个未加载的插件: $pluginId")
            return@withContext
        }

        Timber.tag(TAG).i("开始卸载插件: $pluginId")

        val instance = _pluginInstances.value[pluginId]

        // 1. 执行插件的 onUnload 方法与卸载koin模块
        if (instance != null) {
            executeOnUnload(pluginId, instance)
            unloadKoinModules(pluginId, instance)
        }

        // 2. 注销静态广播等代理组件
        proxyManager.unregisterStaticReceivers(pluginId)
        proxyManager.unregisterProviders(pluginId)

        // 3. 清理依赖关系图中的记录
        dependencyManager.clearDependenciesFor(pluginId)

        // 4. 从状态中原子地移除实例和加载信息
        _loadedPlugins.update { it - pluginId }
        _pluginInstances.update { it - pluginId }

        // 5. 移除插件资源
        resourcesManager.removePluginResources(pluginId)

        // 6. 从全局类索引中移除
        removePluginFromIndex(pluginId)

        Timber.tag(TAG).i("插件 [$pluginId] 卸载完成。")
    }

    /**
     * 清理运行时缓存目录
     */
    private fun clearDexOptCache() {
        try {
            val result = File(context.cacheDir, DEX_OPTIMIZED_DIR_NAME).deleteRecursively()
            if (result) {
                Timber.tag(TAG).i("清理DexOpt缓存成功")
            } else {
                Timber.tag(TAG).w("清理DexOpt缓存失败，目录可能不存在")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "清理DexOpt缓存失败: ${e.message}")
        }
    }

    /**
     * 获取插件的优化目录
     */
    fun getOptimizedDirectory(context: Context, pluginId: String): File {
        val cacheDir = File(context.cacheDir, DEX_OPTIMIZED_DIR_NAME)
        return File(cacheDir, pluginId).apply { mkdirs() }
    }

    /**
     * 设置插件是否自动启动
     *
     * @param pluginId 插件ID
     * @param enabled 是否自动启动
     * @return 设置是否成功
     */
    fun setPluginEnabled(
        pluginId: String,
        enabled: Boolean,
    ): Boolean {
        return try {
            Timber.tag(TAG).i("设置插件自动启动状态: $pluginId = $enabled")

            val pluginInfo = xmlManager.getPluginById(pluginId)
            if (pluginInfo == null) {
                Timber.tag(TAG).e("插件不存在: $pluginId")
                return false
            }

            if (pluginInfo.enabled == enabled) {
                Timber.tag(TAG).d("插件 $pluginId 自动启动状态无需更改: $enabled")
                return true
            }

            val updatedPluginInfo =
                PluginInfo(
                    pluginId = pluginInfo.pluginId,
                    version = pluginInfo.version,
                    path = pluginInfo.path,
                    entryClass = pluginInfo.entryClass,
                    description = pluginInfo.description,
                    enabled = enabled,
                    installTime = pluginInfo.installTime,
                    staticReceivers = pluginInfo.staticReceivers,
                    providers = pluginInfo.providers,
                )

            xmlManager.updatePlugin(updatedPluginInfo)
            xmlManager.flushToDisk()
            Timber.tag(TAG).i("插件 $pluginId 自动启动状态设置成功: $enabled")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "设置插件自动启动状态时发生错误: $pluginId - ${e.message}")
            false
        }
    }

    /**
     * 获取指定插件的完整【依赖方】链（查找“谁依赖我”）。
     *
     * 此方法回答的问题是：“如果我改变或卸载 `pluginId`，会影响到哪些上游插件？”
     * 它会递归遍历反向依赖图，找出所有直接或间接将 `pluginId` 作为其依赖项的插件。
     *
     * 这是一个执行【安全操作】（如重启或卸载）前的关键检查方法。
     *
     * @sample
     * // 依赖关系: plugin-C -> plugin-B -> plugin-A (C依赖B, B依赖A)
     * // 调用: getPluginDependentsChain("plugin-A")
     * // 返回: ["plugin-B", "plugin-C"] (顺序可能不同)
     * // 含义: B和C的运行都依赖于A，因此操作A会影响B和C。
     *
     * @param pluginId 要查询的插件ID（被依赖的插件）。
     * @return 一个包含所有依赖此插件（即依赖方）的ID列表。如果没有任何插件依赖它，则返回空列表。
     * 返回的列表中不包含 `pluginId` 自身。
     */
    fun getPluginDependentsChain(pluginId: String): List<String> {
        return dependencyManager.findDependentsRecursive(pluginId)
    }

    /**
     * 获取指定插件的完整【依赖项】链（查找“我依赖谁”）。
     *
     * 此方法回答的问题是：“要让 `pluginId` 正常运行，它需要哪些下游插件？”
     * 它会递归遍历正向依赖图，找出 `pluginId` 运行所需的所有直接或间接的插件。
     *
     * 主要用于【诊断和分析】。
     *
     * @sample
     * // 依赖关系: plugin-A -> plugin-B -> plugin-C (A依赖B, B依赖C)
     * // 调用: getPluginDependenciesChain("plugin-A")
     * // 返回: ["plugin-B", "plugin-C"] (顺序可能不同)
     * // 含义: A的运行需要B和C，在启动A之前，应确保B和C可用。
     *
     * @param pluginId 要查询的插件ID（发起依赖的插件）。
     * @return 一个包含此插件所有依赖项的ID列表。如果它不依赖任何插件，则返回空列表。
     * 返回的列表中不包含 `pluginId` 自身。
     */
    fun getPluginDependenciesChain(pluginId: String): List<String> {
        return dependencyManager.findDependenciesRecursive(pluginId)
    }

    /**
     * 为插件建立类索引
     *
     * 此方法能够健壮地处理单Dex和多Dex的APK文件。
     *
     * @param pluginId 插件ID
     * @param pluginFile 插件的APK文件
     */
    private fun indexPluginClasses(
        pluginId: String,
        pluginFile: File,
    ) {
        Timber.tag(CLASS_INDEX_TAG).d("开始为插件 [$pluginId] 建立类索引...")
        var totalIndexedClasses = 0
        try {
            val multiDexFile =
                DexFileFactory.loadDexFile(pluginFile, Opcodes.forApi(Build.VERSION.SDK_INT))
            multiDexFile.classes.forEach { classDef ->
                val className = convertDexTypeToClassName(classDef.type)

                Timber.tag(CLASS_INDEX_TAG).d("为类 [$className] 建立索引...")
                val existingPluginId = classIndex.putIfAbsent(className, pluginId)
                if (existingPluginId == null) {
                    totalIndexedClasses++
                }
            }
        } catch (e: Exception) {
            Timber
                .tag(CLASS_INDEX_TAG)
                .e(e, "为插件 [$pluginId] 建立类索引失败: ${pluginFile.absolutePath}")
        }
        Timber
            .tag(CLASS_INDEX_TAG)
            .d("插件 [$pluginId] 的类索引建立完成，共创建 $totalIndexedClasses 个类索引。")
    }

    /**
     * 辅助方法，将类型描述符 转换为标准的Java类名
     */
    private fun convertDexTypeToClassName(dexType: String): String {
        if (dexType.startsWith("L") && dexType.endsWith(";")) {
            return dexType.substring(1, dexType.length - 1).replace('/', '.')
        }
        return dexType.replace('/', '.')
    }

    /**
     * 从类索引中移除指定插件的所有类
     */
    private fun removePluginFromIndex(pluginId: String) {
        Timber.tag(CLASS_INDEX_TAG).d("正在从类索引中移除插件 [$pluginId] 的条目...")
        var removedCount = 0
        val iterator = classIndex.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value == pluginId) {
                iterator.remove()
                removedCount++
            }
        }
        Timber.tag(CLASS_INDEX_TAG).d("从索引中移除了插件 [$pluginId] 的 $removedCount 个类。")
    }

    /**
     * 从所有已加载的插件中，获取指定接口的实现实例。
     *
     * 这是一个便捷的顶层API方法，它利用全局类索引自动查找并加载类，
     * 无需调用者关心该类具体属于哪个插件。
     *
     * @param T 接口类型泛型。请确保 T 为非空类型，例如 `IYourInterface::class.java`
     * @param interfaceClass 接口的Class对象
     * @param className 实现该接口的完整类名
     * @return 接口实现的实例，如果类未找到、插件未加载或实例化失败，则返回null
     */
    fun <T : Any> getInterface(
        interfaceClass: Class<T>,
        className: String,
    ): T? {
        try {
            // 使用全局类索引，快速定位此类属于哪个插件
            val targetPluginId = classIndex[className]
            if (targetPluginId == null) {
                val instance = getInterfaceFromHost(interfaceClass, className)
                if (instance != null) {
                    return instance
                }
                Timber.tag(TAG).w("无法找到类 '$className' 的宿主插件，类索引中不存在该条目。")
                return null
            }

            val loadedPlugin = _loadedPlugins.value[targetPluginId]
            if (loadedPlugin == null) {
                Timber
                    .tag(TAG)
                    .e("类索引不一致：类 '$className' 指向插件 '$targetPluginId'，但该插件当前未加载。")
                return null
            }

            Timber
                .tag(TAG)
                .d("正在从插件 '$targetPluginId' 中获取接口 '${interfaceClass.simpleName}' 的实现 '$className'...")
            return loadedPlugin.classLoader.getInterface(interfaceClass, className)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "通过 PluginManager 获取接口 '$className' 的实例时发生未知错误。")
            return null
        }
    }

    private fun <T : Any> getInterfaceFromHost(
        interfaceClass: Class<T>,
        className: String,
    ): T? {
        return try {
            val clazz = context.classLoader.loadClass(className)
            val instance = clazz.getDeclaredConstructor().newInstance()

            if (interfaceClass.isInstance(instance)) {
                @Suppress("UNCHECKED_CAST")
                instance as T
            } else {
                Timber.tag(TAG)
                    .e("类型不匹配：类 '$className' 未实现接口 '${interfaceClass.simpleName}'")
                null
            }
        } catch (_: ClassNotFoundException) {
            Timber.tag(TAG).w("未找到类: $className")
            null
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "实例化 '$className' 时发生错误")
            null
        }
    }
}
