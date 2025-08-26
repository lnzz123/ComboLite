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

package com.combo.core.interfaces

import androidx.compose.runtime.Composable
import com.combo.core.model.PluginContext
import org.koin.core.module.Module

/**
 * 插件配置接口
 * @property pluginModule 插件依赖注入模块
 * @property onLoad 插件被加载时调用
 * @property onUnload 插件被卸载时调用
 * @property Content 插件主界面
 */
interface IPluginEntryClass {

    /**
     *   (可选) 声明此插件提供的 Koin 依赖注入模块
     *   插件化框架会自动处理依赖注入的加载和卸载
     */
    val pluginModule: List<Module>

    /**
     * onLoad 生命周期回调
     * 当插件被框架加载后，此方法会被调用。
     * 这是执行所有初始化逻辑的最佳位置。
     *
     * @param context 插件运行的上下文环境
     */
    fun onLoad(context: PluginContext)

    /**
     * onUnload 生命周期回调
     * 当插件被框架卸载前，此方法会被调用。
     * 这是执行所有资源清理工作的最佳位置。
     */
    fun onUnload()

    /**
     * 提供插件的 UI 入口
     * 这个方法专门用于定义和返回插件的 Jetpack Compose 界面。
     */
    @Composable
    fun Content()
}
