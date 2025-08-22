/*
 *
 *  * Copyright (c) 2025, 贵州君城网络科技有限公司
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.combo.plugin.sample.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.combo.core.manager.PluginManager
import com.combo.plugin.sample.common.viewmodel.BaseViewModel
import com.combo.plugin.sample.home.state.HomeState
import com.combo.plugin.sample.home.state.PluginStatus
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel<HomeState>(
    initialState = HomeState()
) {
    companion object {
        const val PLUGIN_GUIDE = "guide"
        const val PLUGIN_EXAMPLE = "example"
        const val PLUGIN_SETTING = "setting"
    }

    init {
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
     * 获取指定插件的状态
     *
     * @param pluginId 插件ID
     * @return 插件状态枚举
     */
    fun getPluginStatus(pluginId: String): PluginStatus {
        // 检查插件是否已安装
        val isInstalled = uiState.value.installedPlugins.any { it.pluginId == pluginId }

        if (! isInstalled) {
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
