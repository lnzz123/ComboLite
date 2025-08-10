/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利.
 */

package com.jctech.plugin.sample.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jctech.plugin.core.interfaces.IPluginEntryClass
import com.jctech.plugin.sample.common.navigation.IHubComposeNavigator
import com.jctech.plugin.sample.common.navigation.LocalComposeNavigator
import com.jctech.plugin.sample.home.di.diModule
import org.koin.core.module.Module
import org.koin.java.KoinJavaComponent.inject

/**
 * Compose主插件实现
 *
 * 提供应用的主界面内容，是插件框架的核心插件。
 * 包含了应用的完整UI和导航逻辑。
 *
 * @author IHUB Plugin Framework
 * @since 2.0.0
 */
class PluginEntryClass() : IPluginEntryClass {
    override val pluginModule: List<Module>
        get() = listOf(
            diModule
        )

    @Composable
    override fun Content() {
        ComposeContent()
    }
}

@Composable
fun ComposeContent() {
    val composeNavigator: IHubComposeNavigator by inject(
        clazz = IHubComposeNavigator::class.java
    )

    CompositionLocalProvider(
        LocalComposeNavigator provides composeNavigator,
    ) {
        AppMain(composeNavigator = composeNavigator)
    }
}
