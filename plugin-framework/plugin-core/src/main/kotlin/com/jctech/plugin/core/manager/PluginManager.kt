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

package com.jctech.plugin.core.manager

import android.app.Application
import android.content.res.loader.ResourcesLoader
import android.os.Build
import com.jctech.plugin.core.installer.InstallerManager
import com.jctech.plugin.core.installer.XmlManager
import com.jctech.plugin.core.interfaces.IPluginEntryClass
import com.jctech.plugin.core.loader.IPluginFinder
import com.jctech.plugin.core.loader.PluginClassLoader
import com.jctech.plugin.core.model.PluginInfo
import com.jctech.plugin.core.model.PluginState
import com.jctech.plugin.core.proxy.ProxyManager
import com.jctech.plugin.core.resources.PluginResourcesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.koin.core.context.GlobalContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * 插件管理器
 *
 * 负责插件的安装、运行和更新管理
 * 支持插件的动态加载、资源管理和依赖注入
 * 整合了原PluginLauncher的功能，提供统一的插件生命周期管理
 *
 * 注意：此类已改造为全局单例模式，不再依赖Koin
 */
object PluginManager : IPluginFinder {
    private const val TAG = "PluginManager"
    private const val CLASS_INDEX_TAG = "ClassIndex"
    private const val RUNTIME_CACHE_DIR_NAME = "plugins_runtime"
    private const val PLUGIN_FILE_EXTENSION = ".apk"

    private lateinit var context: Application
    private lateinit var xmlManager: XmlManager
    lateinit var installerManager: InstallerManager
        private set
    lateinit var resourcesManager: PluginResourcesManager
        private set
    lateinit var proxyManager: ProxyManager
        private set


    // 初始化状态标识
    @Volatile
    private var isInitializedInternal = false

    // 运行时缓存目录
    private val runtimeCacheDir: File by lazy {
        File(context.cacheDir, RUNTIME_CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
                Timber.tag(TAG).d("创建运行时插件缓存目录: $absolutePath")
            }
        }
    }

    // 存储已加载的插件信息 - 使用StateFlow
    private val _loadedPlugins = MutableStateFlow<Map<String, LoadedPluginInfo>>(emptyMap())
    val loadedPluginsFlow: StateFlow<Map<String, LoadedPluginInfo>> = _loadedPlugins.asStateFlow()

    // 存储插件实例的缓存 - 使用StateFlow
    private val _pluginInstances = MutableStateFlow<Map<String, IPluginEntryClass>>(emptyMap())
    val pluginInstancesFlow: StateFlow<Map<String, IPluginEntryClass>> = _pluginInstances.asStateFlow()

    // 插件类名索引
    private val classIndex = ConcurrentHashMap<String, String>()

    // 初始化完成的锁对象
    private val initializationLock = Any()

    fun isInitialized(): Boolean {
        return isInitializedInternal
    }

    /**
     * 初始化插件管理器
     *
     * @param context 应用上下文
     * @param callback 初始化完成回调
     */
    fun initialize(context: Application, callback: () -> Unit = {}) {
        if (isInitializedInternal) {
            Timber.tag(TAG).w("PluginManager已经初始化，跳过重复初始化")
            callback()
            return
        }

        synchronized(initializationLock) {
            if (isInitializedInternal) {
                return
            }

            try {
                Timber.tag(TAG).i("开始初始化PluginManager")

                this.context = context
                this.xmlManager = XmlManager(this.context)
                this.installerManager = InstallerManager(this.context, xmlManager)
                this.resourcesManager = PluginResourcesManager(this.context)
                this.proxyManager = ProxyManager()

                clearRuntimeCache()

                isInitializedInternal = true

                Timber.tag(TAG).i("PluginManager初始化完成")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "PluginManager初始化失败: ${e.message}")
                // 失败时重置状态
                isInitializedInternal = false
                throw e // 仍然抛出异常，让上层知道初始化失败
            }
        }

        // 将回调移出同步块
        if (isInitializedInternal) {
            callback()
        }
    }

    /**
     * 已加载插件信息数据类
     */
    data class LoadedPluginInfo(
        val pluginInfo: PluginInfo,
        val classLoader: PluginClassLoader,
        val resourcesLoader: ResourcesLoader? = null,
        val runtimeCacheFile: File? = null, // 运行时缓存文件路径
    )


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
    suspend fun loadEnabledPlugins(): Int = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).i("开始异步初始化所有已启用的插件")

            // 1. 获取所有已启用的插件
            val enabledPlugins = getEnabledPlugins().filter { !isPluginLoaded(it.pluginId) }
            if (enabledPlugins.isEmpty()) {
                Timber.tag(TAG).i("没有找到需要加载的新插件")
                return@withContext 0
            }

            Timber.tag(TAG).i("找到 ${enabledPlugins.size} 个已启用的插件，开始加载")

            // 2. 并发加载所有插件的 dex 和资源文件
            val loadJobs = enabledPlugins.map { plugin ->
                async(Dispatchers.IO) {
                    loadPlugin(plugin)
                }
            }
            val loadedPluginsList = loadJobs.awaitAll().filterNotNull()
            if (loadedPluginsList.isEmpty()){
                Timber.tag(TAG).w("所有插件均加载失败")
                return@withContext 0
            }
            Timber.tag(TAG).i("插件加载阶段完成，成功加载 ${loadedPluginsList.size} 个插件")


            // 3. 并发实例化所有插件的入口类对象
            val instantiateJobs = loadedPluginsList.map { loadedPlugin ->
                async(Dispatchers.Default) {
                    instantiatePlugin(loadedPlugin)?.let { instance ->
                        loadedPlugin.pluginInfo.pluginId to instance // 返回 PluginId 和 实例的 Pair
                    }
                }
            }
            val successfulInstances = instantiateJobs.awaitAll().filterNotNull().toMap()
            if (successfulInstances.isEmpty()){
                Timber.tag(TAG).e("所有已加载的插件均实例化失败")
                loadedPluginsList.forEach{ unloadPlugin(it.pluginInfo.pluginId) }
                return@withContext 0
            }

            // 优化点 3: 集中进行状态更新，保证原子性
            val successfulLoadedInfo = loadedPluginsList
                .filter { successfulInstances.containsKey(it.pluginInfo.pluginId) }
                .associateBy { it.pluginInfo.pluginId }

            _loadedPlugins.update { it + successfulLoadedInfo }
            _pluginInstances.update { it + successfulInstances }

            // 4. 加载插件的 Koin 模块
            successfulInstances.forEach { (pluginId, instance) ->
                loadKoinModules(pluginId, instance)
            }

            val successCount = successfulInstances.size
            Timber.tag(TAG).i("插件异步加载完成，成功加载 $successCount 个插件")

            successCount
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "批量加载插件时发生错误: ${e.message}")
            0
        }
    }

    /**
     * 异步启动指定插件（支持重启已加载的插件）
     *
     * @param pluginId 插件ID
     * @return 是否启动成功
     */
    suspend fun launchPlugin(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).i("开始异步启动插件: $pluginId")

            // 如果插件已经启动，执行卸载逻辑
            if (isPluginLoaded(pluginId)) {
                Timber.tag(TAG).i("插件 $pluginId 已启动，执行重启逻辑")
                unloadPlugin(pluginId)
            }

            // 获取插件信息
            val pluginInfo = xmlManager.getPluginById(pluginId)
            if (pluginInfo == null) {
                Timber.tag(TAG).e("插件信息未找到: $pluginId")
                return@withContext false
            }

            // 加载插件
            val loadedPlugin = loadPlugin(pluginInfo)
            if (loadedPlugin == null) {
                Timber.tag(TAG).e("插件加载失败: $pluginId")
                return@withContext false
            }

            // 实例化插件入口类
            val instance = withContext(Dispatchers.Default) {
                instantiatePlugin(loadedPlugin)
            }
            if (instance == null) {
                Timber.tag(TAG).e("插件实例化失败: $pluginId")
                unloadPlugin(pluginId)
                return@withContext false
            }

            _loadedPlugins.update { it + (pluginId to loadedPlugin) }
            _pluginInstances.update { it + (pluginId to instance) }

            Timber.tag(TAG).d("StateFlows for $pluginId 更新完成。")

            // 加载插件的Koin模块
            loadKoinModules(pluginId, instance)

            Timber.tag(TAG).i("插件异步启动成功: $pluginId")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "启动插件 $pluginId 时发生错误: ${e.message}")
            if (isPluginLoaded(pluginId)) {
                unloadPlugin(pluginId)
            }
            false
        }
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
    fun getPluginInfo(pluginId: String): LoadedPluginInfo? {
        return _loadedPlugins.value[pluginId]
    }

    /**
     * 检查插件是否已加载
     *
     * @param pluginId 插件ID
     * @return 是否已加载
     */
    fun isPluginLoaded(pluginId: String): Boolean {
        return _loadedPlugins.value.containsKey(pluginId)
    }

    /**
     * 获取所有已加载的插件ID列表
     *
     * @return 插件ID列表
     */
    fun getPluginIds(): List<String> {
        return _loadedPlugins.value.keys.toList()
    }

    /**
     * 获取所有插件实例
     *
     * @return 插件实例映射表
     */
    fun getPluginInstances(): Map<String, IPluginEntryClass> {
        return _pluginInstances.value.toMap()
    }

    // 获取所有已加载的插件信息
    fun getAllInstallPlugins(): List<PluginInfo> {
        return xmlManager.getAllPlugins()
    }


    // ==================== 私有方法 ====================

    /**
     * 获取所有已启用的插件信息
     */
    private fun getEnabledPlugins(): List<PluginInfo> {
        return try {
            val allPlugins = xmlManager.getAllPlugins()
            val enabledPlugins = allPlugins.filter { it.status == PluginState.Enabled }

            Timber.tag(TAG).d("从plugins.xml读取到 ${allPlugins.size} 个插件，其中 ${enabledPlugins.size} 个已启用")

            enabledPlugins
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "读取插件信息失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 异步加载单个插件的资源和类（使用运行时缓存）
     */
    private suspend fun loadPlugin(plugin: PluginInfo): LoadedPluginInfo? = withContext(Dispatchers.IO) {
        try {
            // 创建运行时缓存文件（内部会检查原文件存在性）
            val runtimeCacheFile = createCacheFile(plugin.pluginId)
            if (runtimeCacheFile == null) {
                Timber.tag(TAG).e("创建运行时缓存文件失败: ${plugin.pluginId}")
                return@withContext null
            }

            // 索引插件类（使用缓存文件）
            indexPluginClasses(plugin.pluginId, runtimeCacheFile)

            // 使用缓存文件创建类加载器
            val classLoader = PluginClassLoader(
                pluginId = plugin.pluginId,
                pluginFile = runtimeCacheFile,
                parent = context.classLoader,
                pluginFinder = this@PluginManager
            )

            // 加载插件资源（支持所有Android版本）
            val resourcesLoaded = try {
                // 使用 PluginResourcesManager 加载资源（支持所有Android版本）
                val success = resourcesManager.loadPluginResources(plugin.pluginId, runtimeCacheFile)

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

            // ResourcesLoader 字段现在仅用于标记是否成功加载资源
            val resourcesLoader = if (resourcesLoaded) {
                // 创建一个占位符，实际资源管理由 PluginResourcesManager 处理
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ResourcesLoader() // 占位符
                } else {
                    null // 低版本不使用 ResourcesLoader
                }
            } else {
                null
            }

            LoadedPluginInfo(
                pluginInfo = plugin,
                classLoader = classLoader,
                resourcesLoader = resourcesLoader,
                runtimeCacheFile = runtimeCacheFile,
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

            // 使用类加载器获取插件入口类实例
            val instance = classLoader.getInterface(IPluginEntryClass::class.java, plugin.entryClass)

            if (instance != null) {
                Timber.tag(TAG).d("插件入口类实例化成功: ${plugin.pluginId} -> ${plugin.entryClass}")
                instance
            } else {
                Timber.tag(TAG).e("插件入口类实例化失败: ${plugin.pluginId} -> ${plugin.entryClass}")
                null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "实例化插件 ${loadedPlugin.pluginInfo.pluginId} 入口类失败: ${e.message}")
            null
        }
    }

    /**
     * 加载插件的Koin模块
     */
    private fun loadKoinModules(pluginId: String, instance: IPluginEntryClass) {
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
    private fun unloadKoinModules(pluginId: String, instance: IPluginEntryClass) {
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
     * 卸载指定插件
     *
     * @param pluginId 插件ID
     */
    private suspend fun unloadPlugin(pluginId: String) = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("开始卸载插件: $pluginId")
        _pluginInstances.value[pluginId]?.let { unloadKoinModules(pluginId, it) }

        // 原子地移除实例和加载信息
        _loadedPlugins.update { it - pluginId }
        _pluginInstances.update { it - pluginId }

        resourcesManager.removePluginResources(pluginId)
        removePluginFromIndex(pluginId)
        cleanupCacheFile(pluginId)
        Timber.tag(TAG).i("插件 $pluginId 卸载完成")
    }


    // ========== 运行时缓存管理方法 ==========

    /**
     * 清理运行时缓存目录
     */
    private fun clearRuntimeCache() {
        try {
            if (runtimeCacheDir.exists()) {
                val deleted = runtimeCacheDir.deleteRecursively()
                if (deleted) {
                    Timber.tag(TAG).d("运行时缓存目录清理成功")
                    // 重新创建目录
                    runtimeCacheDir.mkdirs()
                } else {
                    Timber.tag(TAG).w("运行时缓存目录清理失败")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "清理运行时缓存目录时发生错误: ${e.message}")
        }
    }

    /**
     * 为插件创建运行时缓存文件
     *
     * @param pluginId 插件ID
     * @return 缓存文件，失败返回null
     */
    private suspend fun createCacheFile(pluginId: String): File? = withContext(Dispatchers.IO) {
        try {
            // 通过插件ID获取原文件路径
            val pluginInfo = xmlManager.getPluginById(pluginId)
            if (pluginInfo == null) {
                Timber.tag(TAG).e("未找到插件信息: $pluginId")
                return@withContext null
            }

            val sourceFile = File(pluginInfo.path)
            if (!sourceFile.exists()) {
                Timber.tag(TAG).e("插件源文件不存在: ${pluginInfo.path}")
                return@withContext null
            }

            // 生成缓存文件名
            val cacheFile = File(runtimeCacheDir, "${pluginId}_${System.currentTimeMillis()}$PLUGIN_FILE_EXTENSION")
            Timber.tag(TAG).d("创建运行时缓存文件: $pluginId -> ${cacheFile.absolutePath}")
            FileInputStream(sourceFile).channel.use { sourceChannel ->
                FileOutputStream(cacheFile).channel.use { destinationChannel ->
                    sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel)
                }
            }

            // 验证复制结果
            if (!cacheFile.exists() || cacheFile.length() != sourceFile.length()) {
                Timber.tag(TAG).e("运行时缓存文件创建验证失败: $pluginId")
                cacheFile.delete()
                return@withContext null
            }

            // 设置文件为只读权限，防止Android系统报"Writable dex file"错误
            if (!cacheFile.setReadOnly()) {
                Timber.tag(TAG).w("设置运行时缓存文件为只读失败: ${cacheFile.absolutePath}")
            } else {
                Timber.tag(TAG).d("运行时缓存文件已设置为只读: ${cacheFile.absolutePath}")
            }

            Timber.tag(TAG).d("运行时缓存文件创建成功: $pluginId")
            cacheFile
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "创建运行时缓存文件失败: $pluginId - ${e.message}")
            null
        }
    }

    /**
     * 清理单个插件的运行时缓存文件
     *
     * @param pluginId 插件ID
     */
    private fun cleanupCacheFile(pluginId: String) {
        try {
            // 查找所有匹配的缓存文件（可能有多个历史缓存）
            val cacheFiles = runtimeCacheDir.listFiles { _, fileName ->
                fileName.startsWith("${pluginId}_") && fileName.endsWith(PLUGIN_FILE_EXTENSION)
            }

            if (cacheFiles != null && cacheFiles.isNotEmpty()) {
                var deletedCount = 0
                cacheFiles.forEach { cacheFile ->
                    try {
                        if (cacheFile.delete()) {
                            deletedCount++
                            Timber.tag(TAG).d("清理缓存文件: ${cacheFile.name}")
                        } else {
                            Timber.tag(TAG).w("清理缓存文件失败: ${cacheFile.name}")
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "删除缓存文件异常: ${cacheFile.name}")
                    }
                }
                Timber.tag(TAG).d("插件 $pluginId 运行时缓存清理完成，共清理 $deletedCount 个文件")
            } else {
                Timber.tag(TAG).d("插件 $pluginId 无运行时缓存文件需要清理")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "清理插件 $pluginId 运行时缓存时发生错误: ${e.message}")
        }
    }

    /**
     * 设置插件是否自动启动
     *
     * @param pluginId 插件ID
     * @param autoLaunch 是否自动启动
     * @return 设置是否成功
     */
    fun setPluginAutoLaunch(pluginId: String, autoLaunch: Boolean): Boolean {
        return try {
            Timber.tag(TAG).i("设置插件自动启动状态: $pluginId = $autoLaunch")
            
            val pluginInfo = xmlManager.getPluginById(pluginId)
            if (pluginInfo == null) {
                Timber.tag(TAG).e("插件不存在: $pluginId")
                return false
            }

            val newStatus = if (autoLaunch) PluginState.Enabled else PluginState.Disabled
            if (pluginInfo.status == newStatus) {
                Timber.tag(TAG).d("插件 $pluginId 自动启动状态无需更改: $autoLaunch")
                return true
            }

            val updatedPluginInfo = PluginInfo(
                pluginId = pluginInfo.pluginId,
                version = pluginInfo.version,
                path = pluginInfo.path,
                entryClass = pluginInfo.entryClass,
                description = pluginInfo.description,
                status = newStatus,
                installTime = pluginInfo.installTime
            )

            xmlManager.updatePlugin(updatedPluginInfo)
            xmlManager.flushToDisk()
            Timber.tag(TAG).i("插件 $pluginId 自动启动状态设置成功: $autoLaunch")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "设置插件自动启动状态时发生错误: $pluginId - ${e.message}")
            false
        }
    }

    // ==================== 核心 findClass 优化实现 ====================

    /**
     * 为插件建立类索引
     *
     * 此方法能够健壮地处理单Dex和多Dex的APK文件。
     *
     * @param pluginId 插件ID
     * @param pluginFile 插件的APK文件
     */
    private fun indexPluginClasses(pluginId: String, pluginFile: File) {
        Timber.tag(CLASS_INDEX_TAG).d("开始为插件 [$pluginId] 建立类索引...")
        var totalIndexedClasses = 0
        try {
            val multiDexFile = DexFileFactory.loadDexFile(pluginFile, Opcodes.forApi(Build.VERSION.SDK_INT))
            multiDexFile.classes.forEach { classDef ->
                val className = convertDexTypeToClassName(classDef.type)

                Timber.tag(CLASS_INDEX_TAG).d("为类 [$className] 建立索引...")
                val existingPluginId = classIndex.putIfAbsent(className, pluginId)
                if (existingPluginId == null) {
                    totalIndexedClasses++
                }
            }
        } catch (e: Exception) {
            Timber.tag(CLASS_INDEX_TAG).e(e, "为插件 [$pluginId] 建立类索引失败: ${pluginFile.absolutePath}")
        }
        Timber.tag(CLASS_INDEX_TAG).d("插件 [$pluginId] 的类索引建立完成，共创建 $totalIndexedClasses 个类索引。")
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
     * 实现新的 IPluginFinder 接口
     * 该方法通过全局类索引实现O(1)时间复杂度的查找
     *
     * @param className 类的完整名称
     * @return 如果找到，则返回 Class 对象，否则返回 null
     */
    override fun findClass(className: String): Class<*>? {
        val targetPluginId = classIndex[className] ?: return null

        val targetPluginInfo = _loadedPlugins.value[targetPluginId]
        if (targetPluginInfo == null) {
            Timber.tag(TAG).e("类索引不一致: 类 '$className' 指向插件 '$targetPluginId'，但该插件未加载。可能索引已过期。")
            return null
        }

        return try {
            targetPluginInfo.classLoader.findClassLocally(className)
        } catch (_: ClassNotFoundException) {
            Timber.tag(TAG).e("类索引不一致: 在插件 '$targetPluginId' 的dex中未找到其索引的类 '$className'。")
            null
        }
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
    fun <T : Any> getInterface(interfaceClass: Class<T>, className: String): T? {
        try {
            // 使用全局类索引，快速定位此类属于哪个插件
            val targetPluginId = classIndex[className]
            if (targetPluginId == null) {
                Timber.tag(TAG).w("无法找到类 '$className' 的宿主插件，类索引中不存在该条目。")
                return null
            }

            // 获取该插件的已加载信息
            val loadedPlugin = _loadedPlugins.value[targetPluginId]
            if (loadedPlugin == null) {
                Timber.tag(TAG).e("类索引不一致：类 '$className' 指向插件 '$targetPluginId'，但该插件当前未加载。")
                return null
            }

            // 调用具体的 PluginClassLoader 来加载并实例化接口
            Timber.tag(TAG).d("正在从插件 '$targetPluginId' 中获取接口 '${interfaceClass.simpleName}' 的实现 '$className'...")
            return loadedPlugin.classLoader.getInterface(interfaceClass, className)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "通过 PluginManager 获取接口 '$className' 的实例时发生未知错误。")
            return null
        }
    }
}
