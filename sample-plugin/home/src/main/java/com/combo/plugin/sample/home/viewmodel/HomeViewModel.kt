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

package com.combo.plugin.sample.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.combo.core.installer.InstallerManager
import com.combo.core.manager.PluginManager
import com.combo.plugin.sample.common.update.DownloadStatus
import com.combo.plugin.sample.common.update.UpdateManager
import com.combo.plugin.sample.common.viewmodel.BaseViewModel
import com.combo.plugin.sample.home.state.HomeState
import com.combo.plugin.sample.home.state.PluginStatus
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel(
    private val updateManager: UpdateManager
) : BaseViewModel<HomeState>(
    initialState = HomeState()
) {
    companion object {
        const val PLUGIN_GUIDE = "guide"
        const val PLUGIN_EXAMPLE = "example"
        const val PLUGIN_SETTING = "setting"
    }

    init {
        // 刷新远程插件列表
        viewModelScope.launch {
            updateManager.fetchRemotePlugins()
        }

        // 监听插件状态
        viewModelScope.launch {
            combine(
                PluginManager.loadedPluginsFlow,
                PluginManager.pluginInstancesFlow,
            ) { loadedPlugins, pluginInstances ->
                updateState {
                    copy(
                        plugins = loadedPlugins,
                        pluginEntryClasses = pluginInstances,
                    )
                }
            }.collect {
                updateState {
                    copy(
                        installedPlugins = PluginManager.getAllInstallPlugins(),
                        guideEntryClass = PluginManager.getPluginInstance(PLUGIN_GUIDE),
                        exampleEntryClass = PluginManager.getPluginInstance(PLUGIN_EXAMPLE),
                        settingEntryClass = PluginManager.getPluginInstance(PLUGIN_SETTING),
                    )
                }
            }
        }
    }

    /**
     * 重试下载失败的插件
     * @param pluginId 插件ID
     */
    fun retryDownload(pluginId: String) {
        updateState {
            copy(failedDownloads = failedDownloads - pluginId)
        }
        installLatestPlugin(pluginId)
    }

    /**
     * 下载并安装最新版本的插件（支持并发和错误处理）
     * @param pluginId 插件ID
     */
    fun installLatestPlugin(pluginId: String) {
        if (uiState.value.downloadingPlugins.containsKey(pluginId)) {
            Timber.w("插件 '$pluginId' 已在下载中。")
            return
        }

        viewModelScope.launch {
            updateState {
                copy(downloadingPlugins = downloadingPlugins + (pluginId to 0f))
            }

            try {
                updateManager.downloadLatestPlugin(pluginId).collect { status ->
                    when (status) {
                        is DownloadStatus.InProgress -> {
                            updateState {
                                copy(downloadingPlugins = downloadingPlugins + (pluginId to status.progress))
                            }
                        }

                        is DownloadStatus.Success -> {
                            val installResult =
                                PluginManager.installerManager.installPlugin(status.file)
                            if (installResult is InstallerManager.InstallResult.Success) {
                                PluginManager.launchPlugin(pluginId)
                            } else {
                                Timber.e("插件安装失败: $pluginId")
                                updateState {
                                    copy(failedDownloads = failedDownloads + pluginId)
                                }
                            }
                            status.file.delete()
                        }

                        is DownloadStatus.Failure -> {
                            Timber.e(status.error, "下载插件失败: $pluginId")
                            updateState {
                                copy(failedDownloads = failedDownloads + pluginId)
                            }
                        }
                    }
                }
            } finally {
                // 任务结束，将插件从下载列表中移除
                updateState {
                    copy(downloadingPlugins = downloadingPlugins - pluginId)
                }
            }
        }
    }

    /**
     * 获取指定插件的状态
     *
     * @param pluginId 插件ID
     * @return 插件状态枚举
     */
    fun getPluginStatus(pluginId: String): PluginStatus {
        // 检查插件是否已安装
        val isInstalled = uiState.value.installedPlugins.any { it.pluginId == pluginId }

        if (!isInstalled) {
            return PluginStatus.NOT_INSTALLED
        }

        val entryClass = PluginManager.getPluginInstance(pluginId)

        return if (entryClass != null) {
            PluginStatus.INSTALLED_AND_STARTED
        } else {
            PluginStatus.INSTALLED_NOT_STARTED
        }
    }
}