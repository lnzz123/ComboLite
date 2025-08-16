package com.combo.plugin.sample.example.state

import com.combo.core.interfaces.IPluginEntryClass
import com.combo.core.manager.PluginManager
import com.combo.core.model.PluginInfo
import com.combo.plugin.sample.common.viewmodel.BaseUiState

data class ExampleState(
    var plugins: Map<String, PluginManager.LoadedPluginInfo> = emptyMap(),
    var pluginEntryClasses: Map<String, IPluginEntryClass> = emptyMap(),
    var installedPlugins: List<PluginInfo> = emptyList(),
    val guideEntryClass: IPluginEntryClass? = null,
    val exampleEntryClass: IPluginEntryClass? = null,
    val settingEntryClass: IPluginEntryClass? = null,
    override val isLoading: Boolean = true,
    override val isError: Boolean = false,
    override val errorMessage: String? = null,
) : BaseUiState
