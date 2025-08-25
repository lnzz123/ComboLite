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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.combo.plugin.sample.common.navigation.AppScreen
import com.combo.plugin.sample.example.screen.ActivityScreen
import com.combo.plugin.sample.example.screen.BroadcastReceiverScreen
import com.combo.plugin.sample.example.screen.ContentProviderScreen
import com.combo.plugin.sample.example.screen.ServiceScreen
import com.combo.plugin.sample.example.screen.SoLibraryScreen
import com.combo.plugin.sample.home.screen.HomeScreen

/**
 * 在给定的 [NavGraphBuilder] 中定义应用的导航图。
 * 此函数现在接收一个 SharedTransitionScope 实例作为参数，
 * 允许在导航过程中使用共享过渡动画。
 *
 * @param sharedTransitionScope 用于实现共享元素过渡的上下文作用域。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.appNavigation(sharedTransitionScope: SharedTransitionScope) {
    with(sharedTransitionScope) {
        composable<AppScreen.Home> {
            HomeScreen()
        }

        composable<AppScreen.PluginActivity> {
            ActivityScreen()
        }

        composable<AppScreen.PluginService> {
            ServiceScreen()
        }

        composable<AppScreen.BroadcastReceiver> {
            BroadcastReceiverScreen()
        }

        composable<AppScreen.ContentProvider> {
            ContentProviderScreen()
        }

        composable<AppScreen.SoLibrary> {
            SoLibraryScreen()
        }
    }
}
