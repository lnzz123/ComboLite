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

package com.combo.core.base

import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.PersistableBundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import com.combo.core.ext.getPluginActivity
import com.combo.core.interfaces.IPluginActivity
import com.combo.core.manager.PluginManager
import timber.log.Timber

/**
 * 宿主端的代理Activity基类
 * 它负责加载插件Activity并代理其所有生命周期方法。
 */
open class BaseHostActivity : ComponentActivity() {
    protected var pluginActivity: IPluginActivity? = null
        private set

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
     * 提供一个给子类调用的、受保护的插件初始化方法。
     * 子类在自己的 onCreate 中调用此方法来"尝试"加载插件。
     */
    protected fun initPluginActivity() {
        try {
            intent ?: return
            pluginActivity = intent.getPluginActivity()
            Timber.d("加载插件Activity [${pluginActivity?.javaClass?.name}]")
            pluginActivity?.onAttach(this@BaseHostActivity)
        } catch (e: Exception) {
            e.printStackTrace()
            pluginActivity = null
        }
    }

    /**
     * 标准生命周期
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPluginActivity()
        pluginActivity?.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        pluginActivity?.onStart()
    }

    override fun onResume() {
        super.onResume()
        pluginActivity?.onResume()
    }

    override fun onPause() {
        super.onPause()
        pluginActivity?.onPause()
    }

    override fun onStop() {
        super.onStop()
        pluginActivity?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        pluginActivity?.onDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        pluginActivity?.onRestart()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        pluginActivity?.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
    }

    override fun onSaveInstanceState(
        outState: Bundle,
        outPersistentState: PersistableBundle,
    ) {
        super.onSaveInstanceState(outState, outPersistentState)
        pluginActivity?.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        pluginActivity?.onRestoreInstanceState(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        pluginActivity?.onConfigurationChanged(newConfig)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        pluginActivity?.onWindowFocusChanged(hasFocus)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean = pluginActivity?.onKeyDown(keyCode, event) ?: super.onKeyDown(keyCode, event)

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean = pluginActivity?.onKeyUp(keyCode, event) ?: super.onKeyUp(keyCode, event)

    override fun onTouchEvent(event: MotionEvent?): Boolean =
        pluginActivity?.onTouchEvent(event) ?: super.onTouchEvent(event)
}
