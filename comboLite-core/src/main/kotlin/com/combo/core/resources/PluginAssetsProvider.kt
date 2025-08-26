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

import android.content.res.AssetFileDescriptor
import android.content.res.loader.AssetsProvider
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * 插件 AssetsProvider 实现
 *
 * 为 ResourcesProvider 提供访问插件 assets 文件的能力
 */
@RequiresApi(Build.VERSION_CODES.R)
class PluginAssetsProvider(
    private val pluginFile: File,
) : AssetsProvider {
    override fun loadAssetFd(
        path: String,
        accessMode: Int,
    ): AssetFileDescriptor? =
        try {
            ZipFile(pluginFile).use { zipFile ->
                val assetPath = "assets/$path"
                val entry = zipFile.getEntry(assetPath)

                if (entry != null) {
                    // 将 assets 文件提取到临时文件
                    val tempFile =
                        File.createTempFile("plugin_assets_", "_${path.replace("/", "_")}")
                    tempFile.deleteOnExit()

                    zipFile.getInputStream(entry).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 使用正确的 API 创建 AssetFileDescriptor
                    val parcelFd =
                        ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    AssetFileDescriptor(parcelFd, 0, entry.size)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "插件 Assets 资源加载失败: $path")
            null
        }
}
