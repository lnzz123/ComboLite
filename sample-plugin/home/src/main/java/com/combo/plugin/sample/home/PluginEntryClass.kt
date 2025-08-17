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

package com.combo.plugin.sample.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.combo.core.interfaces.IPluginEntryClass
import com.combo.plugin.sample.common.navigation.IHubComposeNavigator
import com.combo.plugin.sample.common.navigation.LocalComposeNavigator
import com.combo.plugin.sample.home.di.diModule
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
class PluginEntryClass : IPluginEntryClass {
    override val pluginModule: List<Module>
        get() =
            listOf(
                diModule,
            )

    @Composable
    override fun Content() {
        ComposeContent()
    }
}

@Composable
fun ComposeContent() {
    val composeNavigator: IHubComposeNavigator by inject(
        clazz = IHubComposeNavigator::class.java,
    )

    CompositionLocalProvider(
        LocalComposeNavigator provides composeNavigator,
    ) {
        AppMain(composeNavigator = composeNavigator)
    }
}
