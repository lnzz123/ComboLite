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

package com.jctech.plugin.core.di

import com.jctech.plugin.core.installer.InstallerManager
import com.jctech.plugin.core.installer.XmlManager
import com.jctech.plugin.core.manager.PluginManager
import com.jctech.plugin.core.resources.PluginResourcesManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 插件框架的Koin依赖注入模块
 *
 * 提供插件框架核心组件的单例依赖：
 * - PluginManager: 插件管理器
 * - PluginInstaller: 插件安装器
 * - InstallerXmlManager: XML配置管理器
 */
val pluginFrameworkModule = module {

    /**
     * 提供InstallerXmlManager单例
     * 负责管理插件的XML配置文件
     */
    single {
        XmlManager(androidApplication())
    }

    /**
     * 提供PluginInstaller单例
     * 负责插件的安装、卸载、更新操作
     * 依赖：InstallerXmlManager
     */
    single {
        InstallerManager(
            androidApplication(),
            get<XmlManager>(),
        )
    }

    // 插件资源管理器
    single<PluginResourcesManager> {
        PluginResourcesManager(androidContext())
    }

    /**
     * 提供PluginManager单例
     * 负责插件的整体管理，包括加载、启动、关闭等
     * 依赖：PluginInstaller, InstallerXmlManager
     */
    single {
        PluginManager(
            androidApplication(),
            get<XmlManager>(),
            get<InstallerManager>(),
            get<PluginResourcesManager>(),
        )
    }
}
