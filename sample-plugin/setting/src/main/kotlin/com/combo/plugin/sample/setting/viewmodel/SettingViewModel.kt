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

package com.combo.plugin.sample.setting.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.combo.core.manager.PluginManager
import com.combo.plugin.sample.common.viewmodel.BaseViewModel
import com.combo.plugin.sample.setting.state.SettingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
class SettingViewModel(
    private val context: Context
) : BaseViewModel<SettingState>(
    initialState = SettingState()
) {

    init {
        updateInstalledPlugins()
    }

    fun updateInstalledPlugins() {
        try {
            val installedPlugins = PluginManager.getAllInstallPlugins()
            updateState {
                copy(
                    installedPlugins = installedPlugins,
                )
            }
        } catch (e: Exception) {
            Timber.e("刷新插件列表失败:${e.message}")
        }
    }

    fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        PluginManager.setPluginEnabled(pluginId, enabled)
        updateInstalledPlugins()
    }

    fun uninstallPlugin(pluginId: String) {
        val result = PluginManager.installerManager.uninstallPlugin(pluginId)
        if (result) {
            Toast.makeText(
                context,
                "插件[$pluginId]卸载成功",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "插件[$pluginId]卸载失败",
                Toast.LENGTH_SHORT
            ).show()
        }
        updateInstalledPlugins()
    }

    fun refreshPlugins() {
        viewModelScope.launch {
            updateState {
                copy(
                    isLoading = true,
                )
            }
            updateInstalledPlugins()
            delay(1000)
            updateState {
                copy(
                    isLoading = false,
                )
            }
        }
    }
}