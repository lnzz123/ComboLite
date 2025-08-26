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

package com.combo.core.resources

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
                val parcelFd =
                    ParcelFileDescriptor.open(pluginFile, ParcelFileDescriptor.MODE_READ_ONLY)
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
