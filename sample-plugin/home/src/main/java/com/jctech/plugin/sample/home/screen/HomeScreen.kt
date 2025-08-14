package com.jctech.plugin.sample.home.screen

import InstallUtils
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.jctech.plugin.core.installer.InstallerManager
import com.jctech.plugin.core.manager.PluginManager
import com.jctech.plugin.sample.common.component.EmptyPage
import com.jctech.plugin.sample.home.state.PluginStatus
import com.jctech.plugin.sample.home.viewmodel.HomeViewModel
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
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> {
                val pluginStatus = viewModel.getPluginStatus(HomeViewModel.PLUGIN_GUIDE)
                EmptyPage(
                    entryClass = state.explainEntryClass,
                    message = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "插件[${HomeViewModel.PLUGIN_GUIDE}]未安装"
                        PluginStatus.INSTALLED_NOT_STARTED -> "插件[${HomeViewModel.PLUGIN_GUIDE}]未启动"
                        PluginStatus.INSTALLED_AND_STARTED -> "插件[${HomeViewModel.PLUGIN_GUIDE}]已启动"
                    },
                    buttonText = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "安装插件"
                        PluginStatus.INSTALLED_NOT_STARTED -> "启动插件"
                        PluginStatus.INSTALLED_AND_STARTED -> "进入插件"
                    },
                    onButtonClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            val result = InstallUtils.installPluginFromAssets(context, "plugins/other/guide-release.apk")
                            if (result is InstallerManager.InstallResult.Success) {
                                PluginManager.launchPlugin(result.pluginInfo.pluginId)
                            } else {
                                // 安装失败
                                Toast.makeText(context, "安装失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            AppDestinations.SAMPLE -> {
                val pluginStatus = viewModel.getPluginStatus(HomeViewModel.PLUGIN_EXAMPLE)
                EmptyPage(
                    entryClass = state.sampleEntryClass,
                    message = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "插件[${HomeViewModel.PLUGIN_EXAMPLE}]未安装"
                        PluginStatus.INSTALLED_NOT_STARTED -> "插件[${HomeViewModel.PLUGIN_EXAMPLE}]未启动"
                        PluginStatus.INSTALLED_AND_STARTED -> "插件[${HomeViewModel.PLUGIN_EXAMPLE}]已启动"
                    },
                    buttonText = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "安装插件"
                        PluginStatus.INSTALLED_NOT_STARTED -> "启动插件"
                        PluginStatus.INSTALLED_AND_STARTED -> "进入插件"
                    },
                    onButtonClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            val result = InstallUtils.installPluginFromAssets(context, "plugins/other/example-release.apk")
                            if (result is InstallerManager.InstallResult.Success) {
                                PluginManager.launchPlugin(result.pluginInfo.pluginId)
                            } else {
                                // 安装失败
                                Toast.makeText(context, "安装失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            AppDestinations.SETTING -> {
                val pluginStatus = viewModel.getPluginStatus(HomeViewModel.PLUGIN_SETTING)
                EmptyPage(
                    entryClass = state.settingEntryClass,
                    message = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "插件[${HomeViewModel.PLUGIN_SETTING}]未安装"
                        PluginStatus.INSTALLED_NOT_STARTED -> "插件[${HomeViewModel.PLUGIN_SETTING}]未启动"
                        PluginStatus.INSTALLED_AND_STARTED -> "插件[${HomeViewModel.PLUGIN_SETTING}]已启动"
                    },
                    buttonText = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "安装插件"
                        PluginStatus.INSTALLED_NOT_STARTED -> "启动插件"
                        PluginStatus.INSTALLED_AND_STARTED -> "进入插件"
                    },
                    onButtonClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            val result = InstallUtils.installPluginFromAssets(context, "plugins/other/setting-release.apk")
                            if (result is InstallerManager.InstallResult.Success) {
                                PluginManager.launchPlugin(result.pluginInfo.pluginId)
                            } else {
                                // 安装失败
                                Toast.makeText(context, "安装失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
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
