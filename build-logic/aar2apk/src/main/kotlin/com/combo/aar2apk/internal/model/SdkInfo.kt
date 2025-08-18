package com.combo.aar2apk.internal.model

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal // <-- [修复] 导入 @Internal 注解
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

    /**
     * 获取指定名称的构建工具路径
     */
    fun getTool(name: String): String {
        val buildToolsDir = File(sdkPath, "build-tools/$buildToolsVersion")

        if (System.getProperty("os.name").lowercase().contains("windows")) {
            val exeFile = File(buildToolsDir, "$name.exe")
            if (exeFile.exists()) return exeFile.absolutePath

            val batFile = File(buildToolsDir, "$name.bat")
            if (batFile.exists()) return batFile.absolutePath
        } else {
            val toolFile = File(buildToolsDir, name)
            if (toolFile.exists()) return toolFile.absolutePath
        }

        throw IllegalStateException("在Build Tools目录 '$buildToolsDir' 中找不到工具: '$name'")
    }
}