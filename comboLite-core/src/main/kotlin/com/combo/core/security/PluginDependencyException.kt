/*
 * Copyright (c) 2025, 贵州君城网络科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.combo.core.security

/**
 * 插件依赖异常
 *
 * 当一个插件在运行时无法找到其依赖的类时抛出。
 * 这个异常会携带肇事插件ID和缺失的类名，以便上层处理器能够精准定位问题。
 *
 * @param culpritPluginId 引发此异常的插件ID (即哪个插件在尝试加载一个不存在的类)。
 * @param missingClassName 未找到的、依赖的类的完整名称。
 * @param cause 原始的 ClassNotFoundException。
 */
class PluginDependencyException(
    val culpritPluginId: String,
    val missingClassName: String,
    cause: Throwable? = null
) : ClassNotFoundException("插件 [$culpritPluginId] 依赖的类 [$missingClassName] 未找到。", cause)