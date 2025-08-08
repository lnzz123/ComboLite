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

package com.jctech.plugin.core.resources

import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.File

/**
 * 插件资源加载器工具类 (Android 11+)
 *
 * 纯工具类，仅负责创建 ResourcesLoader 实例
 * 不管理资源的添加和卸载，这些操作由 PluginResourcesManager 负责
 *
 * 职责：
 * - 从插件文件创建 ResourcesLoader
 * - 处理资源提供器的配置
 * - 不涉及资源的生命周期管理
 */
object PluginResourcesLoader {

    private const val TAG = "PluginResourcesLoader"

    /**
     * 从插件文件创建 ResourcesLoader
     *
     * @param pluginFile 插件文件
     * @return ResourcesLoader 实例，创建失败时返回 null
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun loadPluginResources(pluginFile: File): ResourcesLoader? {
        return try {
            if (!pluginFile.exists()) {
                Timber.tag(TAG).e("插件文件未找到: ${pluginFile.path}")
                return null
            }

            val resourcesLoader = ResourcesLoader()

            try {
                val assetsProvider = PluginAssetsProvider(pluginFile)
                val parcelFd = ParcelFileDescriptor.open(pluginFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val resourcesProvider = ResourcesProvider.loadFromApk(parcelFd, assetsProvider)
                resourcesLoader.addProvider(resourcesProvider)
                Timber.tag(TAG).d("插件资源提供器配置成功: ${pluginFile.name}")
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "插件资源提供器配置失败: ${pluginFile.name}")
                return null
            }

            resourcesLoader
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "创建插件资源加载器失败: ${pluginFile.name}")
            null
        }
    }
}
