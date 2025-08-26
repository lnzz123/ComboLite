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

package com.combo.aar2apk.internal.utils

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.util.Properties

/**
 * 负责定位Android SDK及其工具
 */
internal object SdkLocator {

    fun getSdkPath(project: Project): String {
        System.getenv("ANDROID_HOME")?.let { return it }
        System.getenv("ANDROID_SDK_ROOT")?.let { return it }

        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            val props = Properties()
            localPropertiesFile.inputStream().use { props.load(it) }
            props.getProperty("sdk.dir")?.let { return it }
        }

        val userHome = System.getProperty("user.home")
        listOf(
            "$userHome/Library/Android/sdk", // macOS
            "$userHome/Android/Sdk", // Linux
            "$userHome/AppData/Local/Android/Sdk", // Windows
        ).firstOrNull { File(it).exists() }?.let { return it }

        throw GradleException("未能找到Android SDK。请设置 ANDROID_HOME 环境变量或在项目的 local.properties 文件中指定 sdk.dir。")
    }

    fun findLatestBuildTools(sdkPath: String): String {
        val buildToolsDir = File(sdkPath, "build-tools")
        if (!buildToolsDir.exists()) throw GradleException("Android Build-Tools 目录不存在: ${buildToolsDir.absolutePath}")
        return buildToolsDir.listFiles { file -> file.isDirectory }
            ?.mapNotNull { it.name }
            ?.maxOrNull()
            ?: throw GradleException("在 ${buildToolsDir.absolutePath} 中未找到任何 Build-Tools 版本。")
    }

    fun findLatestPlatform(sdkPath: String): String {
        val platformsDir = File(sdkPath, "platforms")
        if (!platformsDir.exists()) throw GradleException("Android Platforms 目录不存在: ${platformsDir.absolutePath}")
        return platformsDir.listFiles { file -> file.isDirectory && file.name.startsWith("android-") }
            ?.map { it.name }
            ?.maxByOrNull { it.substringAfter("android-").toIntOrNull() ?: 0 }
            ?: throw GradleException("在 ${platformsDir.absolutePath} 中未找到任何 Android Platform。")
    }
}