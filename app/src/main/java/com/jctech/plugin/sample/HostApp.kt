/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利。
 *
 * 本软件（包括但不限于代码、文档、资源文件等）受《中华人民共和国著作权法》及相关法律法规保护。
 * 未经本公司书面授权，任何单位或个人不得：
 * 1. 以任何形式复制、传播、修改、分发本软件的全部或部分内容；
 * 2. 将本软件用于商业目的或未经授权的第三方项目；
 * 3. 删除或篡改本软件中的版权声明、商标标识及技术标识。
 *
 * 违反上述条款者，本公司将依法追究其民事及刑事责任，并有权要求赔偿因此造成的全部经济损失。
 *
 * 授权许可请联系：贵州君城网络科技有限公司法律事务部
 * 邮箱：1755858138@qq.com
 * 电话：+86-175-85074415
 */

package com.jctech.plugin.sample

import com.jctech.plugin.core.BuildConfig
import com.jctech.plugin.core.base.BaseHostApplication
import com.jctech.plugin.core.manager.PluginManager
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber

/**
 * IHUB Compose应用入口
 *
 * 该类是IHUB应用的主Application入口，集成了完整的插件化框架。
 * 采用轻量级空壳应用设计，所有业务逻辑都通过插件动态加载。
 *
 * 主要功能：
 * - 初始化插件框架核心组件
 * - 配置和启动插件加载器
 * - 管理应用生命周期和插件协调
 * - 提供全局的依赖注入支持
 * - 处理异常情况和降级策略
 *
 * 插件化特性：
 * - 主Activity插件加载（compose-main）
 * - 功能插件动态加载（feature模块）
 * - 插件间通信机制
 * - 插件热更新和版本管理
 *
 * @author IHUB Plugin Framework
 * @since 2.0.0
 */
class HostApp : BaseHostApplication() {
    override fun onCreate() {
        super.onCreate()
        // 配置代理
        PluginManager.proxyManager.setHostActivity(HostActivity::class.java)
        PluginManager.proxyManager.setServicePool(
            listOf(
                HostService1::class.java,
                HostService2::class.java,
                HostService3::class.java,
                HostService4::class.java,
                HostService5::class.java,
                HostService6::class.java,
                HostService7::class.java,
                HostService8::class.java,
                HostService9::class.java,
                HostService10::class.java,
            )
        )

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        loadKoinModules(
            module {
                viewModel { LoadingViewModel(applicationContext) }
            }
        )
    }
}
