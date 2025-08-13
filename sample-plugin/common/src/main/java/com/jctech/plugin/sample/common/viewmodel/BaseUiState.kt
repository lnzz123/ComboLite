package com.jctech.plugin.sample.common.viewmodel

/**
 * 所有UI状态的基接口，提供了通用的加载、错误状态管理
 * 采用不可变设计，确保状态更新的安全性
 */
interface BaseUiState {
    val isLoading: Boolean
    val isError: Boolean
    val errorMessage: String?
}
