package com.jctech.plugin.sample.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 所有 ViewModel 的基类，继承自 Android 的 [ViewModel] 类。
 * 该类提供了一个通用的 [ViewModelKey] 和创建 [ViewModelStateFlow] 的方法，
 * 用于在不同的 ViewModel 中统一管理状态流。
 *
 * 优化特性：
 * - 状态更新去重机制，避免不必要的UI刷新
 * - 丰富的便利方法，简化状态管理
 * - 日志和调试支持
 * - 测试友好的设计
 */
abstract class BaseViewModel<T : BaseUiState>(
    initialState: T,
) : ViewModel() {

    /**
     * 用于标识当前 ViewModel 的唯一键，通过当前类的全限定名创建。
     * 该键会在创建 [ViewModelStateFlow] 时使用，确保状态流的操作与当前 ViewModel 关联。
     */
    protected val key: ViewModelKey = ViewModelKey(this::class.java.name)

    /**
     * 初始的 UI 状态
     */
    protected val initialUiState: T = initialState

    /**
     * 调试模式开关，可在子类中重写
     */
    protected open val isDebugMode: Boolean = false

    /**
     * 私有的可变状态流，用于在 ViewModel 内部更新 UI 状态。
     */
    private val _uiState: ViewModelStateFlow<T> = viewModelStateFlow(initialUiState)

    /**
     * 只读状态流，暴露给外部用于观察 UI 状态的变化。
     */
    val uiState: StateFlow<T> = _uiState

    /**
     * 状态历史记录（仅在调试模式下启用）
     */
    private val stateHistory = if (isDebugMode) mutableListOf<T>() else null

    /**
     * 创建一个 [ViewModelStateFlow] 实例。
     * 该方法会使用当前 ViewModel 的 [key] 和传入的初始值 [value] 来初始化状态流。
     *
     * @param R 状态流中存储的值的类型。
     * @param value 状态流的初始值。
     * @return 一个新的 [ViewModelStateFlow] 实例。
     */
    private fun <R> viewModelStateFlow(value: R): ViewModelStateFlow<R> =
        ViewModelStateFlow(key = key, value = value)

    /**
     * 封装的状态更新方法，默认使用 viewModelScope 协程作用域
     * 包含状态去重机制，避免不必要的UI更新
     * @param update 状态更新逻辑
     */
    protected fun updateState(update: T.() -> T) {
        updateState(viewModelScope, update)
    }

    /**
     * 封装的状态更新方法，使用自定义的协程作用域
     * 包含状态去重机制，避免不必要的UI更新
     * @param coroutineScope 自定义的协程作用域
     * @param update 状态更新逻辑
     */
    protected fun updateState(coroutineScope: CoroutineScope, update: T.() -> T) {
        coroutineScope.launch {
            try {
                val oldState = _uiState.value
                val newState = oldState.update()

                // 状态去重机制：只有状态真正改变时才更新
                if (newState != oldState) {
                    logStateChange(oldState, newState)
                    recordStateHistory(newState)
                    _uiState.emitValue(newState)
                }
            } catch (e: Exception) {
                handleStateUpdateError(e)
            }
        }
    }

    /**
     * 同步状态更新方法，直接更新状态值而不使用协程
     * 适用于简单的状态更新场景
     * @param update 状态更新逻辑
     */
    protected fun updateStateSync(update: T.() -> T) {
        try {
            val oldState = _uiState.value
            val newState = oldState.update()

            if (newState != oldState) {
                logStateChange(oldState, newState)
                recordStateHistory(newState)
                _uiState.value = newState
            }
        } catch (e: Exception) {
            handleStateUpdateError(e)
        }
    }

    /**
     * 处理状态更新时发生的错误，子类可以重写该方法实现自定义错误处理
     * @param error 发生的异常
     */
    open fun handleStateUpdateError(error: Exception) {
        logError(error)
    }

    /**
     * 记录状态变化日志
     */
    private fun logStateChange(oldState: T, newState: T) {
        if (isDebugMode) {
            Timber.d("State updated in " + this.key + ": " + oldState + " -> " + newState)
        }
    }

    /**
     * 记录错误日志
     */
    private fun logError(error: Exception) {
        if (isDebugMode) {
            Timber.e(error, "State update error in " + this.key)
        }
    }

    /**
     * 记录状态历史
     */
    private fun recordStateHistory(newState: T) {
        stateHistory?.add(newState)
    }
}
