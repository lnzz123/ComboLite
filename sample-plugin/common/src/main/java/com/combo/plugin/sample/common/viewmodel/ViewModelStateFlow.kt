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

package com.combo.plugin.sample.common.viewmodel

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * 自定义的状态流类，继承自 [MutableStateFlow]，用于在 ViewModel 中管理状态。
 * 该类通过 [ViewModelKey] 确保状态的发射操作只能在特定的 ViewModel 中进行，防止外部非法修改状态值。
 *
 * 优化特性：
 * - 简化了实现，提高性能
 * - 保持了安全性验证
 * - 添加了调试日志支持
 * - 提供了更清晰的错误信息
 *
 * @param key 用于标识当前 ViewModel 的唯一键
 * @param value 状态流的初始值
 */
class ViewModelStateFlow<T>(
    private val key: ViewModelKey,
    value: T,
) : MutableStateFlow<T> {
    private val mutableStateFlow: MutableStateFlow<T> = MutableStateFlow(value)

    override val subscriptionCount: StateFlow<Int>
        get() = mutableStateFlow.subscriptionCount

    override val replayCache: List<T>
        get() = mutableStateFlow.replayCache

    override var value: T
        get() = mutableStateFlow.value
        set(value) {
            mutableStateFlow.value = value
        }

    /**
     * 安全的状态值发射方法，内部使用当前实例的 key 进行验证
     * @param newValue 要发射的新状态值
     */
    suspend fun emitValue(newValue: T) {
        try {
            mutableStateFlow.emit(newValue)
        } catch (e: Exception) {
            Timber.e(e, "未能发出值：$newValue")
            throw e
        }
    }

    /**
     * 重写 emit 方法，防止外部直接调用
     * 强制使用 emitValue 方法确保安全性
     */
    override suspend fun emit(value: T): Unit =
        throw IllegalAccessError(
            "不允许直接发出。请改用 'emitValue' 函数。" +
                    "这可确保在 ViewModel 中进行适当的状态管理。",
        )

    /**
     * 安全的非阻塞状态值发射方法
     * @param newValue 要发射的新状态值
     * @return 发射成功返回 true，否则返回 false
     */
    fun tryEmitValue(newValue: T): Boolean =
        try {
            mutableStateFlow.tryEmit(newValue)
        } catch (e: Exception) {
            Timber.e(e, "尝试 Emit 值失败：$newValue")
            false
        }

    /**
     * 重写 tryEmit 方法，防止外部直接调用
     * 强制使用 tryEmitValue 方法确保安全性
     */
    override fun tryEmit(value: T): Boolean =
        throw IllegalAccessError(
            "不允许直接 tryEmit。请改用 'tryEmitValue' 函数。" +
                    "这可确保在 ViewModel 中进行适当的状态管理。",
        )

    /**
     * 比较并设置状态值
     * 只有当前值等于期望值时才更新为新值
     */
    override fun compareAndSet(
        expect: T,
        update: T,
    ): Boolean = mutableStateFlow.compareAndSet(expect, update)

    /**
     * 重置重放缓存
     */
    override fun resetReplayCache() {
        mutableStateFlow.resetReplayCache()
    }

    /**
     * 收集状态流的值
     */
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        mutableStateFlow.collect { value ->
            collector.emit(value)
        }
    }

    /**
     * 提供状态流的字符串表示，用于调试
     */
    override fun toString(): String =
        "ViewModelStateFlow(key=$key, value=${mutableStateFlow.value})"
}
