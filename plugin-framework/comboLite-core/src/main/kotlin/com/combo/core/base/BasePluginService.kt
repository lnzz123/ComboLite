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

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import com.combo.core.interfaces.IPluginService

/**
 * 插件Service的基类，提供了IPluginService的默认实现。
 * 插件开发者应该继承此类来创建自己的Service。
 */
open class BasePluginService : IPluginService {
    /**
     * 插件Service的代理Service。
     * 通过它，插件可以访问Context等Android系统服务。
     */
    protected var proxyService: Service? = null
        private set

    /**
     * 当宿主加载插件时，会通过这个方法把自身（代理Service）传递进来。
     */
    override fun onAttach(proxyService: Service) {
        this.proxyService = proxyService
    }

    override fun onCreate() {}

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = Service.START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onUnbind(intent: Intent?): Boolean = false

    override fun onRebind(intent: Intent?) {}

    override fun onDestroy() {}

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {}

    override fun onTrimMemory(level: Int) {}
}
