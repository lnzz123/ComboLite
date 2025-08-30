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

package com.combo.plugin.sample.common.update

import android.content.Context
import com.combo.plugin.sample.common.update.model.PluginVersionInfo
import com.combo.plugin.sample.common.update.model.RemotePlugin
import com.combo.plugin.sample.common.update.net.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * 定义下载状态，用于Flow发射，使结果更清晰
 */
sealed class DownloadStatus {
    /**
     * 下载中
     * @param progress 下载进度，值为 0.0 到 1.0
     */
    data class InProgress(val progress: Float) : DownloadStatus()

    /**
     * 下载成功
     * @param file 下载完成的文件对象
     */
    data class Success(val file: File) : DownloadStatus()

    /**
     * 新增：下载失败状态
     * @param error 导致失败的异常
     */
    data class Failure(val error: Throwable) : DownloadStatus()
}

/**
 * 插件更新管理器 (重构版)
 * 负责从远程服务器检查版本、缓存插件信息和下载插件文件。
 */
class UpdateManager(
    private val context: Context,
    private val apiService: ApiService
) {

    // 使用独立的协程作用域，避免影响其他组件
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 缓存远程插件列表
    private var remotePlugins: List<RemotePlugin> = emptyList()

    init {
        scope.launch {
            fetchRemotePlugins()
        }
    }

    /**
     * 从远程服务器获取所有插件的最新信息，并更新内部缓存。
     */
    suspend fun fetchRemotePlugins(): List<RemotePlugin> {
        return try {
            val config = apiService.fetchPluginConfig()
            remotePlugins = config.plugins
            Timber.d("获取插件列表成功，共 ${remotePlugins.size} 个插件。")
            remotePlugins
        } catch (e: Exception) {
            Timber.e(e, "获取插件列表失败")
            emptyList()
        }
    }

    /**
     * 根据插件ID获取插件信息。
     */
    fun getPlugin(pluginId: String): RemotePlugin? {
        return remotePlugins.find { it.id == pluginId }
    }

    /**
     * 根据插件ID获取所有版本信息。
     */
    fun getAllVersions(pluginId: String): List<PluginVersionInfo>? {
        return getPlugin(pluginId)?.versions
    }

    /**
     * 根据插件ID获取最新版本信息。
     */
    fun getLatestVersion(pluginId: String): PluginVersionInfo? {
        return getAllVersions(pluginId)?.maxWithOrNull(::compareVersionNames)
    }

    /**
     * 比较两个版本名称字符串。
     */
    private fun compareVersionNames(v1: PluginVersionInfo, v2: PluginVersionInfo): Int {
        val parts1 = v1.version.split('.').map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.version.split('.').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(parts1.size, parts2.size)

        for (i in 0 until size) {
            val part1 = parts1.getOrElse(i) { 0 }
            val part2 = parts2.getOrElse(i) { 0 }
            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        return 0
    }

    /**
     * 下载指定插件的最新版本。
     */
    fun downloadLatestPlugin(pluginId: String): Flow<DownloadStatus> {
        val latestVersion = getLatestVersion(pluginId)
        val plugin = getPlugin(pluginId)
        return if (latestVersion != null && plugin != null) {
            downloadPlugin(plugin.name, latestVersion)
        } else {
            Timber.w("找不到插件 '$pluginId' 或其最新版本。")
            flow {
                emit(DownloadStatus.Failure(Exception("插件 '$pluginId' 或其最新版本不存在")))
            }
        }
    }

    /**
     * 下载指定的插件文件，不执行安装操作。
     */
    fun downloadPlugin(
        pluginName: String,
        versionInfo: PluginVersionInfo
    ): Flow<DownloadStatus> = flow {
        val destinationFile = File(context.cacheDir, "${pluginName}-${versionInfo.version}.apk")

        apiService.downloadFile(versionInfo.downloadUrl).use { body ->
            val totalBytes = body.contentLength()
            var bytesCopied = 0L

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            val progress = bytesCopied.toFloat() / totalBytes
                            emit(DownloadStatus.InProgress(progress))
                        }
                    }
                }
            }
        }
        emit(DownloadStatus.Success(destinationFile))
    }.catch { e ->
        emit(DownloadStatus.Failure(e))
    }.flowOn(Dispatchers.IO)
}