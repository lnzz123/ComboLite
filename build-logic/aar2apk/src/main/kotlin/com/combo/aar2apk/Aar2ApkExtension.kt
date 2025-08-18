package com.combo.aar2apk

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
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
 * Aar2Apk 插件的配置扩展 (DSL)
 */
abstract class Aar2ApkExtension @Inject constructor(objects: ObjectFactory) {

    /**
     * 需要转换为插件APK的模块列表 (例如: [":feature:home", ":feature:user"])
     */
    abstract val modules: ListProperty<String>

    /**
     * APK签名配置
     */
    val signingConfig: SigningConfig = objects.newInstance(SigningConfig::class.java)

    /**
     * 配置签名的便捷方法
     * e.g.,
     * aar2apk {
     * signing {
     * keystorePath.set(...)
     * }
     * }
     */
    fun signing(action: Action<SigningConfig>) {
        action.execute(signingConfig)
    }
}