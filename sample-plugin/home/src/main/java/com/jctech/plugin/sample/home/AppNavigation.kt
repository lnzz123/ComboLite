package com.jctech.plugin.sample.home

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jctech.plugin.sample.common.navigation.AppScreen
import com.jctech.plugin.sample.home.screen.HomeScreen
import com.jctech.plugin.sample.guide.ScreenOne
import com.jctech.plugin.sample.guide.ScreenTwo

/**
 * 在给定的 [NavGraphBuilder] 中定义应用的导航图。
 * 此函数现在接收一个 SharedTransitionScope 实例作为参数，
 * 允许在导航过程中使用共享过渡动画。
 *
 * @param sharedTransitionScope 用于实现共享元素过渡的上下文作用域。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.appNavigation(sharedTransitionScope: SharedTransitionScope) {
    with(sharedTransitionScope) {
        composable<AppScreen.Home> {
            HomeScreen()
        }

        composable<AppScreen.ScreenOne> {
            ScreenOne()
        }

        composable<AppScreen.ScreenTwo> {
            ScreenTwo()
        }
    }
}
