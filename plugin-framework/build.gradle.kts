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

import java.io.ByteArrayOutputStream
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ==================== 统一配置区域 ====================

// 1. 在这里配置你的所有插件模块路径
val pluginModules =
    listOf(
        ":sample-plugin:common",
        ":sample-plugin:home",
        ":sample-plugin:guide",
        ":sample-plugin:example",
        ":sample-plugin:setting",
    )

// 2. Package ID配置 - 为每个插件分配独立的package ID避免资源冲突
val pluginPackageIds =
    pluginModules
        .mapIndexed { index, modulePath ->
            val packageId = String.format("0x%02x", 0x80 + index)
            modulePath to packageId
        }.toMap()

// 3. 签名配置
val keystorePathProvider =
    project.providers.provider {
        project.rootProject.file("jctech.jks").absolutePath
    }
val keystorePasswordProvider = project.providers.provider { "he1755858138" }
val keyAliasProvider = project.providers.provider { "jctech" }
val keyPasswordProvider = project.providers.provider { "he1755858138" }

// ==================== 自定义任务定义 ====================

/**
 * AAR到插件APK转换任务基类
 */
abstract class ConvertAarToApkTask : DefaultTask() {
    @get:Input
    abstract val pluginName: Property<String>

    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val aarFilePath: Property<String>

    @get:Input
    abstract val keystorePath: Property<String>

    @get:Input
    abstract val keystorePassword: Property<String>

    @get:Input
    abstract val keyAlias: Property<String>

    @get:Input
    abstract val keyPassword: Property<String>

    @get:Input
    abstract val androidSdkPath: Property<String>

    @get:Input
    abstract val buildToolsVersion: Property<String>

    @get:Input
    abstract val androidPlatform: Property<String>

    @get:Input
    abstract val buildType: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val buildTimestamp: Property<String>

    @get:Input
    abstract val packageId: Property<String>

    private lateinit var logWriter: PrintWriter
    private lateinit var logFile: File

    @TaskAction
    fun execute() {
        executeAarBuild()
        val aarFile = project.file(aarFilePath.get())
        if (!aarFile.exists()) {
            throw GradleException("找不到文件: ${aarFile.absolutePath}")
        }

        val workDir = temporaryDir
        workDir.mkdirs()

        initializeTaskLogging(aarFile)

        val extractDir = File(workDir, "extracted")
        val buildDir = File(workDir, "build")
        val sourceDir = File(buildDir, "java")
        extractDir.mkdirs()
        buildDir.mkdirs()
        sourceDir.mkdirs()

        logMessage("🚀 开始转换: ${aarFile.name} (来自模块 ${pluginName.get()})")
        logMessage("工作目录: ${workDir.absolutePath}")

        try {
            // 现在直接从解压开始
            extractAarContents(aarFile, extractDir)

            val manifestFile = File(extractDir, "AndroidManifest.xml")
            if (!manifestFile.exists()) {
                logMessage("⚠️ 未找到AndroidManifest.xml，跳过构建（纯代码库）")
                return
            }

            compileResourcesAndGenerateR(extractDir, buildDir, sourceDir, manifestFile)
            compileJavaSourcesToDex(extractDir, buildDir, sourceDir)
            buildUnsignedApk(buildDir, manifestFile)
            val unsignedApk = File(buildDir, "unsigned.apk")
            addDexToApk(unsignedApk, buildDir)
            addNativeLibrariesToApk(unsignedApk, extractDir, buildDir)
            addAssetsToApk(unsignedApk, extractDir, buildDir)

            val buildTypeValue = buildType.get()
            val apkFileName = "${pluginName.get()}-$buildTypeValue.apk"
            val signedApk = File(outputDirectory.get().asFile, apkFileName)
            signFinalApk(unsignedApk, signedApk)

            logMessage("✅ 转换成功! APK大小: ${signedApk.length() / 1024} KB")
            logMessage("APK文件: ${signedApk.absolutePath}")
        } catch (e: Exception) {
            logMessage("❌ 转换失败: ${e.message}")
            logWriter.println("错误详情: ${e.stackTraceToString()}")
            throw e
        } finally {
            closeTaskLogging()
        }
    }

    // ==================== 日志管理 ====================
    private fun initializeTaskLogging(aarFile: File) {
        val logDir =
            File(
                project.layout.buildDirectory
                    .get()
                    .asFile,
                "logs",
            )
        logDir.mkdirs()

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val aarName = aarFile.nameWithoutExtension
        val buildTypeValue = buildType.get()
        logFile = File(logDir, "aar-to-apk-$aarName-$buildTypeValue-$timestamp.log")

        logWriter = PrintWriter(FileWriter(logFile, true))
        logWriter.println("==================== AAR转APK构建日志 ====================")
        logWriter.println("开始时间: ${LocalDateTime.now()}")
        logWriter.println("构建类型: ${buildTypeValue.uppercase()}")
        logWriter.println("AAR文件: ${aarFile.name}")
        logWriter.println("输出目录: ${outputDirectory.get().asFile.absolutePath}")
        logWriter.println("========================================================")
        logWriter.flush()
    }

    // ... (其他日志方法保持不变)
    private fun logMessage(message: String) {
        val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val logMessage = "[$timeStamp] $message"

        logger.lifecycle(logMessage)
        if (::logWriter.isInitialized) {
            logWriter.println(logMessage)
            logWriter.flush()
        }
    }

    private fun closeTaskLogging() {
        if (::logWriter.isInitialized) {
            logWriter.println("========================================================")
            logWriter.println("结束时间: ${LocalDateTime.now()}")
            logWriter.println("日志文件: ${logFile.absolutePath}")
            logWriter.close()

            logger.lifecycle("构建日志已保存到: ${logFile.absolutePath}")
        }
    }

    // ==================== 工具方法 ====================

    // ******** 恢复你的 executeAarBuild 方法 ********
    private fun executeAarBuild() {
        val buildTypeValue = buildType.get()
        val modulePathValue = modulePath.get()
        logMessage("开始通过独立Gradle进程构建模块 AAR: $modulePathValue ($buildTypeValue)")

        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val rootDir = project.rootProject.projectDir
        val gradlewFile = if (isWindows) File(rootDir, "gradlew.bat") else File(rootDir, "gradlew")

        if (!gradlewFile.exists()) {
            throw RuntimeException("Gradle Wrapper 文件不存在: ${gradlewFile.absolutePath}")
        }

        val assembleTask = "assemble${buildTypeValue.replaceFirstChar { it.uppercase() }}"

        val command = listOf(gradlewFile.absolutePath, "$modulePathValue:$assembleTask")

        logMessage("执行构建命令: ${command.joinToString(" ")}")

        try {
            executeShellCommand(command, rootDir)
            logMessage("✅ AAR 构建子进程执行成功")
        } catch (e: Exception) {
            logMessage("❌ AAR 构建子进程执行失败: ${e.message}")
            // 即使命令失败，也继续，让后续的文件检查来决定是否中断
        }
    }

    private fun getAndroidSdkTool(toolName: String): String {
        val buildToolsDir = File(androidSdkPath.get(), "build-tools/${buildToolsVersion.get()}")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val exeSuffix = if (isWindows) ".exe" else ""
        val batSuffix = if (isWindows) ".bat" else ""

        val exeFile = File(buildToolsDir, "$toolName$exeSuffix")
        if (exeFile.exists()) return exeFile.absolutePath

        val batFile = File(buildToolsDir, "$toolName$batSuffix")
        if (batFile.exists()) return batFile.absolutePath

        throw RuntimeException("在Build Tools目录中找不到工具: $toolName")
    }

    private fun executeShellCommand(
        command: List<String>,
        workDir: File? = null,
    ) {
        val cmdStr = command.joinToString(" ")
        logMessage("执行: ${command[0]} ${command.drop(1).joinToString(" ")}")

        if (::logWriter.isInitialized) {
            logWriter.println("完整命令: $cmdStr")
            workDir?.let { logWriter.println("工作目录: ${it.absolutePath}") }
        }

        val processBuilder = ProcessBuilder(command)
        workDir?.let { processBuilder.directory(it) }

        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()

        val output = ByteArrayOutputStream()
        process.inputStream.transferTo(output)

        val exitCode = process.waitFor()
        val outputStr = output.toString()

        if (::logWriter.isInitialized && outputStr.isNotBlank()) {
            logWriter.println("合并输出 (stdout + stderr): \n$outputStr")
        }

        if (exitCode != 0) {
            if (::logWriter.isInitialized) {
                logWriter.println("命令执行失败，退出码: $exitCode")
                logWriter.flush()
            }
            throw RuntimeException("命令执行失败 (退出码: $exitCode): ${command[0]}\n输出详情:\n$outputStr")
        }
        if (::logWriter.isInitialized) logWriter.flush()
    }

    // ==================== 核心处理方法 ====================
    private fun extractAarContents(
        aar: File,
        targetDir: File,
    ) {
        logMessage("步骤1: 解压AAR文件")
        project.copy {
            from(project.zipTree(aar))
            into(targetDir)
        }
        logMessage("✅ AAR文件解压完成")
    }

    private fun compileResourcesAndGenerateR(
        extractDir: File,
        buildDir: File,
        sourceDir: File,
        manifestFile: File,
    ) {
        val resDir = File(extractDir, "res")
        if (!resDir.exists() || !resDir.isDirectory) {
            logMessage("步骤2: 跳过资源编译（未找到res目录）")
            return
        }

        val pluginPackageId = packageId.get()
        logMessage("步骤2: 编译资源文件并生成R.java (Package ID: $pluginPackageId)")
        val compiledResDir = File(buildDir, "compiled_res")
        compiledResDir.deleteRecursively()
        compiledResDir.mkdirs()

        try {
            val resourceFiles =
                resDir.walkTopDown().filter { it.isFile && !it.name.startsWith(".") }.toList()
            if (resourceFiles.isEmpty()) {
                logMessage("res目录中无资源文件，跳过编译")
                return
            }

            logMessage("找到${resourceFiles.size}个资源文件，开始编译")

            resourceFiles.forEach { resourceFile ->
                try {
                    logWriter.println("编译资源: ${resourceFile.name}")
                    executeShellCommand(
                        listOf(
                            getAndroidSdkTool("aapt2"),
                            "compile",
                            resourceFile.absolutePath,
                            "-o",
                            compiledResDir.absolutePath,
                        ),
                    )
                } catch (e: Exception) {
                    logWriter.println("跳过有问题的资源: ${resourceFile.name} - ${e.message}")
                }
            }

            val androidJar =
                File(androidSdkPath.get(), "platforms/${androidPlatform.get()}/android.jar")
            if (!androidJar.exists()) {
                throw RuntimeException("Android Platform JAR不存在: ${androidJar.absolutePath}")
            }

            val rTxtFile = File(buildDir, "R.txt")
            val linkCommand =
                mutableListOf(
                    getAndroidSdkTool("aapt2"),
                    "link",
                    "-I",
                    androidJar.absolutePath,
                    "--manifest",
                    manifestFile.absolutePath,
                    "--auto-add-overlay",
                    "--no-version-vectors",
                    "--emit-ids",
                    rTxtFile.absolutePath,
                    "--java",
                    sourceDir.absolutePath,
                    "--package-id",
                    pluginPackageId,
                )

            val flatFiles =
                compiledResDir
                    .walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".flat") }
                    .toList()
            if (flatFiles.isNotEmpty()) {
                logMessage("添加${flatFiles.size}个编译后的资源")
                flatFiles.forEach { file ->
                    linkCommand.add("-R")
                    linkCommand.add(file.absolutePath)
                }
            }

            val tempApk = File(buildDir, "temp_for_r.apk")
            linkCommand.addAll(listOf("-o", tempApk.absolutePath))

            executeShellCommand(linkCommand)

            val generatedRFiles =
                sourceDir.walkTopDown().filter { it.isFile && it.name == "R.java" }.toList()
            if (generatedRFiles.isNotEmpty()) {
                logMessage("✅ 资源编译完成，生成了${generatedRFiles.size}个R.java文件")
            } else {
                logMessage("⚠️ 未生成R.java文件，但资源编译完成")
            }
        } catch (e: Exception) {
            logMessage("⚠️ 资源编译失败，将跳过资源：${e.message}")
            compiledResDir.deleteRecursively()
            compiledResDir.mkdirs()
        }
    }

    private fun compileJavaSourcesToDex(
        extractDir: File,
        buildDir: File,
        sourceDir: File,
    ) {
        logMessage("步骤3: 编译Java源代码为DEX")

        val jarFiles = mutableListOf<File>()
        val classesJar = File(extractDir, "classes.jar")
        if (classesJar.exists()) jarFiles.add(classesJar)

        val libsDir = File(extractDir, "libs")
        if (libsDir.exists() && libsDir.isDirectory) {
            libsDir
                .listFiles { file ->
                    file.isFile &&
                            file.extension.equals(
                                "jar",
                                ignoreCase = true,
                            )
                }?.forEach { jarFiles.add(it) }
        }

        extractDir
            .listFiles { file -> file.isFile && file.name.matches(Regex("classes\\d*\\.jar")) }
            ?.forEach {
                if (!jarFiles.contains(it)) jarFiles.add(it)
            }

        val rJavaFiles =
            sourceDir.walkTopDown().filter { it.isFile && it.name == "R.java" }.toList()
        val needCompileRJava = rJavaFiles.isNotEmpty()

        if (jarFiles.isEmpty() && !needCompileRJava) {
            logMessage("⚠️ 未找到JAR文件或R.java文件，跳过DEX转换")
            return
        }

        logMessage("找到${jarFiles.size}个JAR文件${if (needCompileRJava) "和${rJavaFiles.size}个R.java文件" else ""}，开始DEX转换")

        if (needCompileRJava) {
            compileRJavaToClasses(sourceDir, buildDir)
        }

        convertJavaContentsToDex(jarFiles, buildDir, needCompileRJava)

        val classesDex = File(buildDir, "classes.dex")
        if (!classesDex.exists()) throw RuntimeException("DEX转换失败：未生成classes.dex文件")
        logMessage("✅ DEX转换完成 (${classesDex.length() / 1024} KB)")
    }

    private fun compileRJavaToClasses(
        sourceDir: File,
        buildDir: File,
    ) {
        logMessage("编译R.java文件为字节码")
        val classesDir = File(buildDir, "classes")
        classesDir.mkdirs()
        val androidJar =
            File(androidSdkPath.get(), "platforms/${androidPlatform.get()}/android.jar")
        val javaFiles =
            sourceDir.walkTopDown().filter { it.isFile && it.name.endsWith(".java") }.toList()
        if (javaFiles.isEmpty()) return

        val javaFilesList = File(buildDir, "java_files.txt")
        javaFilesList.writeText(javaFiles.joinToString("\n") { it.absolutePath })

        try {
            executeShellCommand(
                listOf(
                    "javac",
                    "-cp",
                    androidJar.absolutePath,
                    "-d",
                    classesDir.absolutePath,
                    "@${javaFilesList.absolutePath}",
                ),
                buildDir,
            )
            val rClassesJar = File(buildDir, "r_classes.jar")
            executeShellCommand(
                listOf(
                    "jar",
                    "cf",
                    rClassesJar.absolutePath,
                    "-C",
                    classesDir.absolutePath,
                    ".",
                ),
                buildDir,
            )
            logMessage("✅ R.java编译完成: ${rClassesJar.name} (${rClassesJar.length() / 1024} KB)")
        } finally {
            javaFilesList.delete()
        }
    }

    private fun convertJavaContentsToDex(
        jarFiles: List<File>,
        buildDir: File,
        includeRClasses: Boolean,
    ) {
        val buildTypeValue = buildType.get()
        logMessage("使用D8进行${buildTypeValue.uppercase()}转换")

        val command = mutableListOf(getAndroidSdkTool("d8"))
        when (buildTypeValue) {
            "debug" -> command.add("--debug")
            "release" -> command.add("--release")
        }
        command.add("--min-api")
        command.add("21")

        jarFiles.forEach { command.add(it.absolutePath) }

        if (includeRClasses) {
            val rClassesJar = File(buildDir, "r_classes.jar")
            if (rClassesJar.exists()) command.add(rClassesJar.absolutePath)
        }

        command.addAll(listOf("--output", buildDir.absolutePath))
        executeShellCommand(command)
    }

    private fun addNativeLibrariesToApk(
        apkFile: File,
        extractDir: File,
        buildDir: File,
    ) {
        val jniDir = File(extractDir, "jni")
        if (!jniDir.exists() || !jniDir.isDirectory) {
            logMessage("步骤5: 跳过native库（未找到jni目录）")
            return
        }
        logMessage("步骤5: 处理native库")
        val soFiles = jniDir.walkTopDown().filter { it.isFile && it.name.endsWith(".so") }.toList()
        if (soFiles.isEmpty()) return

        val tempLibDir = File(buildDir, "temp_lib/lib")
        tempLibDir.mkdirs()
        soFiles.forEach { soFile ->
            val relativePath = jniDir.toPath().relativize(soFile.toPath()).toString()
            val targetFile = File(tempLibDir, relativePath)
            targetFile.parentFile.mkdirs()
            soFile.copyTo(targetFile, overwrite = true)
        }
        executeShellCommand(
            listOf(
                "jar",
                "uf",
                apkFile.absolutePath,
                "-C",
                File(buildDir, "temp_lib").absolutePath,
                "lib",
            ),
            workDir = buildDir,
        )
        logMessage("✅ native库添加完成")
    }

    private fun addAssetsToApk(
        apkFile: File,
        extractDir: File,
        buildDir: File,
    ) {
        val assetsDir = File(extractDir, "assets")
        if (!assetsDir.exists() || !assetsDir.isDirectory ||
            assetsDir
                .walkTopDown()
                .filter { it.isFile }
                .toList()
                .isEmpty()
        ) {
            logMessage("步骤6: 跳过assets（未找到或为空）")
            return
        }
        logMessage("步骤6: 添加assets目录")
        executeShellCommand(
            listOf(
                "jar",
                "uf",
                apkFile.absolutePath,
                "-C",
                extractDir.absolutePath,
                "assets",
            ),
            workDir = buildDir,
        )
        logMessage("✅ assets添加完成")
    }

    private fun buildUnsignedApk(
        buildDir: File,
        manifestFile: File,
    ) {
        val pluginPackageId = packageId.get()
        logMessage("步骤4: 构建基础APK结构 (Package ID: $pluginPackageId)")
        val unsignedApk = File(buildDir, "unsigned.apk")
        val androidJar =
            File(androidSdkPath.get(), "platforms/${androidPlatform.get()}/android.jar")
        if (!androidJar.exists()) throw RuntimeException("Android Platform JAR不存在: ${androidJar.absolutePath}")

        val rTxtFile = File(buildDir, "R.txt")
        val command =
            mutableListOf(
                getAndroidSdkTool("aapt2"),
                "link",
                "-o",
                unsignedApk.absolutePath,
                "-I",
                androidJar.absolutePath,
                "--manifest",
                manifestFile.absolutePath,
                "--auto-add-overlay",
                "--no-version-vectors",
                "--no-resource-removal",
                "--emit-ids",
                rTxtFile.absolutePath,
                "--package-id",
                pluginPackageId,
            )

        val compiledResDir = File(buildDir, "compiled_res")
        if (compiledResDir.exists() && compiledResDir.list()?.isNotEmpty() == true) {
            val flatFiles =
                compiledResDir
                    .walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".flat") }
                    .toList()
            if (flatFiles.isNotEmpty()) {
                logMessage("添加${flatFiles.size}个编译后的资源")
                flatFiles.forEach { file ->
                    command.add("-R")
                    command.add(file.absolutePath)
                }
            }
        }

        try {
            executeShellCommand(command)
            logMessage("✅ APK基础结构构建完成")
        } catch (e: Exception) {
            logMessage("⚠️ 带资源APK构建失败，尝试无资源版本：${e.message}")
            val fallbackCommand =
                mutableListOf(
                    getAndroidSdkTool("aapt2"),
                    "link",
                    "-o",
                    unsignedApk.absolutePath,
                    "-I",
                    androidJar.absolutePath,
                    "--manifest",
                    manifestFile.absolutePath,
                    "--auto-add-overlay",
                    "--no-version-vectors",
                    "--no-resource-removal",
                    "--emit-ids",
                    rTxtFile.absolutePath,
                    "--package-id",
                    pluginPackageId,
                )
            executeShellCommand(fallbackCommand)
            logMessage("✅ 无资源APK构建完成")
        }
    }

    private fun addDexToApk(
        apkFile: File,
        buildDir: File,
    ) {
        logMessage("步骤7: 添加DEX文件到APK")
        val classesDex = File(buildDir, "classes.dex")
        if (!classesDex.exists()) return // 如果没有DEX文件，则静默跳过
        executeShellCommand(
            listOf("jar", "uf", apkFile.absolutePath, "classes.dex"),
            workDir = buildDir,
        )
        logMessage("✅ DEX文件添加完成")
    }

    private fun signFinalApk(
        unsignedApk: File,
        signedApk: File,
    ) {
        logMessage("步骤8: 签名APK")
        signedApk.parentFile.mkdirs()
        if (signedApk.exists()) signedApk.delete()
        executeShellCommand(
            listOf(
                getAndroidSdkTool("apksigner"),
                "sign",
                "--ks",
                keystorePath.get(),
                "--ks-pass",
                "pass:${keystorePassword.get()}",
                "--ks-key-alias",
                keyAlias.get(),
                "--key-pass",
                "pass:${keyPassword.get()}",
                "--min-sdk-version",
                "21",
                "--v4-signing-enabled",
                "false",
                "--out",
                signedApk.absolutePath,
                unsignedApk.absolutePath,
            ),
        )
        logMessage("✅ APK签名完成")
    }
}

// ==================== 动态任务注册和依赖配置 (已修复) ====================

val androidSdkPathValue = getAndroidSdkPath(project)
val buildToolsVersionValue = findLatestBuildTools(File(androidSdkPathValue, "build-tools"))
val androidPlatformVersionValue = findLatestPlatform(File(androidSdkPathValue, "platforms"))

val debugTasks = mutableListOf<TaskProvider<ConvertAarToApkTask>>()
val releaseTasks = mutableListOf<TaskProvider<ConvertAarToApkTask>>()

pluginModules.forEach { modulePath ->
    val subproject = project.rootProject.project(modulePath)
    val baseTaskName = modulePath.replace(":", "_").removePrefix("_")

    // --- 创建Debug任务 ---
    tasks
        .register<ConvertAarToApkTask>("convert_${baseTaskName}_debug") {
            group = "plugin debug"
            description = "自动构建 ${subproject.name} 模块并转换为Debug插件APK"

            val aarPathString =
                "${subproject.layout.buildDirectory.get()}/outputs/aar/${subproject.name}-debug.aar"
            this.aarFilePath.set(aarPathString)

            this.pluginName.set(subproject.name)
            this.modulePath.set(modulePath)
            this.outputDirectory.set(project.layout.buildDirectory.dir("output/plugin/debug"))
            this.keystorePath.set(keystorePathProvider)
            this.keystorePassword.set(keystorePasswordProvider)
            this.keyAlias.set(keyAliasProvider)
            this.keyPassword.set(keyPasswordProvider)
            this.androidSdkPath.set(androidSdkPathValue)
            this.buildToolsVersion.set(buildToolsVersionValue)
            this.androidPlatform.set(androidPlatformVersionValue)
            this.buildType.set("debug")
            this.buildTimestamp.set(System.currentTimeMillis().toString())
            this.packageId.set(
                pluginPackageIds[modulePath]
                    ?: throw RuntimeException("未找到模块 $modulePath 的 Package ID"),
            )
        }.also { debugTasks.add(it) }

    // --- 创建Release任务 ---
    tasks
        .register<ConvertAarToApkTask>("convert_${baseTaskName}_release") {
            group = "plugin release"
            description = "自动构建 ${subproject.name} 模块并转换为Release插件APK"

            val aarPathString =
                "${subproject.layout.buildDirectory.get()}/outputs/aar/${subproject.name}-release.aar"
            this.aarFilePath.set(aarPathString)

            this.pluginName.set(subproject.name)
            this.modulePath.set(modulePath)
            this.outputDirectory.set(project.layout.buildDirectory.dir("output/plugin/release"))
            this.keystorePath.set(keystorePathProvider)
            this.keystorePassword.set(keystorePasswordProvider)
            this.keyAlias.set(keyAliasProvider)
            this.keyPassword.set(keyPasswordProvider)
            this.androidSdkPath.set(androidSdkPathValue)
            this.buildToolsVersion.set(buildToolsVersionValue)
            this.androidPlatform.set(androidPlatformVersionValue)
            this.buildType.set("release")
            this.buildTimestamp.set(System.currentTimeMillis().toString())
            this.packageId.set(
                pluginPackageIds[modulePath]
                    ?: throw RuntimeException("未找到模块 $modulePath 的 Package ID"),
            )
        }.also { releaseTasks.add(it) }
}

tasks.register("convertAllToDebugPluginApks") {
    group = "plugin"
    description = "一键构建所有已配置模块的Debug插件APK"
    dependsOn(debugTasks)
}
tasks.register("convertAllToReleasePluginApks") {
    group = "plugin"
    description = "一键构建所有已配置模块的Release插件APK"
    dependsOn(releaseTasks)
}
tasks.register("cleanAllPluginOutputs") {
    group = "plugin"
    description = "清理所有生成的插件APK、日志文件和构建临时文件"

    doLast {
        project.layout.buildDirectory
            .dir("output/plugin")
            .get()
            .asFile
            .deleteRecursively()
        File(
            project.layout.buildDirectory
                .get()
                .asFile,
            "logs",
        ).deleteRecursively()
        File(
            project.layout.buildDirectory
                .get()
                .asFile,
            "tmp",
        ).listFiles()?.forEach { file ->
            if (file.name.startsWith("convert_")) file.deleteRecursively()
        }
        logger.lifecycle("✅ 已清理所有插件相关输出。")
    }
}

// ==================== 全局辅助函数 (保持不变) ====================
fun getAndroidSdkPath(project: Project): String {
    System.getenv("ANDROID_HOME")?.let { return it }
    System.getenv("ANDROID_SDK_ROOT")?.let { return it }

    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val props = java.util.Properties()
        localPropertiesFile.inputStream().use { props.load(it) }
        props.getProperty("sdk.dir")?.let { return it }
    }

    val userHome = System.getProperty("user.home")
    listOf(
        "$userHome/Library/Android/sdk", // macOS
        "$userHome/Android/Sdk", // Linux
        "$userHome/AppData/Local/Android/Sdk", // Windows
    ).forEach { path ->
        val dir = File(path)
        if (dir.exists() && dir.isDirectory) {
            return path
        }
    }

    throw GradleException("未能找到Android SDK。请设置 ANDROID_HOME 环境变量或在项目的 local.properties 文件中指定 sdk.dir。")
}

fun findLatestBuildTools(buildToolsDir: File): String {
    if (!buildToolsDir.exists()) throw RuntimeException("Android Build-Tools 目录不存在: ${buildToolsDir.absolutePath}")
    return buildToolsDir
        .listFiles { file -> file.isDirectory }
        ?.mapNotNull { it.name }
        ?.maxOrNull()
        ?: throw RuntimeException("在 ${buildToolsDir.absolutePath} 中未找到任何 Build-Tools 版本。")
}

fun findLatestPlatform(platformsDir: File): String {
    if (!platformsDir.exists()) throw RuntimeException("Android Platforms 目录不存在: ${platformsDir.absolutePath}")
    return platformsDir
        .listFiles { file -> file.isDirectory && file.name.startsWith("android-") }
        ?.map { it.name }
        ?.maxByOrNull { it.substringAfter("android-").toIntOrNull() ?: 0 }
        ?: throw RuntimeException("在 ${platformsDir.absolutePath} 中未找到任何 Android Platform。")
}
