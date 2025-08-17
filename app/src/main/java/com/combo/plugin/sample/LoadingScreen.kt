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

package com.combo.plugin.sample

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.combo.plugin.sample.LoadingViewModel.Companion.PLUGIN_COMMON
import com.combo.plugin.sample.LoadingViewModel.Companion.PLUGIN_HOME
import org.koin.androidx.compose.koinViewModel
import kotlin.jvm.java

/**
 * 加载页面
 *
 * 在插件框架初始化期间显示的加载界面
 */
@Composable
fun LoadingScreen(viewModel: LoadingViewModel = koinViewModel()) {
    val loading by viewModel.loading.collectAsState()
    val entryClass by viewModel.entryClass.collectAsState()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Text(
                        text = "正在初始化插件框架...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            } else if (entryClass == null) {
                val homeState = viewModel.getPluginStatus(PLUGIN_HOME)
                val commonState = viewModel.getPluginStatus(PLUGIN_COMMON)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "基础插件${
                            when {
                                homeState == PluginStatus.NOT_INSTALLED || commonState == PluginStatus.NOT_INSTALLED -> "未安装"
                                homeState == PluginStatus.INSTALLED_NOT_STARTED || commonState == PluginStatus.INSTALLED_NOT_STARTED -> "已安装但未启动"
                                else -> "已安装且已启动"
                            }
                        }",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Button(
                        onClick = {
                            when {
                                homeState == PluginStatus.NOT_INSTALLED || commonState == PluginStatus.NOT_INSTALLED -> {
                                    viewModel.installPlugin(LoadingViewModel.BASE_PATH, true)
                                }

                                homeState == PluginStatus.INSTALLED_NOT_STARTED || commonState == PluginStatus.INSTALLED_NOT_STARTED -> {
                                    viewModel.launchBasePlugin()
                                }

                                else -> {
                                    viewModel.launchBasePlugin()
                                }
                            }
                        },
                    ) {
                        Text(
                            text = when {
                                homeState == PluginStatus.NOT_INSTALLED || commonState == PluginStatus.NOT_INSTALLED -> "安装插件"
                                homeState == PluginStatus.INSTALLED_NOT_STARTED || commonState == PluginStatus.INSTALLED_NOT_STARTED -> "启动插件"
                                else -> "打开应用"
                            }
                        )
                    }
                }
            } else {
                entryClass?.Content()
            }
        }
    }
}