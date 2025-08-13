package com.jctech.plugin.sample.home.state

import com.jctech.plugin.core.interfaces.IPluginEntryClass
import com.jctech.plugin.core.manager.PluginManager
import com.jctech.plugin.core.model.PluginInfo
import com.jctech.plugin.sample.common.viewmodel.BaseUiState

/**
 * 插件状态枚举
 */
enum class PluginStatus {
    /** 插件未安装 */
    NOT_INSTALLED,

    /** 插件已安装但未启动 */
    INSTALLED_NOT_STARTED,

    /** 插件已安装且已启动 */
    INSTALLED_AND_STARTED
}

data class HomeState(
    var plugins: Map<String, PluginManager.LoadedPluginInfo> = emptyMap(),
    var pluginEntryClasses: Map<String, IPluginEntryClass> = emptyMap(),
    var installedPlugins: List<PluginInfo> = emptyList(),
    val explainEntryClass: IPluginEntryClass? = null,
    val sampleEntryClass: IPluginEntryClass? = null,
    val settingEntryClass: IPluginEntryClass? = null,
    override val isLoading: Boolean = true,
    override val isError: Boolean = false,
    override val errorMessage: String? = null,
) : BaseUiState