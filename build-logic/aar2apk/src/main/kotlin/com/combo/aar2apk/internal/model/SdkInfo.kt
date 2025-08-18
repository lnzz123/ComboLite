package com.combo.aar2apk.internal.model

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.io.Serializable

/**
 * 封装了构建所需的Android SDK信息
 */
data class SdkInfo(
    @get:Input
    val sdkPath: String,

    @get:Input
    val buildToolsVersion: String,

    @get:Input
    val platformVersion: String
) : Serializable {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    val androidJar: File by lazy {
        File(sdkPath, "platforms/$platformVersion/android.jar").also {
            if (!it.exists()) throw IllegalStateException("Android Platform JAR不存在: ${it.absolutePath}")
        }
    }

    fun getTool(name: String): String {
        val buildToolsDir = File(sdkPath, "build-tools/$buildToolsVersion")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val exeName = if (isWindows) "$name.exe" else name
        val toolFile = File(buildToolsDir, exeName)
        if (!toolFile.exists()) throw IllegalStateException("在Build Tools目录中找不到工具: $name")
        return toolFile.absolutePath
    }
}