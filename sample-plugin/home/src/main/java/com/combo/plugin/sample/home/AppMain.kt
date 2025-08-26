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

package com.combo.plugin.sample.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.combo.plugin.sample.common.navigation.AppComposeNavigator
import com.combo.plugin.sample.common.navigation.AppScreen

/**
 * 应用的主界面。
 *
 * @param composeNavigator 一个实现了导航命令处理的导航器，用于处理AppScreen相关的导航。
 */
@Composable
fun AppMain(composeNavigator: AppComposeNavigator<AppScreen>) {
    val navHostController = rememberNavController()

    LaunchedEffect(Unit) {
        composeNavigator.handleNavigationCommands(navHostController)
    }

    AppNavHost(navHostController = navHostController)
}
