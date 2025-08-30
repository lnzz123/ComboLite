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

package com.combo.plugin.sample

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.combo.core.interfaces.IPluginEntryClass
import com.combo.core.manager.PluginManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 插件状态枚举
 */
enum class PluginStatus {
    /** 插件未安装 */
    NOT_INSTALLED,

    /** 插件已安装但未启动 */
    INSTALLED_NOT_STARTED,

    /** 插件已安装且已启动 */
    INSTALLED_AND_STARTED,
}


class LoadingViewModel(
    context: Context,
) : ViewModel() {
    @SuppressLint("StaticFieldLeak")
    private val context = context.applicationContext

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _entryClass = MutableStateFlow<IPluginEntryClass?>(null)
    val entryClass: StateFlow<IPluginEntryClass?> = _entryClass.asStateFlow()

    companion object {
        const val BASE_PATH = "plugins"
        const val PLUGIN_COMMON = "common"
        const val PLUGIN_HOME = "home"
    }

    init {
        init()
    }

    fun init() {
        viewModelScope.launch {
            setLoading(true)
            if (getPluginStatus(PLUGIN_HOME) == PluginStatus.NOT_INSTALLED || getPluginStatus(
                    PLUGIN_COMMON
                ) == PluginStatus.NOT_INSTALLED
            ) {
                installPlugin(BASE_PATH)
            } else {
                PluginManager.loadEnabledPlugins()
            }
            _entryClass.value = PluginManager.getPluginInstance(PLUGIN_HOME)
            setLoading(false)
        }
    }

    fun setLoading(isLoading: Boolean) {
        _loading.value = isLoading
    }

    fun installPlugin(
        assetPath: String,
        forceOverwrite: Boolean = false,
    ) {
        viewModelScope.launch {
            setLoading(true)
            val pluginFiles = context.assets.list(assetPath)
            pluginFiles?.forEach { fileName ->
                val pluginFile = File(context.filesDir, fileName)
                context.assets.open("$assetPath/$fileName").use { inputStream ->
                    FileOutputStream(pluginFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                PluginManager.installerManager.installPlugin(pluginFile, forceOverwrite)
            }
            PluginManager.loadEnabledPlugins()
            _entryClass.value = PluginManager.getPluginInstance(PLUGIN_HOME)
            setLoading(false)
        }
    }

    fun launchBasePlugin() {
        viewModelScope.launch {
            PluginManager.launchPlugin(PLUGIN_COMMON).let {
                if (it) {
                    PluginManager.getPluginInstance(PLUGIN_COMMON)
                }
            }
            PluginManager.launchPlugin(PLUGIN_HOME).let {
                if (it) {
                    _entryClass.value = PluginManager.getPluginInstance(PLUGIN_HOME)
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
        val isInstalled = PluginManager.getAllInstallPlugins().any { it.pluginId == pluginId }

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
