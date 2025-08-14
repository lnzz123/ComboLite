package com.jctech.plugin.sample.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.jctech.plugin.core.manager.PluginManager
import com.jctech.plugin.sample.common.viewmodel.BaseViewModel
import com.jctech.plugin.sample.home.state.HomeState
import com.jctech.plugin.sample.home.state.PluginStatus
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class HomeViewModel() : BaseViewModel<HomeState>(
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
                PluginManager.pluginInstancesFlow
            ) { loadedPlugins, pluginInstances ->
                updateState {
                    copy(
                        plugins = loadedPlugins,
                        pluginEntryClasses = pluginInstances
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