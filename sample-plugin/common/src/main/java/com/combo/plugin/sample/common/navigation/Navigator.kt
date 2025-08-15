package com.combo.plugin.sample.common.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription

/**
 * 导航过渡动画枚举
 *
 * 定义支持的导航动画类型
 */
enum class NavigationTransition {
    /** 从右侧滑入 */
    SLIDE_IN_FROM_RIGHT,

    /** 从左侧滑入 */
    SLIDE_IN_FROM_LEFT,

    /** 从底部滑入 */
    SLIDE_IN_FROM_BOTTOM,

    /** 从顶部滑入 */
    SLIDE_IN_FROM_TOP,

    /** 淡入 */
    FADE_IN,

    /** 缩放 */
    SCALE_IN,

    /** 无动画 */
    NONE,
}

/**
 * 导航启动模式枚举
 *
 * 定义导航的不同启动行为
 */
enum class LaunchMode {
    /** 标准模式：每次导航都创建新实例 */
    STANDARD,

    /** 单顶模式：如果目标已在栈顶，则复用 */
    SINGLE_TOP,

    /** 单任务模式：如果目标存在于栈中，则清除其上所有页面 */
    SINGLE_TASK,

    /** 清空任务模式：清除所有页面，创建新实例 */
    CLEAR_TASK,
}

/**
 * 导航器抽象基类
 *
 * 提供基础导航功能和状态管理，作为所有导航器的基础类
 */
abstract class Navigator {
    /**
     * 导航命令共享流
     *
     * 用于发送导航命令给导航控制器，使用额外缓冲区以避免背压问题
     */
    val navigationCommands =
        MutableSharedFlow<NavigationCommand>(extraBufferCapacity = Int.MAX_VALUE)

    /**
     * 导航控制器状态流
     *
     * 使用StateFlow允许ViewModel在初始组合之前开始观察导航结果，
     * 并且仍然能够获取到导航结果
     */
    val navControllerFlow = MutableStateFlow<NavController?>(null)

    /**
     * 防止重复导航的变量
     */
    private var lastNavigationTime = 0L
    private val navigationThrottleTime = 500L // 防重复点击的时间间隔（毫秒）

    /**
     * 检查是否可以执行导航（防止重复点击）
     *
     * @return 如果可以导航返回true，否则返回false
     */
    fun canNavigate(): Boolean {
        val currentTime = System.currentTimeMillis()
        val canNav = currentTime - lastNavigationTime >= navigationThrottleTime
        if (canNav) {
            lastNavigationTime = currentTime
        }
        return canNav
    }

    /**
     * 导航返回上一页
     */
    fun navigateUp() {
        if (canNavigate()) {
            navigationCommands.tryEmit(NavigationCommand.NavigateUp)
        }
    }

    /**
     * 导航到根页面
     */
    fun navigateToRoot() {
        if (canNavigate()) {
            navigationCommands.tryEmit(NavigationCommand.NavigateToRoot)
        }
    }

    /**
     * 导航返回指定步数
     *
     * @param steps 返回的步数，默认为1
     */
    fun navigateBack(steps: Int = 1) {
        if (canNavigate()) {
            navigationCommands.tryEmit(NavigationCommand.NavigateBack(steps))
        }
    }
}

/**
 * 应用Compose导航器抽象类
 *
 * 为Jetpack Compose导航提供类型安全的导航接口
 *
 * @param T 路由类型，必须是Any类型
 */
abstract class AppComposeNavigator<T : Any> : Navigator() {
    /**
     * 导航到指定路由
     *
     * @param route 目标路由
     * @param optionsBuilder 导航选项构建器，可选
     */
    abstract fun navigate(
        route: T,
        optionsBuilder: (NavOptionsBuilder.() -> Unit)? = null,
    )

    /**
     * 基于启动模式进行导航
     *
     * @param route 目标路由
     * @param launchMode 启动模式
     * @param transition 过渡动画类型
     * @param popUpTo 返回到指定路由
     * @param popUpToInclusive 是否包含目标路由
     * @param restoreState 是否恢复状态
     */
    fun navigateWithMode(
        route: T,
        launchMode: LaunchMode = LaunchMode.STANDARD,
        transition: NavigationTransition = NavigationTransition.SLIDE_IN_FROM_RIGHT,
        popUpTo: T? = null,
        popUpToInclusive: Boolean = false,
        restoreState: Boolean = false,
    ) {
        if (!canNavigate()) return

        // 获取动画方案
        val animationScheme = NavigationAnimations.getAnimationScheme(transition)

        // 根据启动模式构建导航选项
        val options =
            navOptions {
                when (launchMode) {
                    LaunchMode.STANDARD -> {
                        // 标准模式，不做特殊处理
                    }

                    LaunchMode.SINGLE_TOP -> {
                        launchSingleTop = true
                    }

                    LaunchMode.SINGLE_TASK -> {
                        popUpTo(route) {
                            inclusive = true
                        }
                    }

                    LaunchMode.CLEAR_TASK -> {
                        popUpTo(0)
                    }
                }

                popUpTo?.let { popUpToRoute ->
                    popUpTo(popUpToRoute) {
                        inclusive = popUpToInclusive
                    }
                }

                this.restoreState = restoreState

                // 设置动画
                anim {
                    enter = animationScheme.enterAnim
                    exit = animationScheme.exitAnim
                    popEnter = animationScheme.popEnterAnim
                    popExit = animationScheme.popExitAnim
                }
            }

        // 发送导航命令
        navigationCommands.tryEmit(ComposeNavigationCommand.NavigateToRoute(route, options))
    }

    /**
     * 返回并传递结果
     *
     * @param key 结果的键名
     * @param result 结果数据
     * @param route 目标路由，可选
     */
    abstract fun <R> navigateBackWithResult(
        key: String,
        result: R,
        route: T?,
    )

    /**
     * 设置结果并返回
     *
     * @param resultKey 结果的键名
     * @param result 结果数据
     * @param targetRoute 目标路由，可选
     */
    fun <R> setResultAndNavigateBack(
        resultKey: String,
        result: R,
        targetRoute: T? = null,
    ) {
        if (canNavigate()) {
            navigateBackWithResult(resultKey, result, targetRoute)
        }
    }

    /**
     * 带参数跳转并监听结果
     *
     * 导航到目标页面并返回一个Flow用于监听结果
     *
     * @param route 目标路由
     * @param resultKey 结果的键名
     * @param optionsBuilder 导航选项构建器，可选
     * @return 包含结果数据的Flow
     */
    inline fun <reified R> navigateForResult(
        route: T,
        resultKey: String,
        noinline optionsBuilder: (NavOptionsBuilder.() -> Unit)? = null,
    ): Flow<R> {
        if (canNavigate()) {
            navigate(route, optionsBuilder)
        }

        return navControllerFlow
            .filterNotNull()
            .map { navController ->
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getLiveData<R>(resultKey)
                    ?.value
            }.filterNotNull()
    }

    /**
     * 弹出到指定路由
     *
     * @param route 目标路由
     * @param inclusive 是否包含目标路由，如果为true则目标路由也会被弹出
     */
    abstract fun popUpTo(
        route: T,
        inclusive: Boolean,
    )

    /**
     * 导航到新页面并清空回退栈
     *
     * @param route 目标路由
     */
    abstract fun navigateAndClearBackStack(route: T)

    /**
     * 替换当前页面
     *
     * @param route 目标路由，将替换当前页面
     */
    abstract fun replace(route: T)

    /**
     * 导航到新页面并关闭当前页面
     *
     * @param route 目标路由
     */
    abstract fun navigateAndFinish(route: T)

    /**
     * 检查当前是否可以返回
     *
     * @return 如果可以返回则为true
     */
    fun canNavigateBack(): Boolean = navControllerFlow.value?.previousBackStackEntry != null

    /**
     * 处理导航命令
     *
     * 此方法应在Composable中被调用以连接NavController和导航命令
     *
     * @param navController 导航控制器
     */
    suspend fun handleNavigationCommands(navController: NavController) {
        navigationCommands
            .onSubscription { this@AppComposeNavigator.navControllerFlow.value = navController }
            .onCompletion { this@AppComposeNavigator.navControllerFlow.value = null }
            .collect { navController.handleComposeNavigationCommand(it) }
    }

    /**
     * 处理导航命令的NavController扩展函数
     *
     * 根据不同的导航命令类型执行相应的导航操作
     *
     * @param navigationCommand 要处理的导航命令
     */
    private fun NavController.handleComposeNavigationCommand(navigationCommand: NavigationCommand) {
        when (navigationCommand) {
            is ComposeNavigationCommand.NavigateToRoute<*> -> {
                navigate(navigationCommand.route, navigationCommand.options)
            }

            NavigationCommand.NavigateUp -> navigateUp()

            NavigationCommand.NavigateToRoot -> {
                // 导航到根页面
                popBackStack(0, false)
            }

            is NavigationCommand.NavigateBack -> {
                // 回退指定步数
                repeat(navigationCommand.steps) {
                    if (previousBackStackEntry != null) {
                        navigateUp()
                    }
                }
            }

            is ComposeNavigationCommand.PopUpToRoute<*> ->
                popBackStack(
                    navigationCommand.route,
                    navigationCommand.inclusive,
                )

            is ComposeNavigationCommand.NavigateUpWithResult<*, *> -> {
                navUpWithResult(navigationCommand)
            }

            is ComposeNavigationCommand.ReplaceRoute<*> -> {
                // 替换当前页面
                val options =
                    navigationCommand.options ?: navOptions {
                        currentDestination?.let { destination ->
                            popUpTo(destination.id) {
                                inclusive = true
                            }
                        }
                    }
                navigate(navigationCommand.route, options)
            }

            is ComposeNavigationCommand.NavigateAndClearBackStack<*> -> {
                // 清空栈并导航
                val options =
                    navOptions {
                        popUpTo(0)
                    }
                navigate(navigationCommand.route, options)
            }

            is ComposeNavigationCommand.NavigateAndFinishCurrent<*> -> {
                // 关闭当前页面并导航
                val options =
                    navigationCommand.options ?: navOptions {
                        currentDestination?.let { destination ->
                            popUpTo(destination.id) {
                                inclusive = true
                            }
                        }
                    }
                navigate(navigationCommand.route, options)
            }
        }
    }

    /**
     * 返回并传递结果的NavController扩展函数
     *
     * @param navigationCommand 包含结果信息的导航命令
     */
    private fun NavController.navUpWithResult(navigationCommand: ComposeNavigationCommand.NavigateUpWithResult<*, *>) {
        val backStackEntry =
            navigationCommand.route?.let { route ->
                // 尝试获取特定路由的back stack entry
                try {
                    currentBackStackEntry
                } catch (e: Exception) {
                    null
                }
            } ?: previousBackStackEntry

        backStackEntry?.savedStateHandle?.set(
            navigationCommand.key,
            navigationCommand.result,
        )

        navigationCommand.route?.let { route ->
            try {
                popBackStack(route, false)
            } catch (e: Exception) {
                navigateUp()
            }
        } ?: navigateUp()
    }
}
