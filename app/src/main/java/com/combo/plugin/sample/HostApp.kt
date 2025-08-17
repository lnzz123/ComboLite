/*
 *
 *  * Copyright (c) 2025, 贵州君城网络科技有限公司
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.combo.plugin.sample

import com.combo.core.BuildConfig
import com.combo.core.base.BaseHostApplication
import com.combo.core.manager.PluginManager
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber

/**
 * 主应用程序类
 * 该类是主应用程序的入口点，负责初始化插件框架和配置应用程序的全局状态
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
            ),
        )

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        loadKoinModules(
            module {
                viewModel { LoadingViewModel(applicationContext) }
            },
        )
    }
}
