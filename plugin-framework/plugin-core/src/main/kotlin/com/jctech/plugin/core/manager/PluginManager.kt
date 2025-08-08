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

import android.content.Context
import android.content.res.loader.ResourcesLoader
import android.os.Build
import com.jctech.plugin.core.installer.InstallerManager
import com.jctech.plugin.core.installer.InstallerManager.InstallResult
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
import kotlinx.coroutines.withContext
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
 * 注意：此类已改造为支持Koin依赖注入，不再使用单例模式
 */
class PluginManager(
    private val context: Context,
    private val xmlManager: XmlManager,
    private val installerManager: InstallerManager,
    private val resourcesManager: PluginResourcesManager,
): IPluginFinder {
    companion object {
        private const val TAG = "PluginManager"
    }

    // 运行时缓存目录
    private val runtimeCacheDir: File by lazy {
        File(context.cacheDir, "plugins_runtime").apply {
            if (!exists()) {
                mkdirs()
                Timber.tag(TAG).d("创建运行时插件缓存目录: $absolutePath")
            }
        }
    }

    // 存储已加载的插件信息
    private val loadedPlugins = ConcurrentHashMap<String, LoadedPluginInfo>()

    // 存储插件实例的缓存
    private val pluginInstances = ConcurrentHashMap<String, IPluginEntryClass>()

    // 初始化状态标识
    @Volatile
    private var isInitialized = false

    // 初始化完成回调列表
    private val initializationCallbacks = mutableListOf<() -> Unit>()

    // 初始化完成的锁对象
    private val initializationLock = Any()

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
     * 异步初始化所有已启用的插件
     *
     * 该方法会按顺序执行以下操作：
     * 1. 从 xmlManager 读取所有已启用的插件信息
     * 2. 并发加载所有插件的 dex 和资源文件
     * 3. 实例化所有插件的入口类对象
     * 4. 加载插件的 Koin 模块
     *
     * @return 成功初始化的插件数量
     */
    suspend fun initialize(): Int = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).i("开始异步初始化所有已启用的插件")

            // 0. 清理运行时缓存目录
            clearRuntimeCache()

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
                    loadSinglePlugin(plugin)?.also {
                        loadedPlugins[plugin.pluginId] = it
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
                        val instance = instantiateSinglePlugin(loadedPlugin)
                        if (instance != null) {
                            pluginInstances[pluginId] = instance
                            Timber.tag(TAG).d("插件 $pluginId 实例化成功")
                            pluginId to instance
                        } else {
                            Timber.tag(TAG).e("插件 $pluginId 实例化失败")
                            loadedPlugins.remove(pluginId)
                            null
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "插件 $pluginId 实例化异常: ${e.message}")
                        loadedPlugins.remove(pluginId)
                        pluginInstances.remove(pluginId)
                        null
                    }
                }
            }

            val instances = instantiateJobs.awaitAll().filterNotNull()

            // 4. 在主线程加载插件的 Koin 模块
            withContext(Dispatchers.Main.immediate) {
                instances.forEach { (pluginId, instance) ->
                    loadPluginKoinModules(pluginId, instance)
                }
            }

            val successCount = instances.size
            Timber.tag(TAG).i("插件异步初始化完成，成功初始化 $successCount 个插件")

            // 设置初始化状态为成功
            synchronized(initializationLock) {
                isInitialized = true
                // 执行所有等待的回调
                initializationCallbacks.forEach { callback ->
                    try {
                        callback()
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "执行初始化回调时发生错误")
                    }
                }
                initializationCallbacks.clear()
            }

            successCount
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "批量初始化插件时发生错误: ${e.message}")
            isInitialized = false
            0
        }
    }

    /**
     * 异步启动指定插件
     *
     * @param pluginId 插件ID
     * @return 是否启动成功
     */
    suspend fun launchPlugin(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).i("开始异步启动插件: $pluginId")

            // 检查插件是否已经启动
            if (isPluginLoaded(pluginId)) {
                Timber.tag(TAG).w("插件 $pluginId 已经启动，跳过重复启动")
                return@withContext true
            }

            // 获取插件信息
            val pluginInfo = xmlManager.getPluginById(pluginId)
            if (pluginInfo == null) {
                Timber.tag(TAG).e("插件信息未找到: $pluginId")
                return@withContext false
            }

            // 检查插件状态
            if (pluginInfo.status != PluginState.enabled) {
                Timber.tag(TAG).w("插件 $pluginId 未启用，状态: ${pluginInfo.status}")
                return@withContext false
            }

            // 异步加载插件
            val loadedPlugin = loadSinglePlugin(pluginInfo)
            if (loadedPlugin == null) {
                Timber.tag(TAG).e("插件加载失败: $pluginId")
                return@withContext false
            }

            // 在Default线程池实例化插件入口类（CPU密集型操作）
            val instance = withContext(Dispatchers.Default) {
                instantiateSinglePlugin(loadedPlugin)
            }
            if (instance == null) {
                Timber.tag(TAG).e("插件实例化失败: $pluginId")
                return@withContext false
            }

            // 保存到缓存
            loadedPlugins[pluginId] = loadedPlugin
            pluginInstances[pluginId] = instance

            // 在主线程加载插件的Koin模块
            withContext(Dispatchers.Main.immediate) {
                loadPluginKoinModules(pluginId, instance)
            }

            Timber.tag(TAG).i("插件异步启动成功: $pluginId")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "启动插件 $pluginId 时发生错误: ${e.message}")
            false
        }
    }

    /**
     * 关闭指定插件（清理运行时缓存）
     *
     * @param pluginId 插件ID
     * @return 是否关闭成功
     */
    fun closePlugin(pluginId: String): Boolean {
        return try {
            Timber.tag(TAG).i("开始关闭插件: $pluginId")

            // 检查插件是否已加载
            if (!isPluginLoaded(pluginId)) {
                Timber.tag(TAG).w("插件 $pluginId 未加载，无需关闭")
                return true
            }

            // 获取插件信息（在移除前获取，用于清理缓存）
            val loadedPlugin = loadedPlugins[pluginId]

            // 从Koin中移除插件实例
            pluginInstances[pluginId]?.let { unloadPluginKoinModules(pluginId, it) }

            // 从 PluginResourcesManager 中移除插件资源
            // 这将同时处理资源的卸载和生命周期管理
            resourcesManager.removePluginResources(pluginId)

            // 清理运行时缓存文件
            cleanupRuntimeCacheFile(pluginId)

            // 移除插件相关缓存
            loadedPlugins.remove(pluginId)
            pluginInstances.remove(pluginId)

            Timber.tag(TAG).i("插件关闭成功: $pluginId（包括运行时缓存清理）")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "关闭插件 $pluginId 时发生错误: ${e.message}")
            false
        }
    }

    /**
     * 异步重新启动指定插件（用于插件版本更新后重新加载）
     *
     * @param pluginId 插件ID
     * @return 是否重启成功
     */
    suspend fun relaunchPlugin(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).i("开始异步重新启动插件: $pluginId")

            // 先关闭插件
            closePlugin(pluginId)

            // 使用协程延迟替代Thread.sleep，确保资源完全释放
            delay(100)

            // 重新启动插件
            val result = launchPlugin(pluginId)

            if (result) {
                Timber.tag(TAG).i("插件异步重启成功: $pluginId")
            } else {
                Timber.tag(TAG).e("插件异步重启失败: $pluginId")
            }

            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "异步重启插件 $pluginId 时发生错误: ${e.message}")
            false
        }
    }

    /**
     * 获取插件入口类的实例化对象（直接从本地缓存获取）
     *
     * @param pluginId 插件ID
     * @return 插件入口类实例，如果未找到则返回null
     */
    fun getPluginEntryInstance(pluginId: String): IPluginEntryClass? {
        return try {
            // 直接从本地缓存获取插件实例
            val instance = pluginInstances[pluginId]
            if (instance != null) {
                Timber.tag(TAG).d("成功获取插件实例: $pluginId")
                return instance
            }

            // 检查插件是否已加载但未实例化
            if (loadedPlugins.containsKey(pluginId)) {
                Timber.tag(TAG).w("插件 $pluginId 已加载但未实例化，尝试重新实例化")
                val loadedPlugin = loadedPlugins[pluginId]
                if (loadedPlugin != null) {
                    val newInstance = instantiateSinglePlugin(loadedPlugin)
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

    fun getLoadedPluginInfo(pluginId: String): LoadedPluginInfo? {
        return loadedPlugins[pluginId]
    }

    /**
     * 异步启动所有已启用的插件（并发执行，效率更高）
     *
     * @return 成功启动的插件数量
     */
    suspend fun launchAllPlugins(): Int = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).i("开始异步启动所有插件...")

            val enabledPlugins = getEnabledPlugins()
            if (enabledPlugins.isEmpty()) {
                Timber.tag(TAG).i("没有找到已启用的插件")
                return@withContext 0
            }

            // 并发启动所有插件
            val launchJobs = enabledPlugins.map { plugin ->
                async {
                    launchPlugin(plugin.pluginId)
                }
            }

            val results = launchJobs.awaitAll()
            val successCount = results.count { it }

            Timber.tag(TAG).i("插件异步启动完成，成功启动 $successCount/${enabledPlugins.size} 个插件")
            successCount
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "异步启动所有插件时发生错误: ${e.message}")
            0
        }
    }

    /**
     * 关闭所有已加载的插件（清理所有运行时缓存）
     *
     * @return 成功关闭的插件数量
     */
    fun closeAllPlugins(): Int {
        return try {
            Timber.tag(TAG).i("开始关闭所有插件...")

            val loadedPluginIds = loadedPlugins.keys.toList()
            var successCount = 0

            for (pluginId in loadedPluginIds) {
                if (closePlugin(pluginId)) { // closePlugin 方法已经包含缓存清理
                    successCount++
                }
            }

            Timber.tag(TAG).i("插件关闭完成，成功关闭 $successCount/${loadedPluginIds.size} 个插件（包括运行时缓存清理）")

            // 验证所有运行时缓存已清理
            if (successCount == loadedPluginIds.size) {
                Timber.tag(TAG).d("所有插件运行时缓存已清理")
            }

            successCount
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "关闭所有插件时发生错误: ${e.message}")
            0
        }
    }

    /**
     * 检查插件是否已加载
     *
     * @param pluginId 插件ID
     * @return 是否已加载
     */
    fun isPluginLoaded(pluginId: String): Boolean {
        return loadedPlugins.containsKey(pluginId)
    }

    /**
     * 获取已加载插件的数量
     *
     * @return 已加载插件数量
     */
    fun getLoadedPluginCount(): Int {
        return loadedPlugins.size
    }

    /**
     * 获取所有已加载的插件ID列表
     *
     * @return 插件ID列表
     */
    fun getLoadedPluginIds(): List<String> {
        return loadedPlugins.keys.toList()
    }

    /**
     * 获取所有插件实例
     *
     * @return 插件实例映射表
     */
    fun getAllPluginInstances(): Map<String, IPluginEntryClass> {
        return pluginInstances.toMap()
    }

    /**
     * 获取插件资源管理器实例
     *
     * @return PluginResourcesManager 实例
     */
    fun getResourcesManager(): PluginResourcesManager {
        return resourcesManager
    }

    /**
     * 等待插件管理器初始化完成后执行回调
     * 如果已经初始化完成，则立即执行回调
     *
     * @param callback 初始化完成后要执行的回调函数
     */
    fun onInitialized(callback: () -> Unit) {
        synchronized(initializationLock) {
            if (isInitialized) {
                // 已经初始化完成，立即执行回调
                try {
                    callback()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "执行初始化回调时发生错误")
                }
            } else {
                // 还未初始化完成，添加到等待列表
                initializationCallbacks.add(callback)
            }
        }
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
    private suspend fun loadSinglePlugin(plugin: PluginInfo): LoadedPluginInfo? = withContext(Dispatchers.IO) {
        try {
            // 创建运行时缓存文件（内部会检查原文件存在性）
            val runtimeCacheFile = createRuntimeCacheFile(plugin.pluginId)
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
    private fun instantiateSinglePlugin(loadedPlugin: LoadedPluginInfo): IPluginEntryClass? {
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
    private fun loadPluginKoinModules(pluginId: String, instance: IPluginEntryClass) {
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
    private fun unloadPluginKoinModules(pluginId: String, instance: IPluginEntryClass) {
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
    private suspend fun createRuntimeCacheFile(pluginId: String): File? = withContext(Dispatchers.IO) {
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
    private fun cleanupRuntimeCacheFile(pluginId: String) {
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

    // ========== 插件管理方法 ==========

    /**
     * 安装插件
     *
     * @param pluginApkFile 插件APK文件
     * @return 安装是否成功
     */
    suspend fun installPlugin(pluginApkFile: File): Boolean = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("开始异步安装插件: ${pluginApkFile.name}")
        val result = installerManager.installPlugin(pluginApkFile)
        result is InstallResult.Success
    }

    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     */
    fun uninstallPlugin(pluginId: String): Boolean {
        Timber.tag(TAG).i("开始卸载插件: $pluginId")

        // 如果插件正在运行，先关闭它
        if (isPluginLoaded(pluginId)) {
            Timber.tag(TAG).d("插件正在运行，先关闭插件: $pluginId")
            closePlugin(pluginId)
        }

        return installerManager.uninstallPlugin(pluginId)
    }

    /**
     * 更新插件
     *
     * @param pluginApkFile 新版本插件APK文件
     * @return 更新是否成功
     */
    suspend fun updatePlugin(pluginApkFile: File): Boolean = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("开始异步更新插件: ${pluginApkFile.name}")
        val result = installerManager.updatePlugin(pluginApkFile)
        result is InstallResult.Success
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
        loadedPlugins.forEach { (loadedPluginId, loadedPluginInfo) ->
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
