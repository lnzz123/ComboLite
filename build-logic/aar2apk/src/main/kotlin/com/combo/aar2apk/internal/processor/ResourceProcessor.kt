package com.combo.aar2apk.internal.processor

import com.combo.aar2apk.internal.model.SdkInfo
import com.combo.aar2apk.internal.utils.ShellExecutor
import com.combo.aar2apk.internal.utils.TaskLogger
import org.gradle.api.Project
import java.io.File

internal data class LinkedResources(val unsignedApk: File, val rJavaSourcesDir: File)

/**
 * 负责处理Android资源 (aapt2 compile, aapt2 link)
 */
internal class ResourceProcessor(
    private val project: Project,
    private val shellExecutor: ShellExecutor,
    private val sdkInfo: SdkInfo,
    private val logger: TaskLogger
) {
    fun process(
        extractDir: File,
        dependencyAars: Set<File>,
        packageId: String,
        workDir: File
    ): LinkedResources? {
        val manifestFile = File(extractDir, "AndroidManifest.xml")
        if (!manifestFile.exists()) {
            logger.log("⚠️ 未找到AndroidManifest.xml，跳过资源处理。")
            return null
        }

        val buildDir = File(workDir, "build")

        buildDir.mkdirs()

        // 步骤2.1: 编译主模块的资源
        val mainCompiledResDir = compileMainModuleResources(extractDir, buildDir)

        // 步骤2.2: 编译所有依赖库AAR的资源
        val dependencyCompiledResDirs = compileDependencyResources(dependencyAars, buildDir)

        // 步骤3: 链接所有编译后的资源
        val allCompiledResourceDirs = listOfNotNull(mainCompiledResDir) + dependencyCompiledResDirs
        return linkResources(allCompiledResourceDirs, manifestFile, packageId, buildDir)
    }

    private fun compileMainModuleResources(extractDir: File, buildDir: File): File? {
        val mainResDir = File(extractDir, "res")
        if (!mainResDir.exists() || mainResDir.listFiles()?.isEmpty() == true) {
            logger.log("步骤2.1: 主模块无资源文件，跳过aapt2 compile。")
            return null
        }
        logger.log("步骤2.1: 使用 aapt2 compile 编译主模块资源")
        val outputDir = File(buildDir, "compiled_res/main")
        compileResourceDir(mainResDir, outputDir)
        return outputDir
    }

    /**
     * 遍历、解压并编译所有依赖AAR中的资源。
     */
    private fun compileDependencyResources(dependencyAars: Set<File>, buildDir: File): List<File> {
        if (dependencyAars.isEmpty()) return emptyList()

        logger.log("步骤2.2: 编译 ${dependencyAars.size} 个依赖库的资源")
        val unzipDir = File(buildDir, "unzip_deps")
        val compiledDir = File(buildDir, "compiled_res/deps")

        return dependencyAars.mapNotNull { aarFile ->
            val libName = aarFile.nameWithoutExtension
            val specificUnzipDir = File(unzipDir, libName)
            specificUnzipDir.deleteRecursively()
            specificUnzipDir.mkdirs()

            project.copy {
                from(project.zipTree(aarFile))
                into(specificUnzipDir)
            }

            val resDir = File(specificUnzipDir, "res")
            if (resDir.exists() && resDir.isDirectory && resDir.listFiles()?.isNotEmpty() == true) {
                val outputDir = File(compiledDir, libName)
                compileResourceDir(resDir, outputDir)
                outputDir
            } else {
                null
            }
        }
    }

    /**
     * 编译单个资源目录
     */
    private fun compileResourceDir(resDir: File, outputDir: File) {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        shellExecutor.execute(
            listOf(
                sdkInfo.getTool("aapt2"), "compile",
                "--dir", resDir.absolutePath,
                "-o", outputDir.absolutePath
            )
        )
    }

    /**
     * 链接所有编译后的资源
     */
    private fun linkResources(
        compiledResourceDirs: List<File>,
        manifestFile: File,
        packageId: String,
        buildDir: File
    ): LinkedResources {
        logger.log("步骤3: 使用 aapt2 link 链接所有资源并生成R.java")
        val rJavaSourcesDir = File(buildDir, "gen_java")
        val unsignedApk = File(buildDir, "unsigned.apk")

        val allFlatFiles = compiledResourceDirs.flatMap { dir ->
            dir.walk().filter { file -> file.isFile && file.name.endsWith(".flat") }
        }

        val responseFile = File(buildDir, "aapt2-R-files")
        responseFile.writeText(
            allFlatFiles.joinToString(separator = System.lineSeparator()) { it.absolutePath }
        )

        val command = mutableListOf(
            sdkInfo.getTool("aapt2"), "link",
            "-o", unsignedApk.absolutePath,
            "-I", sdkInfo.androidJar.absolutePath,
            "--manifest", manifestFile.absolutePath,
            "--java", rJavaSourcesDir.absolutePath,
            "--package-id", packageId,
            "--auto-add-overlay",
            "--no-version-vectors",
            "--no-static-lib-packages"
        )

        command.add("-R")
        command.add("@${responseFile.absolutePath}")

        shellExecutor.execute(command)
        return LinkedResources(unsignedApk, rJavaSourcesDir)
    }
}