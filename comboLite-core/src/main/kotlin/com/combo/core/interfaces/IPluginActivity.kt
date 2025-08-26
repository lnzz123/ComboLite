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

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity

/**
 * 插件Activity接口
 * 定义插件Activity的基本行为和生命周期
 */
interface IPluginActivity {
    /**
     * 【必须】将代理Activity注入插件，建立连接。这是插件生命周期的第一步，在onCreate之前调用。
     * 通过持有的 proxyActivity 引用，插件可以调用所有 Context 和 Activity 的方法。
     * @param proxyActivity 正在运行的代理Activity实例。
     */
    fun onAttach(proxyActivity: ComponentActivity)

    // 标准生命周期
    fun onCreate(savedInstanceState: Bundle?)

    fun onResume()

    fun onPause()

    fun onStart()

    fun onStop()

    fun onDestroy()

    fun onRestart()

    // 交互与结果处理

    /**
     * 当通过 requestPermissions 请求权限后，系统返回结果时调用。
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int,
    )

    // 状态保存与恢复

    /**
     * 在Activity可能被系统意外销毁前调用（如屏幕旋转、内存不足），用于保存临时状态。
     * 保存的数据会在 onCreate 或 onRestoreInstanceState 中恢复。
     */
    fun onSaveInstanceState(outState: Bundle)

    /**
     * 在Activity从已保存状态重新创建时调用。此方法在 onStart 之后调用。
     * 你也可以选择在 onCreate 中恢复数据，两者效果类似。
     */
    fun onRestoreInstanceState(savedInstanceState: Bundle)

    // 系统与窗口事件

    /**
     * 当设备配置改变时调用，例如屏幕旋转、深色模式切换、语言更改等。
     */
    fun onConfigurationChanged(newConfig: Configuration)

    /**
     * 当Activity的窗口焦点发生变化时调用（例如，弹出对话框或下拉通知栏）。
     */
    fun onWindowFocusChanged(hasFocus: Boolean)

    // 底层输入事件

    /**
     * 处理物理按键按下事件。仅在需要全局拦截或处理特殊按键时使用。
     * @return true 表示消费事件，false 表示不消费。
     */
    fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean?

    /**
     * 处理物理按键抬起事件。
     */
    fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean?

    /**
     * 处理触摸事件。通常由Compose手势修饰符处理，仅在需要进行全局触摸拦截时使用。
     */
    fun onTouchEvent(event: MotionEvent?): Boolean?
}
