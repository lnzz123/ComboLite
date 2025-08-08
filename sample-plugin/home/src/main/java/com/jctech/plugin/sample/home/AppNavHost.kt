package com.jctech.plugin.sample.home

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.jctech.plugin.sample.common.navigation.AppScreen

/**
 * 设置导航主机，管理应用的导航逻辑。
 *
 * @param navHostController 用于控制导航的控制器。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(
  navHostController: NavHostController,
) {
  SharedTransitionLayout {
    NavHost(
      navController = navHostController,
      startDestination = AppScreen.Home,
    ) {
      appNavigation(sharedTransitionScope = this@SharedTransitionLayout)
    }
  }
}
