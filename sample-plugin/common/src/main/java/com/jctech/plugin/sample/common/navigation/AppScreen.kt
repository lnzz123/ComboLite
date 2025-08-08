package com.jctech.plugin.sample.common.navigation

import kotlinx.serialization.Serializable

/**
 * 应用屏幕路由定义
 *
 * 使用密封接口定义应用中所有可导航的屏幕
 * 采用此设计可提供类型安全的导航，并便于扩展
 */
sealed interface AppScreen {

  @Serializable
  data object Home : AppScreen


  @Serializable
  data object ScreenTwo : AppScreen


  @Serializable
  data object ScreenOne : AppScreen
}
