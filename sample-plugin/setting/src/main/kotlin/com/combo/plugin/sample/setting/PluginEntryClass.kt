package com.combo.plugin.sample.setting

import androidx.compose.runtime.Composable
import com.combo.core.interfaces.IPluginEntryClass
import com.combo.plugin.sample.setting.di.diModule
import com.combo.plugin.sample.setting.screen.SettingMainScreen
import org.koin.core.module.Module

class PluginEntryClass : IPluginEntryClass {
    override val pluginModule: List<Module>
        get() = listOf(
            diModule
        )

    @Composable
    override fun Content() {
        SettingMainScreen()
    }
}