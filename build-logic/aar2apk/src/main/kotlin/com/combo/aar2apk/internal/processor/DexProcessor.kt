package com.combo.aar2apk.internal.processor

import com.combo.aar2apk.internal.model.SdkInfo
import com.combo.aar2apk.internal.utils.ShellExecutor
import com.combo.aar2apk.internal.utils.TaskLogger
import java.io.File

/**
 * 负责编译Java/Kotlin代码并转换为DEX (javac, d8)
 */
internal class DexProcessor(
    private val shellExecutor: ShellExecutor,
    private val sdkInfo: SdkInfo,
    private val logger: TaskLogger
) {
    fun process(
        extractDir: File,
        rJavaSourcesDir: File?,
        buildType: String,
        workDir: File
    ): File? {
        logger.log("步骤4: 编译Java/Kotlin代码并转换为DEX")
        val buildDir = File(workDir, "build")
        val jarFiles = findJarFiles(extractDir)
        val rJavaFiles = rJavaSourcesDir?.walk()?.filter { it.isFile && it.name.endsWith(".java") }?.toList() ?: emptyList()

        if (jarFiles.isEmpty() && rJavaFiles.isEmpty()) {
            logger.log("⚠️ 未找到JAR或R.java文件，跳过DEX转换。")
            return null
        }

        val rClassesJar = if (rJavaFiles.isNotEmpty()) {
            compileRJava(rJavaSourcesDir!!, buildDir)
        } else null

        val allJars = jarFiles + listOfNotNull(rClassesJar)
        return convertToDex(allJars, buildType, buildDir)
    }

    private fun findJarFiles(extractDir: File): List<File> {
        val jars = mutableListOf<File>()
        File(extractDir, "classes.jar").takeIf { it.exists() }?.let { jars.add(it) }
        File(extractDir, "libs").takeIf { it.exists() }?.walk()
            ?.filter { it.isFile && it.extension.equals("jar", true) }
            ?.forEach { jars.add(it) }
        return jars.distinct()
    }

    private fun compileRJava(sourceDir: File, buildDir: File): File {
        logger.log("  编译R.java文件...")
        val classesDir = File(buildDir, "r_classes")
        val rClassesJar = File(buildDir, "r_classes.jar")
        classesDir.deleteRecursively()
        classesDir.mkdirs()

        val javaFiles = sourceDir.walk().filter { it.isFile && it.name.endsWith(".java") }.joinToString(" ") { it.absolutePath }

        shellExecutor.execute(
            listOf(
                "javac", "-cp", sdkInfo.androidJar.absolutePath,
                "-d", classesDir.absolutePath,
                *javaFiles.split(" ").toTypedArray()
            ),
            buildDir
        )

        shellExecutor.execute(
            listOf("jar", "cf", rClassesJar.absolutePath, "-C", classesDir.absolutePath, "."),
            buildDir
        )
        return rClassesJar
    }

    private fun convertToDex(jarFiles: List<File>, buildType: String, buildDir: File): File {
        logger.log("  使用 D8 将JAR文件转换为DEX...")
        val command = mutableListOf(
            sdkInfo.getTool("d8"),
            "--min-api", "21",
            "--output", buildDir.absolutePath
        )
        if (buildType == "release") {
            command.add("--release")
        }
        jarFiles.forEach { command.add(it.absolutePath) }
        shellExecutor.execute(command)

        val classesDex = File(buildDir, "classes.dex")
        if (!classesDex.exists()) throw IllegalStateException("DEX转换失败，未生成classes.dex文件。")
        return classesDex
    }
}