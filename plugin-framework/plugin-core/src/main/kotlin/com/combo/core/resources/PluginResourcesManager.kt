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

package com.combo.core.resources

import android.annotation.SuppressLint
import android.app.Application
import android.content.res.AssetManager
import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 插件资源管理器
 *
 * 专门用于管理插件的全局资源，解决 Android 系统 Resources 缓存机制问题
 *
 * 核心设计：
 * 1. 维护一个合并了所有插件资源的宿主应用 Resources 实例
 * 2. 根据 Android 版本使用不同的资源加载策略：
 *    - Android 11+: 使用官方 ResourcesLoader API
 *    - Android 11以下: 使用 AssetManager.addAssetPath 反射API
 * 3. 绕过系统资源缓存机制，确保实时资源访问
 * 4. 支持动态资源更新和版本控制
 *
 * 职责：
 * - 管理所有插件资源的合并和生命周期
 * - 提供统一的资源访问接口
 * - 处理不同 Android 版本的兼容性
 * - 维护资源版本和缓存机制
 */
class PluginResourcesManager(
    private val context: Application,
) {
    companion object {
        private const val TAG = "PluginResourcesManager"
    }

    // 存储已加载的插件文件
    private val loadedPluginFiles = ConcurrentHashMap<String, File>()

    // Android 11+ 使用的 ResourcesLoader 映射表（pluginId -> ResourcesLoader）
    private val resourcesLoaderMap = ConcurrentHashMap<String, ResourcesLoader>()

    // 当前合并的资源实例（默认为应用的 resources，添加插件后为合并资源）
    private val _mResources = MutableStateFlow(context.resources)
    val mResourcesFlow: StateFlow<Resources> = _mResources.asStateFlow()

    /**
     * 获取当前合并后的资源实例
     * 这个方法会被 BaseComposeActivity.getResources() 调用
     * 绕过系统缓存机制，始终返回包含所有插件资源的最新实例
     */
    fun getResources(): Resources = _mResources.value

    /**
     * 从插件文件加载并添加资源到宿主应用
     * @param pluginId 插件ID
     * @param pluginFile 插件文件
     * @return 是否加载成功
     */
    fun loadPluginResources(
        pluginId: String,
        pluginFile: File,
    ): Boolean {
        return try {
            Timber.tag(TAG).d("开始加载插件资源: $pluginId (Android ${Build.VERSION.SDK_INT})")

            // 检查插件文件是否存在
            if (!pluginFile.exists()) {
                Timber.tag(TAG).e("插件文件不存在: ${pluginFile.absolutePath}")
                return false
            }

            // 根据 Android 版本选择不同的资源加载策略
            val success =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+: 使用官方 ResourcesLoader AP
                    loadResourcesWithResourcesLoader(pluginId, pluginFile)
                } else {
                    // Android11以下: 使用 AssetManager.addAssetPath 反射API
                    loadResourcesWithAddAssetPath(pluginId, pluginFile)
                }

            if (success) {
                // 记录插件文件
                loadedPluginFiles[pluginId] = pluginFile
                Timber.tag(TAG).i("插件资源加载成功: $pluginId")
            } else {
                Timber.tag(TAG).w("插件资源加载失败: $pluginId")
            }

            success
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "加载插件资源异常: $pluginId")
            false
        }
    }

    /**
     * Android 11+ 使用 ResourcesLoader API 加载资源
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadResourcesWithResourcesLoader(
        pluginId: String,
        pluginFile: File,
    ): Boolean {
        return try {
            Timber.tag(TAG).d("使用 ResourcesLoader API 加载资源: $pluginId")

            // 使用工具类创建 ResourcesLoader
            val resourcesLoader = PluginResourcesLoader.loadPluginResources(pluginFile)
            if (resourcesLoader == null) {
                Timber.tag(TAG).e("创建 ResourcesLoader 失败: $pluginId")
                return false
            }

            // 添加到宿主应用的资源中
            _mResources.value.addLoaders(resourcesLoader)

            // 保存 ResourcesLoader 引用到映射表
            resourcesLoaderMap[pluginId] = resourcesLoader

            Timber.tag(TAG).d("ResourcesLoader 已添加到系统资源: $pluginId")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "ResourcesLoader 加载失败: $pluginId")
            false
        }
    }

    /**
     * Android 11以下使用 AssetManager.addAssetPath 反射API加载资源
     */
    @SuppressLint("DiscouragedPrivateApi")
    private fun loadResourcesWithAddAssetPath(
        pluginId: String,
        pluginFile: File,
    ): Boolean {
        return try {
            Timber.tag(TAG).d("使用 addAssetPath 反射API加载资源: $pluginId")

            // 获取当前 AssetManager
            val assetManager = _mResources.value.assets

            // 使用反射调用 addAssetPath 方法
            val addAssetPathMethod =
                AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
            addAssetPathMethod.isAccessible = true
            val result = addAssetPathMethod.invoke(assetManager, pluginFile.absolutePath) as Int

            if (result == 0) {
                Timber.tag(TAG).e("addAssetPath 返回0，加载失败: $pluginId")
                return false
            }

            // 重新创建资源实例以包含新的资源
            val configuration = _mResources.value.configuration
            val displayMetrics = _mResources.value.displayMetrics
            @Suppress("DEPRECATION")
            _mResources.value = Resources(assetManager, displayMetrics, configuration)

            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "addAssetPath 反射加载失败: $pluginId")
            false
        }
    }

    /**
     * 移除插件资源
     * @param pluginId 插件ID
     */
    fun removePluginResources(pluginId: String) {
        try {
            Timber.tag(TAG).d("开始移除插件资源: $pluginId")

            // 检查插件是否已加载
            val removedFile = loadedPluginFiles.remove(pluginId)
            if (removedFile == null) {
                Timber.tag(TAG).d("插件资源不存在，无需移除: $pluginId")
                return
            }

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // Android 11+: 移除特定的 ResourcesLoader
                    removeResourcesLoaderForPlugin(pluginId)
                }

                else -> {
                    // Android 11以下: 需要重新构建整个 AssetManager
                    rebuildAllResourcesForLowerVersions()
                }
            }

            Timber.tag(TAG).i("插件资源移除成功: $pluginId")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "移除插件资源失败: $pluginId")
        }
    }

    /**
     * Android 11+ 移除特定插件的 ResourcesLoader
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun removeResourcesLoaderForPlugin(pluginId: String) {
        try {
            // 从映射表中移除特定插件的 ResourcesLoader
            val resourcesLoader = resourcesLoaderMap.remove(pluginId)
            if (resourcesLoader != null) {
                // 从系统资源中移除这个 ResourcesLoader
                _mResources.value.removeLoaders(resourcesLoader)
                Timber.tag(TAG).d("ResourcesLoader 已从系统资源中移除: $pluginId")
            } else {
                Timber.tag(TAG).w("未找到插件对应的 ResourcesLoader: $pluginId")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "移除 ResourcesLoader 失败: $pluginId")
        }
    }

    /**
     * Android 11以下版本重新构建所有资源
     * 由于反射API的限制，需要重新创建 AssetManager 和 Resources
     */
    @SuppressLint("DiscouragedPrivateApi")
    private fun rebuildAllResourcesForLowerVersions() {
        try {
            Timber.tag(TAG).d("重新构建所有插件资源（Android 11以下）")

            // 重新创建 AssetManager 和 Resources 实例
            // 从原始应用资源开始
            _mResources.value = context.resources

            // 重新加载所有剩余的插件资源
            val remainingPlugins = loadedPluginFiles.toMap()

            remainingPlugins.forEach { (pluginId, pluginFile) ->
                if (pluginFile.exists()) {
                    loadResourcesWithAddAssetPath(pluginId, pluginFile)
                } else {
                    Timber.tag(TAG).w("插件文件不存在，从记录中移除: $pluginId")
                    loadedPluginFiles.remove(pluginId)
                }
            }

            Timber.tag(TAG).d("资源重建完成，剩余 ${loadedPluginFiles.size} 个插件")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "重新构建资源失败")
            // 发生错误时，重置为原始应用资源
            _mResources.value = context.resources
        }
    }

    /**
     * 刷新所有资源缓存
     * 重新构建当前的资源实例
     */
    fun refreshAllResources() {
        try {
            Timber.tag(TAG).d("开始刷新所有资源缓存")

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // Android 11+: ResourcesLoader 已经自动刷新，无需额外操作
                    Timber.tag(TAG).d("Android 11+ 资源自动更新")
                }

                else -> {
                    // Android 11以下: 重新构建资源
                    rebuildAllResourcesForLowerVersions()
                }
            }

            Timber.tag(TAG).i("资源缓存刷新完成")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "刷新资源缓存失败")
        }
    }

    /**
     * 更新插件资源
     * @param pluginId 插件ID
     * @param pluginFile 新的插件文件
     * @return 是否更新成功
     */
    fun updatePluginResources(
        pluginId: String,
        pluginFile: File,
    ): Boolean =
        try {
            Timber.tag(TAG).d("更新插件资源: $pluginId")

            // 先移除旧资源，再加载新资源
            removePluginResources(pluginId)
            val success = loadPluginResources(pluginId, pluginFile)

            if (success) {
                Timber.tag(TAG).i("插件资源更新成功: $pluginId")
            } else {
                Timber.tag(TAG).w("插件资源更新失败: $pluginId")
            }

            success
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "更新插件资源失败: $pluginId")
            false
        }

    /**
     * 获取已加载的插件ID列表
     */
    fun getLoadedPluginIds(): Set<String> = loadedPluginFiles.keys.toSet()

    /**
     * 清理所有插件资源
     */
    fun clearAllResources() {
        try {
            Timber.tag(TAG).d("开始清理所有插件资源")

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // Android 11+: 移除所有 ResourcesLoader
                    resourcesLoaderMap.values.forEach { loader ->
                        _mResources.value.removeLoaders(loader)
                    }
                    resourcesLoaderMap.clear()
                }

                else -> {
                    // Android 11以下: 重置为原始应用资源
                    _mResources.value = context.resources
                    Timber.tag(TAG).d("资源已重置为原始应用资源")
                }
            }

            // 清空所有状态
            loadedPluginFiles.clear()

            Timber.tag(TAG).i("所有插件资源清理完成")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "清理插件资源失败")
            // 发生错误时，强制重置为原始资源
            _mResources.value = context.resources
            loadedPluginFiles.clear()
            resourcesLoaderMap.clear()
        }
    }
}
