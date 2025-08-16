package com.combo.plugin.sample.setting.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.combo.plugin.sample.setting.component.PluginCard
import com.combo.plugin.sample.setting.viewmodel.SettingViewModel
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
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = { viewModel.refreshPlugins() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "插件管理",
                        fontWeight = FontWeight.Bold,
                    )
                },
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
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(
                            onClick = {
                                viewModel.refreshPlugins()
                                Toast.makeText(
                                    context,
                                    "刷新插件列表:${state.installedPlugins}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "信息",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "当前共安装了 ${state.installedPlugins.size} 个插件。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // 插件列表
                items(state.installedPlugins) { plugin ->
                    PluginCard(
                        plugin = plugin,
                        onEnableChange = {
                            viewModel.setPluginEnabled(plugin.pluginId, it)
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