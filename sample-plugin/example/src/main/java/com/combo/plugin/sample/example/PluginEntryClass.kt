/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利.
 */

package com.combo.plugin.sample.example

import androidx.compose.runtime.Composable
import com.combo.core.interfaces.IPluginEntryClass
import com.combo.plugin.sample.example.di.diModule
import com.combo.plugin.sample.example.screen.ExampleMainScreen
import org.koin.core.module.Module

class PluginEntryClass : IPluginEntryClass {
    override val pluginModule: List<Module>
        get() = listOf(
            diModule
        )

    @Composable
    override fun Content() {
        ExampleMainScreen()
    }
}
