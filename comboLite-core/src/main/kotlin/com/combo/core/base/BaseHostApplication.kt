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

package com.combo.core.base

import android.app.Application
import android.content.res.AssetManager
import android.content.res.Resources
import com.combo.core.manager.PluginManager
import com.combo.core.security.PluginCrashHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 宿主端的插件框架Application基类
 * 用于快速一键初始化插件框架，加载插件
 */
open class BaseHostApplication : Application() {
    /**
     * 初始化工作
     */
    override fun onCreate() {
        super.onCreate()
        PluginCrashHandler.initialize(this)
        PluginManager.initialize(this) {
            CoroutineScope(Dispatchers.Default).launch {
                PluginManager.loadEnabledPlugins()
            }
        }
    }

    /**
     * 重写getResources方法，返回插件资源
     */
    override fun getResources(): Resources =
        if (PluginManager.isInitialized) {
            PluginManager.resourcesManager.getResources()
        } else {
            super.getResources()
        }

    /**
     * 重写getAssets方法，返回插件资源
     */
    override fun getAssets(): AssetManager =
        if (PluginManager.isInitialized) {
            PluginManager.resourcesManager.getResources().assets
        } else {
            super.getAssets()
        }
}
