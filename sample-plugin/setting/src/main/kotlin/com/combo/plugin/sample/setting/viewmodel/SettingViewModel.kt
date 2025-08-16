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