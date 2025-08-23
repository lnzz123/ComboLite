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

package com.combo.plugin.sample.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.combo.core.interfaces.IPluginEntryClass
import com.combo.core.model.PluginContext
import com.combo.plugin.sample.example.di.diModule
import com.combo.plugin.sample.example.receiver.NotificationUtil
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

    override fun onLoad(context: PluginContext) {
        NotificationUtil.createChannels(context.application)
    }

    override fun onUnload() {
    }
}
