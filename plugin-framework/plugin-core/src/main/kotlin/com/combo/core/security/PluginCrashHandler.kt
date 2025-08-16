package com.combo.core.security

import android.content.Context
import android.content.Intent
import com.combo.core.ext.startPluginActivity
import com.combo.core.manager.PluginManager
import timber.log.Timber

/**
 * 全局插件崩溃处理器 (优化版)
 *
 * 作为一个全局的"安全网"，负责捕获所有未被处理的致命异常。
 *
 * 核心职责:
 * 1. 在异常链中精准查找由插件依赖问题引发的 `PluginDependencyException`。
 * 2. 如果找到，则自动禁用肇事插件，实现"熔断"机制。
 * 3. 启动一个友好的错误提示页面 (`CrashActivity`)，而不是直接重启，提升用户体验。
 * 4. 如果是其他未知类型的崩溃，则交由系统默认的处理器处理。
 */
class PluginCrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val pluginException = findCause<PluginDependencyException>(throwable)

        if (pluginException != null) {
            handlePluginException(pluginException)
        } else {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * 处理由 PluginDependencyException 引发的崩溃
     */
    private fun handlePluginException(exception: PluginDependencyException) {
        try {
            Timber.e(exception, "捕获到插件运行时依赖异常，将自动禁用插件并重启。")

            val culpritPluginId = exception.culpritPluginId
            val missingClassName = exception.missingClassName

            Timber.e("定位到肇事插件: [$culpritPluginId], 缺失的类: [$missingClassName]")
            Timber.e("自动禁用插件 [$culpritPluginId] 以防止应用下次启动时再次崩溃。")

            PluginManager.setPluginEnabled(culpritPluginId, false)

            context.startPluginActivity(CrashActivity::class.java) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(
                    EXTRA_CRASH_MESSAGE,
                    "功能模块 '$culpritPluginId' 遇到问题已被自动修复。\n请重启应用以恢复正常。"
                )
            }

        } catch (_: Exception) {
            defaultHandler?.uncaughtException(Thread.currentThread(), exception)
        }
    }

    /**
     * 在异常链中递归查找指定类型的 Cause
     */
    private inline fun <reified T : Throwable> findCause(throwable: Throwable?): T? {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }

    companion object {
        private const val TAG = "PluginCrashHandler"
        const val EXTRA_CRASH_MESSAGE = "CRASH_MESSAGE"

        /**
         * 初始化并注册全局崩溃处理器
         * @param context Application Context
         */
        fun initialize(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

            if (defaultHandler is PluginCrashHandler) {
                Timber.tag(TAG).w("PluginCrashHandler 已被初始化，无需重复设置。")
                return
            }
            Thread.setDefaultUncaughtExceptionHandler(
                PluginCrashHandler(context.applicationContext, defaultHandler)
            )
            Timber.tag(TAG).i("全局插件崩溃处理器已成功注册。")
        }
    }
}