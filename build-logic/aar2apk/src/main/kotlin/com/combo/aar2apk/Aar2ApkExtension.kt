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

package com.combo.aar2apk

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import javax.inject.Inject

/**
 * 插件签名配置
 */
abstract class SigningConfig {
    @get:Input
    abstract val keystorePath: Property<String>

    @get:Input
    abstract val keystorePassword: Property<String>

    @get:Input
    abstract val keyAlias: Property<String>

    @get:Input
    abstract val keyPassword: Property<String>
}

/**
 * 每个模块的精细化打包选项
 */
abstract class PackagingOptions @Inject constructor(
    @get:Input
    val path: String
) {
    /** 是否包含【所有】传递性依赖的【资源】 */
    @get:Input
    abstract val includeDependenciesRes: Property<Boolean>

    /** 是否包含【所有】传递性依赖的【代码 (dex)】 */
    @get:Input
    abstract val includeDependenciesDex: Property<Boolean>

    /** 是否包含【所有】传递性依赖的【Assets】 */
    @get:Input
    abstract val includeDependenciesAssets: Property<Boolean>

    /** 是否包含【所有】传递性依赖的【JNI so库】 */
    @get:Input
    abstract val includeDependenciesJni: Property<Boolean>

    init {
        // 默认所有选项都为 false，即默认采用“最小化”模式
        includeDependenciesRes.convention(false)
        includeDependenciesDex.convention(false)
        includeDependenciesAssets.convention(false)
        includeDependenciesJni.convention(false)
    }

    /** 一个便捷的辅助方法，用于一键开启所有依赖打包 */
    fun includeAllDependencies() {
        includeDependenciesRes.set(true)
        includeDependenciesDex.set(true)
        includeDependenciesAssets.set(true)
        includeDependenciesJni.set(true)
    }

    /** 内部辅助方法，判断是否需要任何一种依赖 */
    @Internal
    fun isAnyDependencyIncluded(): Boolean {
        return includeDependenciesRes.get() || includeDependenciesDex.get() ||
                includeDependenciesAssets.get() || includeDependenciesJni.get()
    }
}

/**
 * 用于在 DSL 中配置模块列表的容器
 */
abstract class ModuleConfigContainer {
    @get:Internal
    val modules =
        mutableListOf<PackagingOptions>() // **FIXED**: Renamed 'moduleConfigs' to 'modules'

    @Inject
    protected abstract fun getObjectFactory(): ObjectFactory

    /**
     * 配置一个模块
     * @param path 模块的 Gradle 路径 (e.g., ":feature:home")
     * @param action 一个配置块，用于设置该模块的打包选项
     */
    fun module(path: String, action: Action<PackagingOptions>) {
        val options = getObjectFactory().newInstance(PackagingOptions::class.java, path)
        action.execute(options)
        modules.add(options)
    }

    /**
     * 配置一个模块
     * @param path 模块的 Gradle 路径 (e.g., ":feature:home")
     */
    fun module(path: String) {
        val options = getObjectFactory().newInstance(PackagingOptions::class.java, path)
        modules.add(options)
    }
}

/**
 * Aar2Apk 插件的配置扩展 (DSL)
 */
abstract class Aar2ApkExtension @Inject constructor(objects: ObjectFactory) {
    val signingConfig: SigningConfig = objects.newInstance(SigningConfig::class.java)

    @get:Internal
    val moduleConfigs: ModuleConfigContainer =
        objects.newInstance(ModuleConfigContainer::class.java)

    fun signing(action: Action<SigningConfig>) {
        action.execute(signingConfig)
    }

    fun modules(action: Action<ModuleConfigContainer>) {
        action.execute(moduleConfigs)
    }
}