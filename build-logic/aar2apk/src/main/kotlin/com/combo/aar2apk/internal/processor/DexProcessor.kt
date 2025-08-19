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
    /**
     * @param allJarFiles 包含主模块和所有依赖库的 .jar 文件集合
     * @param rJavaSourcesDir aapt2 link 生成的 R.java 源码目录
     * @return 生成的 classes.dex 文件
     */
    fun process(
        allJarFiles: Set<File>,
        rJavaSourcesDir: File?,
        buildType: String,
        workDir: File
    ): File? {
        logger.log("步骤4: 编译Java/Kotlin代码并转换为DEX")
        val buildDir = File(workDir, "build")
        val rJavaFiles = rJavaSourcesDir?.walk()?.filter { it.isFile && it.name.endsWith(".java") }?.toList() ?: emptyList()

        // 如果没有任何代码，则跳过
        if (allJarFiles.isEmpty() && rJavaFiles.isEmpty()) {
            logger.log("⚠️ 未找到任何JAR或R.java文件，跳过DEX转换。")
            return null
        }

        // 编译 R.java (如果存在)
        val rClassesJar = if (rJavaFiles.isNotEmpty()) {
            compileRJava(rJavaSourcesDir!!, buildDir)
        } else null

        // 将编译后的 R.jar 和其他所有 jar 文件合并，一起转换为 DEX
        val jarsToDex = allJarFiles + listOfNotNull(rClassesJar)
        return convertToDex(jarsToDex, buildType, buildDir)
    }

    private fun compileRJava(sourceDir: File, buildDir: File): File {
        logger.log("  编译R.java文件...")
        val classesDir = File(buildDir, "r_classes")
        val rClassesJar = File(buildDir, "r_classes.jar")
        classesDir.deleteRecursively()
        classesDir.mkdirs()

        val javaFilePaths = sourceDir.walk()
            .filter { it.isFile && it.name.endsWith(".java") }
            .map { it.absolutePath }
            .toList()

        // 使用 javac 编译
        shellExecutor.execute(
            listOf(
                "javac", "-cp", sdkInfo.androidJar.absolutePath,
                "-d", classesDir.absolutePath,
                *javaFilePaths.toTypedArray()
            ),
            buildDir
        )

        // 将编译后的 .class 文件打成 jar 包
        shellExecutor.execute(
            listOf("jar", "cf", rClassesJar.absolutePath, "-C", classesDir.absolutePath, "."),
            buildDir
        )
        return rClassesJar
    }

    private fun convertToDex(jarFiles: Collection<File>, buildType: String, buildDir: File): File {
        logger.log("  使用 D8 将 ${jarFiles.size} 个 JAR 文件转换为 DEX...")
        val dexOutputDir = File(buildDir, "dex_output")
        dexOutputDir.deleteRecursively()
        dexOutputDir.mkdirs()

        val command = mutableListOf(
            sdkInfo.getTool("d8"),
            "--min-api", "21",
            "--output", dexOutputDir.absolutePath
        )
        if (buildType == "release") {
            command.add("--release")
        }
        jarFiles.forEach { command.add(it.absolutePath) }
        shellExecutor.execute(command)

        val classesDex = File(dexOutputDir, "classes.dex")
        if (!classesDex.exists()) throw IllegalStateException("DEX转换失败，未生成classes.dex文件。")
        return classesDex
    }
}