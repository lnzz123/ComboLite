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

package com.combo.plugin.sample.example.state

import com.combo.core.interfaces.IPluginEntryClass
import com.combo.core.manager.PluginManager
import com.combo.core.model.PluginInfo
import com.combo.plugin.sample.common.viewmodel.BaseUiState

data class ExampleState(
    var plugins: Map<String, PluginManager.LoadedPluginInfo> = emptyMap(),
    var pluginEntryClasses: Map<String, IPluginEntryClass> = emptyMap(),
    var installedPlugins: List<PluginInfo> = emptyList(),
    val guideEntryClass: IPluginEntryClass? = null,
    val exampleEntryClass: IPluginEntryClass? = null,
    val settingEntryClass: IPluginEntryClass? = null,
    override val isLoading: Boolean = true,
    override val isError: Boolean = false,
    override val errorMessage: String? = null,
) : BaseUiState
