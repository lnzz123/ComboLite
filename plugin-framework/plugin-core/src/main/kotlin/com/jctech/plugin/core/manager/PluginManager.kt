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
import com.jctech.plugin.core.ext.update
import com.jctech.plugin.core.installer.InstallerManager
import com.jctech.plugin.core.installer.XmlManager
import com.jctech.plugin.core.interfaces.IPluginEntryClass
import com.jctech.plugin.core.loader.IPluginFinder
import com.jctech.plugin.core.loader.PluginClassLoader
import com.jctech.plugin.core.model.PluginInfo
import com.jctech.plugin.core.model.PluginState
import com.jctech.plugin.core.resources.PluginResourcesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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
    
    // 使用lateinit var声明依赖，并在init方法中初始化
    private lateinit var context: Application
    private lateinit var xmlManager: XmlManager
    private lateinit var installerManager: InstallerManager
    private lateinit var resourcesManager: PluginResourcesManager
    
    // 初始化状态标识
    @Volatile
    private var isInitializedInternal = false

    // 运行时缓存目录
    private val runtimeCacheDir: File by lazy {
        File(context.cacheDir, "plugins_runtime").apply {
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

                clearRuntimeCache()
                
                isInitializedInternal = true

                callback()
                
                Timber.tag(TAG).i("PluginManager初始化完成")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "PluginManager初始化失败: ${e.message}")
                throw e
            }
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
     * 3. 实例化所有插件的入口类对象
     * 4. 加载插件的 Koin 模块
     *
     * @return 成功加载的插件数量
     */
    suspend fun loadEnabledPlugins(): Int = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).i("开始异步初始化所有已启用的插件")

            // 1. 获取所有已启用的插件
            val enabledPlugins = getEnabledPlugins()
            if (enabledPlugins.isEmpty()) {
                Timber.tag(TAG).i("没有找到已启用的插件")
                return@withContext 0
            }

            Timber.tag(TAG).i("找到 ${enabledPlugins.size} 个已启用的插件，开始并发加载")

            // 2. 并发加载所有插件的 dex 和资源文件
            val loadJobs = enabledPlugins.filter { !isPluginLoaded(it.pluginId) }.map { plugin ->
                async(Dispatchers.IO) {
                    loadPlugin(plugin)?.also {
                        _loadedPlugins.update { this[plugin.pluginId] = it }
                        Timber.tag(TAG).d("插件 ${plugin.pluginId} 加载成功")
                    } ?: run {
                        Timber.tag(TAG).e("插件 ${plugin.pluginId} 加载失败")
                        null
                    }
                }
            }

            val loadedPluginsList = loadJobs.awaitAll().filterNotNull()
            Timber.tag(TAG).i("插件加载阶段完成，成功加载 ${loadedPluginsList.size} 个插件")

            // 3. 并发实例化所有插件的入口类对象（CPU密集型操作）
            val instantiateJobs = loadedPluginsList.map { loadedPlugin ->
                async(Dispatchers.Default) {
                    val pluginId = loadedPlugin.pluginInfo.pluginId
                    try {
                        val instance = instantiatePlugin(loadedPlugin)
                        if (instance != null) {
                            _pluginInstances.update { this[pluginId] = instance }
                            Timber.tag(TAG).d("插件 $pluginId 实例化成功")
                            pluginId to instance
                        } else {
                            Timber.tag(TAG).e("插件 $pluginId 实例化失败")
                            _loadedPlugins.update { remove(pluginId) }
                            null
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "插件 $pluginId 实例化异常: ${e.message}")
                        _loadedPlugins.update { remove(pluginId) }
                        _pluginInstances.update { remove(pluginId) }
                        null
                    }
                }
            }

            val instances = instantiateJobs.awaitAll().filterNotNull()

            // 4. 加载插件的 Koin 模块
            instances.forEach { (pluginId, instance) ->
                loadKoinModules(pluginId, instance)
            }

            val successCount = instances.size
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

            // 如果插件已经启动，执行重启逻辑
            if (isPluginLoaded(pluginId)) {
                Timber.tag(TAG).i("插件 $pluginId 已启动，执行重启逻辑")
                
                // 从Koin中移除插件实例
                _pluginInstances.value[pluginId]?.let { unloadKoinModules(pluginId, it) }

                // 从 PluginResourcesManager 中移除插件资源
                resourcesManager.removePluginResources(pluginId)

                // 清理运行时缓存文件
                cleanupCacheFile(pluginId)

                // 移除插件相关缓存
                _loadedPlugins.update { remove(pluginId) }
                _pluginInstances.update { remove(pluginId) }

                // 使用协程延迟确保资源完全释放
                delay(100)
            }

            // 获取插件信息
            val pluginInfo = xmlManager.getPluginById(pluginId)
            if (pluginInfo == null) {
                Timber.tag(TAG).e("插件信息未找到: $pluginId")
                return@withContext false
            }

            // 异步加载插件
            val loadedPlugin = loadPlugin(pluginInfo)
            if (loadedPlugin == null) {
                Timber.tag(TAG).e("插件加载失败: $pluginId")
                return@withContext false
            }

            // 在Default线程池实例化插件入口类（CPU密集型操作）
            val instance = withContext(Dispatchers.Default) {
                instantiatePlugin(loadedPlugin)
            }
            if (instance == null) {
                Timber.tag(TAG).e("插件实例化失败: $pluginId")
                return@withContext false
            }

            // 保存到缓存
            _loadedPlugins.update { this[pluginId] = loadedPlugin }
            _pluginInstances.update { this[pluginId] = instance }

            // 在主线程加载插件的Koin模块
            loadKoinModules(pluginId, instance)

            Timber.tag(TAG).i("插件异步启动成功: $pluginId")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "启动插件 $pluginId 时发生错误: ${e.message}")
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

    /**
     * 获取插件安装器实例
     *
     * @return PluginInstaller 实例
     */
    fun getInstallerManager(): InstallerManager {
        return installerManager
    }

    /**
     * 获取插件资源管理器实例
     *
     * @return PluginResourcesManager 实例
     */
    fun getResourcesManager(): PluginResourcesManager {
        return resourcesManager
    }

    // ==================== 私有方法 ====================

    /**
     * 获取所有已启用的插件信息
     */
    private fun getEnabledPlugins(): List<PluginInfo> {
        return try {
            val allPlugins = xmlManager.getAllPlugins()
            val enabledPlugins = allPlugins.filter { it.status == PluginState.enabled }

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
            val cacheFileName = "${pluginId}_${System.currentTimeMillis()}.apk"
            val cacheFile = File(runtimeCacheDir, cacheFileName)

            Timber.tag(TAG).d("创建运行时缓存文件: $pluginId -> ${cacheFile.absolutePath}")

            // 异步复制文件到缓存目录
            val buffer = ByteArray(8192)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
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
                fileName.startsWith("${pluginId}_") && fileName.endsWith(".apk")
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

            val newStatus = if (autoLaunch) PluginState.enabled else PluginState.disabled
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

    /**
     * 查找类
     *
     * 该方法会遍历所有已加载的插件，查找指定插件ID下的类。
     * 如果找到，会返回类对象；如果未找到，会返回null。
     *
     * @param pluginId 插件ID
     * @param className 类名
     * @return 类对象，未找到返回null
     */
    override fun findClass(pluginId: String, className: String): Class<*>? {
        _loadedPlugins.value.forEach { (loadedPluginId, loadedPluginInfo) ->
            if (loadedPluginId != pluginId) {
                try {
                    return loadedPluginInfo.classLoader.findClassLocally(className)
                } catch (_: ClassNotFoundException) {
                    Timber.tag(TAG).d("插件 $loadedPluginId 未找到类 $className")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "在插件 $loadedPluginId 中查找类 $className 时出错")
                }
            }
        }
        return null
    }
}
