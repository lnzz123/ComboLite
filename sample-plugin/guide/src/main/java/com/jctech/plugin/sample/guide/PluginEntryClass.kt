/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利.
 */

package com.jctech.plugin.sample.guide

import androidx.compose.runtime.Composable
import com.jctech.plugin.core.interfaces.IPluginEntryClass
import org.koin.core.module.Module

class PluginEntryClass() : IPluginEntryClass {
    override val pluginModule: List<Module>
        get() = emptyList()

    @Composable
    override fun Content() {
        GuideMainScreen()
    }
}

