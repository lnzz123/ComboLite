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

package com.combo.plugin.sample.example.receiver

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 一个简单的单例日志记录器，用于在UI和后台Receiver之间共享接收到的广播信息。
 */
object BroadcastLog {
    const val DYNAMIC_ACTION = "com.combo.plugin.example.action.DYNAMIC_BROADCAST"
    const val STATIC_ACTION = "com.combo.plugin.example.action.STATIC_BROADCAST"
    private val _logFlow = MutableSharedFlow<String>(replay = 20)
    val logFlow = _logFlow.asSharedFlow()

    /**
     * 添加一条日志
     * @param source 来源 (e.g., "静态", "动态")
     * @param action 广播Action
     * @param data 附加数据
     */
    fun add(source: String, action: String?, data: String?) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val message = "[$time] [$source] 收到广播\n" +
                "   - Action: $action\n" +
                "   - Data: ${data ?: "无"}"
        _logFlow.tryEmit(message)
    }

    /**
     * 清空日志缓存
     * 调用此方法会清除 SharedFlow 的重播缓存区。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        _logFlow.resetReplayCache()
    }
}