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

package com.combo.plugin.sample.common.navigation

//noinspection SuspiciousImport
import android.R
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.geometry.Rect

// bounds变换动画（用于共享元素过渡）
val boundsTransform = { _: Rect, _: Rect -> tween<Rect>(550) }

// 动画持续时间常量
object AnimationDuration {
    const val SHORT = 300 // 短动画持续时间
    const val MEDIUM = 500 // 中等动画持续时间
    const val LONG = 800 // 长动画持续时间
}

/**
 * 导航动画配置
 * 提供常用的导航动画效果
 */
object NavigationAnimations {
    /**
     * 右滑入动画（从右侧滑入）
     * @param duration 动画持续时间
     */
    fun slideInFromRight(duration: Int = AnimationDuration.MEDIUM) =
        slideInHorizontally(
            animationSpec = tween(duration),
            initialOffsetX = { fullWidth -> fullWidth },
        )

    /**
     * 左滑出动画（向左侧滑出）
     * @param duration 动画持续时间
     */
    fun slideOutToLeft(duration: Int = AnimationDuration.MEDIUM) =
        slideOutHorizontally(
            animationSpec = tween(duration),
            targetOffsetX = { fullWidth -> -fullWidth },
        )

    /**
     * 左滑入动画（从左侧滑入）
     * @param duration 动画持续时间
     */
    fun slideInFromLeft(duration: Int = AnimationDuration.MEDIUM) =
        slideInHorizontally(
            animationSpec = tween(duration),
            initialOffsetX = { fullWidth -> -fullWidth },
        )

    /**
     * 右滑出动画（向右侧滑出）
     * @param duration 动画持续时间
     */
    fun slideOutToRight(duration: Int = AnimationDuration.MEDIUM) =
        slideOutHorizontally(
            animationSpec = tween(duration),
            targetOffsetX = { fullWidth -> fullWidth },
        )

    /**
     * 下滑入动画（从下方滑入）
     * @param duration 动画持续时间
     */
    fun slideInFromBottom(duration: Int = AnimationDuration.MEDIUM) =
        slideInVertically(
            animationSpec = tween(duration),
            initialOffsetY = { fullHeight -> fullHeight },
        )

    /**
     * 上滑出动画（向上方滑出）
     * @param duration 动画持续时间
     */
    fun slideOutToTop(duration: Int = AnimationDuration.MEDIUM) =
        slideOutVertically(
            animationSpec = tween(duration),
            targetOffsetY = { fullHeight -> -fullHeight },
        )

    /**
     * 淡入动画
     * @param duration 动画持续时间
     */
    fun fadeIn(duration: Int = AnimationDuration.MEDIUM) =
        androidx.compose.animation.fadeIn(
            animationSpec = tween(duration),
        )

    /**
     * 淡出动画
     * @param duration 动画持续时间
     */
    fun fadeOut(duration: Int = AnimationDuration.MEDIUM) =
        androidx.compose.animation.fadeOut(
            animationSpec = tween(duration),
        )

    /**
     * 根据导航过渡类型获取动画方案
     * @param transition 导航过渡类型
     * @return 对应的导航动画方案
     */
    fun getAnimationScheme(transition: NavigationTransition): NavigationAnimationScheme =
        when (transition) {
            NavigationTransition.SLIDE_IN_FROM_RIGHT -> NavigationAnimationSchemes.slideHorizontal
            NavigationTransition.SLIDE_IN_FROM_LEFT -> NavigationAnimationSchemes.slideFromLeft
            NavigationTransition.SLIDE_IN_FROM_BOTTOM -> NavigationAnimationSchemes.slideVertical
            NavigationTransition.SLIDE_IN_FROM_TOP -> NavigationAnimationSchemes.slideFromTop
            NavigationTransition.FADE_IN -> NavigationAnimationSchemes.fade
            NavigationTransition.SCALE_IN -> NavigationAnimationSchemes.fade // 简化为淡入淡出
            NavigationTransition.NONE -> NavigationAnimationSchemes.none
        }
}

/**
 * 预定义的导航动画方案
 * 提供常用的导航动画组合
 */
object NavigationAnimationSchemes {
    /**
     * 标准的左右滑动动画方案
     * 进入时从右滑入，退出时向左滑出
     */
    val slideHorizontal =
        object : NavigationAnimationScheme {
            override val enterAnim = R.anim.slide_in_left
            override val exitAnim = R.anim.slide_out_right
            override val popEnterAnim = R.anim.slide_in_left
            override val popExitAnim = R.anim.slide_out_right
        }

    /**
     * 从左侧滑入的动画方案
     */
    val slideFromLeft =
        object : NavigationAnimationScheme {
            override val enterAnim = R.anim.slide_in_left
            override val exitAnim = R.anim.slide_out_right
            override val popEnterAnim = R.anim.slide_in_left
            override val popExitAnim = R.anim.slide_out_right
        }

    /**
     * 标准的上下滑动动画方案
     * 进入时从下滑入，退出时向上滑出
     */
    val slideVertical =
        object : NavigationAnimationScheme {
            override val enterAnim = R.anim.slide_in_left
            override val exitAnim = R.anim.slide_out_right
            override val popEnterAnim = R.anim.slide_in_left
            override val popExitAnim = R.anim.slide_out_right
        }

    /**
     * 从顶部滑入的动画方案
     */
    val slideFromTop =
        object : NavigationAnimationScheme {
            override val enterAnim = R.anim.slide_in_left
            override val exitAnim = R.anim.slide_out_right
            override val popEnterAnim = R.anim.slide_in_left
            override val popExitAnim = R.anim.slide_out_right
        }

    /**
     * 淡入淡出动画方案
     * 进入和退出均使用淡入淡出效果
     */
    val fade =
        object : NavigationAnimationScheme {
            override val enterAnim = R.anim.fade_in
            override val exitAnim = R.anim.fade_out
            override val popEnterAnim = R.anim.fade_in
            override val popExitAnim = R.anim.fade_out
        }

    /**
     * 无动画方案
     * 不应用任何动画效果
     */
    val none =
        object : NavigationAnimationScheme {
            override val enterAnim = 0
            override val exitAnim = 0
            override val popEnterAnim = 0
            override val popExitAnim = 0
        }
}

/**
 * 导航动画方案接口
 * 定义导航动画方案的核心组件
 */
interface NavigationAnimationScheme {
    val enterAnim: Int
    val exitAnim: Int
    val popEnterAnim: Int
    val popExitAnim: Int
}
