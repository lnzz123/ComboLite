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
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.IBinder
import com.combo.core.ext.ExtConstant
import com.combo.core.ext.getPluginService
import com.combo.core.interfaces.IPluginService
import com.combo.core.manager.PluginManager
import timber.log.Timber

/**
 * 宿主端的代理Service基类。
 * 这个Service需要在宿主应用的AndroidManifest.xml中注册。
 * 它负责加载插件Service并代理其所有生命周期方法。
 */
open class BaseHostService : Service() {
    /**
     * 插件Service的实例。
     * 这个字段存储了当前代理的插件Service的实例。
     * 当插件Service被绑定或启动时，会通过反射创建插件Service的实例。
     * 当插件Service被解绑或销毁时，会将这个字段设为null。
     */
    protected var pluginService: IPluginService? = null
        private set

    /**
     * 存储当前代理的插件Service的完整类名。
     * 这个字段至关重要，用于在onDestroy时通知ProxyManager要释放哪个插件的映射。
     */
    private var pluginClassName: String? = null

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

    /**
     * 在Service创建时，初始化插件Service。
     */
    private fun initPluginService(intent: Intent?) {
        if (pluginService == null) {
            synchronized(this) {
                if (pluginService == null) {
                    try {
                        val instance = intent?.getPluginService()
                        if (instance == null) {
                            throw IllegalStateException("创建插件服务实例失败: $pluginClassName")
                        }
                        this.pluginService = instance
                        this.pluginClassName =
                            intent.getStringExtra(ExtConstant.PLUGIN_SERVICE_CLASS_NAME)
                        pluginService!!.onAttach(this@BaseHostService)
                        pluginService!!.onCreate()
                    } catch (e: Exception) {
                        Timber.e(e, "初始化插件 [${this.pluginClassName}] 失败。")
                        pluginService = null
                        stopSelf()
                    }
                }
            }
        }
    }

    // --- 生命周期代理 ---

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        initPluginService(intent)
        return pluginService?.onStartCommand(intent, flags, startId)
            ?: super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        initPluginService(intent)
        return pluginService?.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean =
        pluginService?.onUnbind(intent) ?: super.onUnbind(intent)

    override fun onRebind(intent: Intent?) {
        pluginService?.onRebind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        pluginService?.onDestroy()
        pluginService = null
        pluginClassName?.let {
            PluginManager.proxyManager.releaseServiceProxy(it)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        pluginService?.onConfigurationChanged(newConfig)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        pluginService?.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        pluginService?.onTrimMemory(level)
    }
}
