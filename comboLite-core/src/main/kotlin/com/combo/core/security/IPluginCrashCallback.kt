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

package com.combo.core.security

/**
 * 封装了插件崩溃的详细信息。
 *
 * @param throwable 捕获到的原始异常。
 * @param culpritPluginId 引发崩溃的插件ID。如果无法确定，则为 null。
 * @param defaultMessage 框架根据异常类型生成的默认提示信息。
 */
data class PluginCrashInfo(
    val throwable: Throwable,
    val culpritPluginId: String?,
    val defaultMessage: String
)

/**
 * 插件崩溃处理回调接口。
 * 开发者可以实现此接口，并通过 `PluginCrashHandler.setCrashCallback` 进行注册，
 * 以自定义处理插件崩溃的逻辑。
 */
interface IPluginCrashCallback {

    /**
     * 当插件因 ClassCastException 崩溃时调用。
     * 通常发生在插件热更新后，新旧类的实例冲突。
     *
     * @param info 崩溃详情。
     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
     */
    fun onClassCastException(info: PluginCrashInfo): Boolean = false

    /**
     * 当插件因 PluginDependencyException (ClassNotFound) 崩溃时调用。
     *
     * @param info 崩溃详情。
     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
     */
    fun onDependencyException(info: PluginCrashInfo): Boolean = false

    /**
     * 当插件因 Resources.NotFoundException 崩溃时调用。
     * 通常发生在插件更新后，代码尝试访问一个不再存在的资源ID。
     *
     * @param info 崩溃详情。
     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
     */
    fun onResourceNotFoundException(info: PluginCrashInfo): Boolean = false

    /**
     * 当插件因 API 不兼容 (如 NoSuchMethodError, NoSuchFieldError) 崩溃时调用。
     *
     * @param info 崩溃详情。
     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
     */
    fun onApiIncompatibleException(info: PluginCrashInfo): Boolean = false

    /**
     * 当发生其他与插件相关的未知异常时调用。
     *
     * @param info 崩溃详情。
     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
     */
    fun onOtherPluginException(info: PluginCrashInfo): Boolean = false
}