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

package com.combo.plugin.sample.example.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.combo.core.installer.InstallerManager
import com.combo.core.manager.PluginManager
import com.combo.plugin.sample.common.update.DownloadStatus
import com.combo.plugin.sample.common.update.UpdateManager
import com.combo.plugin.sample.common.update.model.PluginVersionInfo
import com.combo.plugin.sample.common.viewmodel.BaseViewModel
import com.combo.plugin.sample.example.state.PluginUpdateState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class PluginUpdateViewModel(
    private val application: Application,
    private val updateManager: UpdateManager
) : BaseViewModel<PluginUpdateState>(PluginUpdateState()) {

    init {
        fetchPlugins()
    }

    fun fetchPlugins() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            val plugins = updateManager.fetchRemotePlugins()
            updateState { copy(isLoading = false, remotePlugins = plugins) }
        }
    }

    fun downloadAndInstallPlugin(
        pluginId: String,
        pluginName: String,
        versionInfo: PluginVersionInfo
    ) {
        viewModelScope.launch {
            val downloadIdentifier = "$pluginId-${versionInfo.version}"
            updateState {
                copy(downloadingPlugins = downloadingPlugins + (downloadIdentifier to 0f))
            }

            updateManager.downloadPlugin(pluginName, versionInfo).collectLatest { status ->
                when (status) {
                    is DownloadStatus.InProgress -> {
                        updateState {
                            copy(downloadingPlugins = downloadingPlugins + (downloadIdentifier to status.progress))
                        }
                    }

                    is DownloadStatus.Success -> {
                        updateState {
                            copy(
                                downloadingPlugins = downloadingPlugins - downloadIdentifier,
                                installingPlugins = installingPlugins + downloadIdentifier
                            )
                        }
                        installPlugin(status.file, downloadIdentifier)
                    }

                    is DownloadStatus.Failure -> {
                        Toast.makeText(
                            application,
                            "下载失败: ${status.error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateState {
                            copy(
                                downloadingPlugins = downloadingPlugins - downloadIdentifier,
                                isError = true,
                                errorMessage = "Download failed: ${status.error.message}"
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun installPlugin(pluginFile: File, downloadIdentifier: String) {
        val result = PluginManager.installerManager.installPlugin(pluginFile, true)
        updateState { copy(installingPlugins = installingPlugins - downloadIdentifier) }
        when (result) {
            is InstallerManager.InstallResult.Success -> {
                Toast.makeText(
                    application,
                    "安装成功: ${result.pluginInfo.pluginId}",
                    Toast.LENGTH_SHORT
                ).show()
                PluginManager.launchPlugin(result.pluginInfo.pluginId)
            }

            is InstallerManager.InstallResult.Failure -> {
                Toast.makeText(
                    application,
                    "安装失败: ${result.reason}",
                    Toast.LENGTH_SHORT
                ).show()
                updateState {
                    copy(
                        isError = true,
                        errorMessage = "Install failed: ${result.reason}"
                    )
                }
            }
        }
    }
}