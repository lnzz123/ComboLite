package com.jctech.plugin.sample.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.jctech.plugin.sample.common.navigation.AppComposeNavigator
import com.jctech.plugin.sample.common.navigation.AppScreen

/**
 * 应用的主界面。
 *
 * @param composeNavigator 一个实现了导航命令处理的导航器，用于处理AppScreen相关的导航。
 */
@Composable
fun AppMain(composeNavigator: AppComposeNavigator<AppScreen>) {
    val navHostController = rememberNavController()

    LaunchedEffect(Unit) {
        composeNavigator.handleNavigationCommands(navHostController)
    }

    AppNavHost(navHostController = navHostController)
}
