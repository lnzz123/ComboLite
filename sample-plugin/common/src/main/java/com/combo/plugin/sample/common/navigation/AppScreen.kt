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

import kotlinx.serialization.Serializable

/**
 * 应用屏幕路由定义
 *
 * 使用密封接口定义应用中所有可导航的屏幕
 * 采用此设计可提供类型安全的导航，并便于扩展
 */
sealed interface AppScreen {
    @Serializable
    data object Home : AppScreen

    @Serializable
    data object PluginService : AppScreen

    @Serializable
    data object PluginActivity : AppScreen

    @Serializable
    data object BroadcastReceiver : AppScreen
}
