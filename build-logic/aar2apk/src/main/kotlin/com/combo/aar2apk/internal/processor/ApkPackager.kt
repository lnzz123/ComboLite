package com.combo.aar2apk.internal.processor

import com.combo.aar2apk.internal.utils.ShellExecutor
import com.combo.aar2apk.internal.utils.TaskLogger
import java.io.File

/**
 * 负责将各类文件打包进APK
 */
internal class ApkPackager(
    private val shellExecutor: ShellExecutor,
    private val logger: TaskLogger
) {
    fun addDex(apkFile: File, dexFile: File?) {
        if (dexFile == null || !dexFile.exists()) return
        logger.log("步骤5.1: 添加DEX文件到APK")
        shellExecutor.execute(
            listOf("jar", "uf", apkFile.absolutePath, "-C", dexFile.parent, dexFile.name)
        )
    }

    fun addNativeLibs(apkFile: File, extractDir: File) {
        val jniDir = File(extractDir, "jni")
        if (!jniDir.exists() || jniDir.listFiles()?.isEmpty() == true) return
        logger.log("步骤5.2: 添加Native库到APK")

        // aapt2 link 命令会自动处理 jni 目录，但如果需要手动添加，逻辑如下
        // 我们这里使用 jar 命令确保内容被添加
        val tempLibDir = File(apkFile.parentFile, "temp_lib")
        jniDir.copyRecursively(File(tempLibDir, "lib"))

        shellExecutor.execute(
            listOf("jar", "uf", apkFile.absolutePath, "-C", tempLibDir.absolutePath, "lib")
        )
        tempLibDir.deleteRecursively()
    }

    fun addAssets(apkFile: File, extractDir: File) {
        val assetsDir = File(extractDir, "assets")
        if (!assetsDir.exists() || assetsDir.listFiles()?.isEmpty() == true) return
        logger.log("步骤5.3: 添加Assets到APK")
        shellExecutor.execute(
            listOf("jar", "uf", apkFile.absolutePath, "-C", extractDir.absolutePath, "assets")
        )
    }
}