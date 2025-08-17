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

package com.combo.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PluginInfo(
    val pluginId: String,
    val version: String,
    val path: String,
    val entryClass: String,
    var description: String,
    val enabled: Boolean,
    val installTime: Long,
    val staticReceivers: List<StaticReceiverInfo> = emptyList(),
    val providers: List<ProviderInfo> = emptyList(),
)

@Serializable
data class MetaDataInfo(
    val name: String,
    val value: String?,
    val resource: Int?,
)

@Serializable
data class IntentFilterInfo(
    val actions: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val schemes: List<String> = emptyList(),
)

/**
 * 描述一个静态广播接收器的信息
 * @param className 接收器的完整类名
 * @param enabled 是否启用
 * @param exported 是否导出
 * @param intentFilters 它声明的意图过滤器列表
 */
@Serializable
data class StaticReceiverInfo(
    val className: String,
    val enabled: Boolean,
    val exported: Boolean,
    val intentFilters: List<IntentFilterInfo> = emptyList(),
)

/**
 * 描述一个插件 ContentProvider 的元数据信息
 * @param className Provider 的完整类名
 * @param authorities 它声明的授权列表 (例如 "com.plugin.notes")
 * @param enabled 是否启用
 * @param exported 是否导出
 * @param metaData 它声明的元数据列表
 */
@Serializable
data class ProviderInfo(
    val className: String,
    val authorities: List<String>,
    val enabled: Boolean,
    val exported: Boolean,
    val metaData: List<MetaDataInfo> = emptyList(),
)
