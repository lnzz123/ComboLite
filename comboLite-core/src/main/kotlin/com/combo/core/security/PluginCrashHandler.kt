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

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Process
import com.combo.core.ext.startPluginActivity
import com.combo.core.manager.PluginManager
import timber.log.Timber
import kotlin.system.exitProcess

/**
 * 全局插件崩溃处理器
 *
 * 作为一个全局的"安全网"，负责捕获、识别、并分发所有由插件引起的致命异常。
 *
 * 核心职责:
 * 1.  **精准识别**: 捕获多种插件化场景下的常见异常（依赖、类型转换、资源、API兼容性等）。
 * 2.  **定位源头**: 通过分析堆栈轨迹，尽可能找到引发崩溃的具体插件。
 * 3.  **委托处理**: 允许开发者通过注册 `IPluginCrashCallback` 自定义处理逻辑。
 * 4.  **默认保障**: 如果开发者未处理，则执行默认的容错逻辑（禁用插件、友好提示、重启应用）。
 */
class PluginCrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        const val EXTRA_CRASH_MESSAGE = "CRASH_MESSAGE"
        private var crashCallback: IPluginCrashCallback? = null

        /**
         * 初始化并注册全局崩溃处理器
         * @param context Application Context
         * @param callback （可选）开发者的自定义崩溃处理回调
         */
        @JvmStatic
        fun initialize(context: Context, callback: IPluginCrashCallback? = null) {
            this.crashCallback = callback
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

            if (defaultHandler is PluginCrashHandler) {
                Timber.w("PluginCrashHandler 已被初始化，无需重复设置。")
                return
            }
            Thread.setDefaultUncaughtExceptionHandler(
                PluginCrashHandler(context.applicationContext, defaultHandler)
            )
            Timber.i("全局插件崩溃处理器已成功注册。")
        }

        /**
         * 设置或更新插件崩溃处理回调。
         * 可以在 initialize 之后随时调用此方法来配置自定义处理逻辑。
         * @param callback 开发者自定义的崩溃处理回调，传入 null 可清除。
         */
        @JvmStatic
        fun setCallback(callback: IPluginCrashCallback?) {
            this.crashCallback = callback
            Timber.i("PluginCrashHandler 回调已更新。")
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 尝试将异常作为插件相关异常来处理
            val wasHandled = handlePluginRelatedException(throwable)

            // 如果插件异常处理器没有处理它，则交由默认处理器
            if (!wasHandled) {
                Timber.d("异常并非由插件引起，或未被自定义回调处理，交由默认处理器。")
                defaultHandler?.uncaughtException(thread, throwable)
            }
        } catch (e: Exception) {
            // 确保即使在我们的处理逻辑中也发生异常，也不会丢失原始的崩溃信息
            Timber.e(e, "在PluginCrashHandler内部处理异常时发生错误！")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun handlePluginRelatedException(throwable: Throwable): Boolean {
        val culpritPluginId = findCulpritPluginId(throwable)

        // 1. 依赖缺失异常 (最明确)
        findCause<PluginDependencyException>(throwable)?.let {
            val info = PluginCrashInfo(
                it,
                it.culpritPluginId,
                "功能模块 '${it.culpritPluginId}' 缺少必要的依赖组件 '${it.missingClassName}'。"
            )
            if (crashCallback?.onDependencyException(info) == true) return true
            handlePluginExceptionByDefault(info)
            return true
        }

        // 2. 类型转换异常
        findCause<ClassCastException>(throwable)?.let {
            if (culpritPluginId != null) {
                val info = PluginCrashInfo(
                    it,
                    culpritPluginId,
                    "功能模块 '$culpritPluginId' 似乎未能完全更新，导致组件冲突。"
                )
                if (crashCallback?.onClassCastException(info) == true) return true
                handleIncompatibleUpdateByDefault(info)
                return true
            }
        }

        // 3. 资源未找到异常
        findCause<Resources.NotFoundException>(throwable)?.let {
            if (culpritPluginId != null) {
                val info = PluginCrashInfo(
                    it,
                    culpritPluginId,
                    "功能模块 '$culpritPluginId' 尝试访问一个不存在的资源。"
                )
                if (crashCallback?.onResourceNotFoundException(info) == true) return true
                handleIncompatibleUpdateByDefault(info)
                return true
            }
        }

        // 4. API不兼容异常
        if (throwable is NoSuchMethodError || throwable is NoSuchFieldError || throwable is AbstractMethodError) {
            if (culpritPluginId != null) {
                val info = PluginCrashInfo(
                    throwable,
                    culpritPluginId,
                    "功能模块 '$culpritPluginId' 与当前应用版本不兼容。"
                )
                if (crashCallback?.onApiIncompatibleException(info) == true) return true
                handlePluginExceptionByDefault(info)
                return true
            }
        }

        // 5. 其他与插件相关的异常
        if (culpritPluginId != null) {
            val info = PluginCrashInfo(
                throwable,
                culpritPluginId,
                "功能模块 '$culpritPluginId' 发生未知错误。"
            )
            if (crashCallback?.onOtherPluginException(info) == true) return true
            handlePluginExceptionByDefault(info)
            return true
        }

        return false
    }

    /**
     * 默认处理方式 A：禁用肇事插件，并提示用户
     */
    private fun handlePluginExceptionByDefault(info: PluginCrashInfo) {
        Timber.e(info.throwable, "默认处理：将禁用插件 [${info.culpritPluginId}]")
        info.culpritPluginId?.let { PluginManager.setPluginEnabled(it, false) }

        val message = "${info.defaultMessage}\n该模块已被临时禁用，重启应用后即可恢复其他功能。"
        showCrashActivity(message)
        killProcess()
    }

    /**
     * 默认处理方式 B：不禁用插件，但强制用户重启
     */
    private fun handleIncompatibleUpdateByDefault(info: PluginCrashInfo) {
        Timber.e(
            info.throwable,
            "默认处理：插件 [${info.culpritPluginId}] 发生更新不兼容问题，要求重启。"
        )
        val message = "${info.defaultMessage}\n请重启应用以完成更新并解决此问题。"
        showCrashActivity(message)
        killProcess()
    }

    private fun showCrashActivity(message: String) {
        context.startPluginActivity(CrashActivity::class.java) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(EXTRA_CRASH_MESSAGE, message)
        }
    }

    private fun killProcess() {
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    /**
     * 通过分析堆栈轨迹，找到第一个属于插件的类，从而定位肇事插件ID
     */
    private fun findCulpritPluginId(throwable: Throwable?): String? {
        var current: Throwable? = throwable
        while (current != null) {
            for (element in current.stackTrace) {
                val pluginId = PluginManager.getClassIndex()[element.className]
                if (pluginId != null) {
                    return pluginId
                }
            }
            current = current.cause
        }
        return null
    }

    private inline fun <reified T : Throwable> findCause(throwable: Throwable?): T? {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }
}