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

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import com.combo.core.interfaces.IPluginActivity

open class BasePluginActivity : IPluginActivity {
    /**
     * 插件Activity的代理Activity
     */
    protected var proxyActivity: ComponentActivity? = null
        private set

    /**
     * 当宿主加载插件时，会通过这个方法把自身（代理Activity）传递进来。
     */
    override fun onAttach(proxyActivity: ComponentActivity) {
        this.proxyActivity = proxyActivity
    }

    // 标准生命周期
    override fun onCreate(savedInstanceState: Bundle?) {
    }

    override fun onResume() {}

    override fun onPause() {}

    override fun onStart() {}

    override fun onStop() {}

    override fun onDestroy() {}

    override fun onRestart() {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int,
    ) {
    }

    override fun onSaveInstanceState(outState: Bundle) {}

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {}

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onWindowFocusChanged(hasFocus: Boolean) {}

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean? = null

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean? = null
}
