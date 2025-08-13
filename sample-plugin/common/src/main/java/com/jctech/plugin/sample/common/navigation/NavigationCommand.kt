package com.jctech.plugin.sample.common.navigation

import androidx.navigation.NavOptions

/**
 * 导航命令接口
 *
 * 定义导航系统支持的所有基础命令，采用密封接口设计模式
 */
sealed interface NavigationCommand {
    /**
     * 返回上一页的导航命令
     */
    data object NavigateUp : NavigationCommand

    /**
     * 返回到根页面的导航命令
     */
    data object NavigateToRoot : NavigationCommand

    /**
     * 返回指定步数的导航命令
     *
     * @property steps 要返回的步数，默认为1
     */
    data class NavigateBack(val steps: Int = 1) : NavigationCommand
}

/**
 * Compose导航命令接口
 *
 * 为Jetpack Compose导航框架定义的特定命令集合
 */
sealed interface ComposeNavigationCommand : NavigationCommand {
    /**
     * 导航到指定路由的命令
     *
     * @property route 目标路由
     * @property options 导航选项，可选
     */
    data class NavigateToRoute<T : Any>(
        val route: T,
        val options: NavOptions? = null,
    ) : ComposeNavigationCommand

    /**
     * 返回并传递结果的导航命令
     *
     * @property key 结果的键名
     * @property result 结果数据
     * @property route 目标路由，可选
     */
    data class NavigateUpWithResult<R, T : Any>(
        val key: String,
        val result: R,
        val route: T? = null,
    ) : ComposeNavigationCommand

    /**
     * 弹出到指定路由的导航命令
     *
     * @property route 目标路由
     * @property inclusive 是否包含目标路由，如果为true则目标路由也会被弹出
     */
    data class PopUpToRoute<T : Any>(
        val route: T,
        val inclusive: Boolean,
    ) : ComposeNavigationCommand

    /**
     * 替换当前页面的导航命令
     *
     * @property route 目标路由
     * @property options 导航选项，可选
     */
    data class ReplaceRoute<T : Any>(
        val route: T,
        val options: NavOptions? = null,
    ) : ComposeNavigationCommand

    /**
     * 导航到指定路由并清空回退栈的命令
     *
     * @property route 目标路由
     */
    data class NavigateAndClearBackStack<T : Any>(
        val route: T,
    ) : ComposeNavigationCommand

    /**
     * 导航到指定路由并关闭当前页面的命令
     *
     * @property route 目标路由
     * @property options 导航选项，可选
     */
    data class NavigateAndFinishCurrent<T : Any>(
        val route: T,
        val options: NavOptions? = null,
    ) : ComposeNavigationCommand
}
