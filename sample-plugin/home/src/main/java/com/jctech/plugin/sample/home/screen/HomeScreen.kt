package com.jctech.plugin.sample.home.screen

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
import com.jctech.plugin.sample.common.component.EmptyPage
import com.jctech.plugin.sample.home.state.PluginStatus
import com.jctech.plugin.sample.home.viewmodel.HomeViewModel
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
                val pluginStatus = viewModel.getPluginStatus("example_screen")
                EmptyPage(
                    entryClass = state.explainEntryClass,
                    message = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "插件[explain]未安装"
                        PluginStatus.INSTALLED_NOT_STARTED -> "插件[explain]未启动"
                        PluginStatus.INSTALLED_AND_STARTED -> "插件[explain]已启动"
                    },
                    buttonText = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "安装插件"
                        PluginStatus.INSTALLED_NOT_STARTED -> "启动插件"
                        PluginStatus.INSTALLED_AND_STARTED -> "进入插件"
                    },
                    onButtonClick = { }
                )
            }

            AppDestinations.SAMPLE -> {
                val pluginStatus = viewModel.getPluginStatus("sample")
                EmptyPage(
                    entryClass = state.sampleEntryClass,
                    message = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "插件[sample]未安装"
                        PluginStatus.INSTALLED_NOT_STARTED -> "插件[sample]未启动"
                        PluginStatus.INSTALLED_AND_STARTED -> "插件[sample]已启动"
                    },
                    buttonText = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "安装插件"
                        PluginStatus.INSTALLED_NOT_STARTED -> "启动插件"
                        PluginStatus.INSTALLED_AND_STARTED -> "进入插件"
                    },
                    onButtonClick = { }
                )
            }

            AppDestinations.SETTING -> {
                val pluginStatus = viewModel.getPluginStatus("setting")
                EmptyPage(
                    entryClass = state.settingEntryClass,
                    message = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "插件[setting]未安装"
                        PluginStatus.INSTALLED_NOT_STARTED -> "插件[setting]未启动"
                        PluginStatus.INSTALLED_AND_STARTED -> "插件[setting]已启动"
                    },
                    buttonText = when (pluginStatus) {
                        PluginStatus.NOT_INSTALLED -> "安装插件"
                        PluginStatus.INSTALLED_NOT_STARTED -> "启动插件"
                        PluginStatus.INSTALLED_AND_STARTED -> "进入插件"
                    },
                    onButtonClick = { }
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
