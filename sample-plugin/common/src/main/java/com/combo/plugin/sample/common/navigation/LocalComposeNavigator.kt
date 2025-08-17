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

package com.combo.plugin.sample.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

/**
 * 本地导航器组合项
 *
 * 提供全局访问导航器的能力，如果未提供导航器则会抛出错误
 * 这是使用Composition Local机制让导航器可在整个Composable层次结构中使用的关键部分
 */
val LocalComposeNavigator: ProvidableCompositionLocal<AppComposeNavigator<AppScreen>> =
    compositionLocalOf {
        error(
            "未提供 AppComposeNavigator！",
        )
    }

/**
 * 当前导航器访问扩展属性
 *
 * 提供一个便捷的方式在Composable中访问当前的导航器实例
 * 使用@Composable和@ReadOnlyComposable注解确保在组合阶段安全访问
 */
val currentComposeNavigator: AppComposeNavigator<AppScreen>
    @Composable
    @ReadOnlyComposable
    get() = LocalComposeNavigator.current
