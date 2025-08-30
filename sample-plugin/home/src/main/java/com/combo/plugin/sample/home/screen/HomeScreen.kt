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

package com.combo.plugin.sample.home.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.combo.core.manager.PluginManager
import com.combo.plugin.sample.common.component.EmptyPage
import com.combo.plugin.sample.home.state.PluginStatus
import com.combo.plugin.sample.home.viewmodel.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * 主页屏幕
 *
 * 提供插件测试功能的主界面，包含导航、插件管理等功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    // 监听错误消息
    LaunchedEffect(state.isError, state.errorMessage) {
        if (state.isError && state.errorMessage != null) {
            Toast.makeText(context, state.errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label,
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },
                )
            }
        },
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> PluginScreenContent(
                pluginId = HomeViewModel.PLUGIN_GUIDE,
                viewModel = viewModel
            )

            AppDestinations.SAMPLE -> PluginScreenContent(
                pluginId = HomeViewModel.PLUGIN_EXAMPLE,
                viewModel = viewModel
            )

            AppDestinations.SETTING -> PluginScreenContent(
                pluginId = HomeViewModel.PLUGIN_SETTING,
                viewModel = viewModel
            )
        }
    }
}

/**
 * 插件页面的通用内容布局
 */
@Composable
private fun PluginScreenContent(pluginId: String, viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsState()

    val entryClass = when (pluginId) {
        HomeViewModel.PLUGIN_GUIDE -> state.guideEntryClass
        HomeViewModel.PLUGIN_EXAMPLE -> state.exampleEntryClass
        HomeViewModel.PLUGIN_SETTING -> state.settingEntryClass
        else -> null
    }

    when {
        // 1. 检查是否下载失败
        state.failedDownloads.contains(pluginId) -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("插件[$pluginId]下载失败！")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.retryDownload(pluginId) }) {
                    Text("重试")
                }
            }
        }

        // 2. 检查是否正在下载
        state.downloadingPlugins.containsKey(pluginId) -> {
            val progress = state.downloadingPlugins[pluginId] ?: 0f
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "下载中... ${(progress * 100).toInt()}%")
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(200.dp))
            }
        }

        // 3. 显示正常状态
        else -> {
            val pluginStatus = viewModel.getPluginStatus(pluginId)
            EmptyPage(
                entryClass = entryClass,
                message =
                    when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "插件[$pluginId]未安装"
                        PluginStatus.INSTALLED_NOT_STARTED -> "插件[$pluginId]未启动"
                        PluginStatus.INSTALLED_AND_STARTED -> "插件[$pluginId]已启动"
                    },
                buttonText =
                    when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "下载并安装最新插件"
                        PluginStatus.INSTALLED_NOT_STARTED -> "启动插件"
                        PluginStatus.INSTALLED_AND_STARTED -> "进入插件"
                    },
                onButtonClick = {
                    when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> {
                            viewModel.installLatestPlugin(pluginId)
                        }

                        else -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                PluginManager.launchPlugin(pluginId)
                            }
                        }
                    }
                },
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("首页", Icons.Default.Home),
    SAMPLE("示例", Icons.Default.Star),
    SETTING("设置", Icons.Default.Settings),
}