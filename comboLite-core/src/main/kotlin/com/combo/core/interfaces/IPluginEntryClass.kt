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

package com.combo.core.interfaces

import androidx.compose.runtime.Composable
import org.koin.core.module.Module

/**
 * 插件配置接口
 * @property pluginModule 插件依赖注入模块
 * @property Content 插件主界面
 */
interface IPluginEntryClass {
    val pluginModule: List<Module>

    @Composable
    fun Content()
}
