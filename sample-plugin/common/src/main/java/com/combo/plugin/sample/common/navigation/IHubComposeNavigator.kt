package com.combo.plugin.sample.common.navigation

import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions

/**
 * IHUB应用导航器实现类
 *
 * 实现了AppComposeNavigator抽象类，为IHUB应用提供具体的导航功能
 * 使用Koin依赖注入框架管理实例
 */
class IHubComposeNavigator : AppComposeNavigator<AppScreen>() {
    /**
     * 实现导航到指定路由的方法
     *
     * @param route 目标路由
     * @param optionsBuilder 导航选项构建器，可选
     */
    override fun navigate(
        route: AppScreen,
        optionsBuilder: (NavOptionsBuilder.() -> Unit)?,
    ) {
        if (!canNavigate()) return

        val options = optionsBuilder?.let { navOptions(it) }
        navigationCommands.tryEmit(ComposeNavigationCommand.NavigateToRoute(route, options))
    }

    /**
     * 实现返回并传递结果的方法
     *
     * @param key 结果的键名
     * @param result 结果数据
     * @param route 目标路由，可选
     */
    override fun <R> navigateBackWithResult(
        key: String,
        result: R,
        route: AppScreen?,
    ) {
        if (!canNavigate()) return

        navigationCommands.tryEmit(
            ComposeNavigationCommand.NavigateUpWithResult(
                key = key,
                result = result,
                route = route,
            ),
        )
    }

    /**
     * 实现弹出到指定路由的方法
     *
     * @param route 目标路由
     * @param inclusive 是否包含目标路由，如果为true则目标路由也会被弹出
     */
    override fun popUpTo(
        route: AppScreen,
        inclusive: Boolean,
    ) {
        if (!canNavigate()) return

        navigationCommands.tryEmit(ComposeNavigationCommand.PopUpToRoute(route, inclusive))
    }

    /**
     * 实现导航到新页面并清空回退栈的方法
     *
     * @param route 目标路由
     */
    override fun navigateAndClearBackStack(route: AppScreen) {
        if (!canNavigate()) return

        navigationCommands.tryEmit(ComposeNavigationCommand.NavigateAndClearBackStack(route))
    }

    /**
     * 实现替换当前页面的方法
     *
     * @param route 目标路由
     */
    override fun replace(route: AppScreen) {
        if (!canNavigate()) return

        navigationCommands.tryEmit(ComposeNavigationCommand.ReplaceRoute(route))
    }

    /**
     * 实现导航到新页面并关闭当前页面的方法
     *
     * @param route 目标路由
     */
    override fun navigateAndFinish(route: AppScreen) {
        if (!canNavigate()) return

        navigationCommands.tryEmit(ComposeNavigationCommand.NavigateAndFinishCurrent(route))
    }
}
