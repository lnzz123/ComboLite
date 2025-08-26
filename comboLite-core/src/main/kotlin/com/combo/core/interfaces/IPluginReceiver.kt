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

import android.content.Context
import android.content.Intent

/**
 * 插件广播接收器接口。
 */
interface IPluginReceiver {
    /**
     * 当广播到达时，由宿主代理调用此方法。
     *
     * @param context 宿主应用的上下文。插件可以使用此上下文来执行各种操作，
     * 例如启动服务、发送通知等。
     * @param intent  包含广播动作和数据的原始 Intent 对象。
     */
    fun onReceive(
        context: Context,
        intent: Intent,
    )
}
