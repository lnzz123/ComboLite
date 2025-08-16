package com.combo.plugin.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.combo.plugin.sample.LoadingViewModel.Companion.PLUGIN_COMMON
import com.combo.plugin.sample.LoadingViewModel.Companion.PLUGIN_HOME
import org.koin.androidx.compose.koinViewModel

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
                        text = "基础插件${when {
                            homeState == PluginStatus.NOT_INSTALLED || commonState == PluginStatus.NOT_INSTALLED -> "未安装"
                            homeState == PluginStatus.INSTALLED_NOT_STARTED || commonState == PluginStatus.INSTALLED_NOT_STARTED -> "已安装但未启动"
                            else -> "已安装且已启动"
                        }}",
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
                        Text(text = when {
                            homeState == PluginStatus.NOT_INSTALLED || commonState == PluginStatus.NOT_INSTALLED -> "安装插件"
                            homeState == PluginStatus.INSTALLED_NOT_STARTED || commonState == PluginStatus.INSTALLED_NOT_STARTED -> "启动插件"
                            else -> "打开应用"
                        })
                    }
                }
            } else {
                entryClass?.Content()
            }
        }
    }
} 
