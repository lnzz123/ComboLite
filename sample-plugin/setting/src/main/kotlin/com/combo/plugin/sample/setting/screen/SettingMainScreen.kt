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

package com.combo.plugin.sample.setting.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.combo.core.manager.PluginManager
import com.combo.plugin.sample.setting.component.InfoCard
import com.combo.plugin.sample.setting.component.PluginCard
import com.combo.plugin.sample.setting.viewmodel.SettingViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * 插件设置主界面
 * 展示已安装的插件列表，并提供管理选项
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SettingMainScreen(
    viewModel: SettingViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 从 PluginManager 收集已加载插件的 Flow，以实时获取运行状态
    val loadedPlugins by PluginManager.loadedPluginsFlow.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = { viewModel.refreshPlugins() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插件管理", fontWeight = FontWeight.Bold) },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 顶部信息卡片
                item {
                    InfoCard(pluginCount = state.installedPlugins.size) {
                        viewModel.refreshPlugins()
                        Toast.makeText(context, "刷新插件列表", Toast.LENGTH_SHORT).show()
                    }
                }

                // 插件列表
                items(state.installedPlugins, key = { it.pluginId }) { plugin ->
                    val isRunning = loadedPlugins.containsKey(plugin.pluginId)
                    PluginCard(
                        plugin = plugin,
                        isRunning = isRunning,
                        onEnableChange = { isEnabled ->
                            viewModel.setPluginEnabled(plugin.pluginId, isEnabled)
                        },
                        onLaunch = {
                            viewModel.viewModelScope.launch {
                                PluginManager.launchPlugin(plugin.pluginId)
                            }
                        },
                        onClose = {
                            viewModel.viewModelScope.launch {
                                PluginManager.unloadPlugin(plugin.pluginId)
                            }
                        },
                        onUninstall = {
                            viewModel.uninstallPlugin(plugin.pluginId)
                        }
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = state.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}