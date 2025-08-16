package com.combo.plugin.sample.setting.state

import com.combo.core.model.PluginInfo
import com.combo.plugin.sample.common.viewmodel.BaseUiState

data class SettingState(
    var installedPlugins: List<PluginInfo> = emptyList(),
    override val isLoading: Boolean = false,
    override val isError: Boolean = false,
    override val errorMessage: String? = null,
) : BaseUiState
