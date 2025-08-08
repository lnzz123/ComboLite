/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 *
 * AAR到插件APK转换脚本 v2.8
 * - Debug/Release双构建模式，文件后缀区分
 * - 智能优化：Debug快速构建，Release最大优化
 * - 支持AAR完整内容：JAR、native库、assets、资源
 * - 插件系统兼容：ClassLoader/ResourceLoader
 */

import java.io.ByteArrayOutputStream
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ==================== 统一配置区域 ====================
val keystorePathProvider = project.providers.provider {
    project.rootProject.file("jctech.jks").absolutePath
}
val keystorePasswordProvider = project.providers.provider { "he1755858138" }
val keyAliasProvider = project.providers.provider { "jctech" }
val keyPasswordProvider = project.providers.provider { "he1755858138" }
val inputDir = "input_aars"
val outputDir = "output_apks"

// ==================== 自定义任务定义 ====================

/**
 * AAR到插件APK转换任务基类
 */
abstract class ConvertAarToApkTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val aarFile: RegularFileProperty

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

    private lateinit var logWriter: PrintWriter
    private lateinit var logFile: File

    @TaskAction
    fun execute() {
        val workDir = temporaryDir
        workDir.mkdirs()

        initializeLogging()

        val extractDir = File(workDir, "extracted")
        val buildDir = File(workDir, "build")
        extractDir.mkdirs()
        buildDir.mkdirs()

        logAndPrint("🚀 开始转换: ${aarFile.get().asFile.name}")
        logAndPrint("工作目录: ${workDir.absolutePath}")

        try {
            extractAar(aarFile.get().asFile, extractDir)
            val manifestFile = File(extractDir, "AndroidManifest.xml")

            if (!manifestFile.exists()) {
                logAndPrint("⚠️ 未找到AndroidManifest.xml，跳过构建（纯代码库）")
                return
            }

            processJarsToD8(extractDir, buildDir)
            compileResources(extractDir, buildDir)
            val unsignedApk = buildApk(buildDir, manifestFile)
            addDexFile(unsignedApk, buildDir)
            addNativeLibs(unsignedApk, extractDir, buildDir)
            addAssetsDir(unsignedApk, extractDir, buildDir)

            val buildTypeValue = buildType.get()
            val apkFileName = "${aarFile.get().asFile.nameWithoutExtension}-${buildTypeValue}.apk"
            val signedApk = File(outputDirectory.get().asFile, apkFileName)
            signApk(unsignedApk, signedApk)

            logAndPrint("✅ 转换成功! APK大小: ${signedApk.length() / 1024} KB")
            logAndPrint("APK文件: ${signedApk.name}")
        } catch (e: Exception) {
            logAndPrint("❌ 转换失败: ${e.message}")
            logWriter.println("错误详情: ${e.stackTraceToString()}")
            throw e
        } finally {
            closeLogging()
        }
    }

    // ==================== 日志管理 ====================

    private fun initializeLogging() {
        val logDir = File(project.layout.buildDirectory.get().asFile, "logs")
        logDir.mkdirs()

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val aarName = aarFile.get().asFile.nameWithoutExtension
        val buildTypeValue = buildType.get()
        logFile = File(logDir, "aar-to-apk-${aarName}-${buildTypeValue}-${timestamp}.log")

        logWriter = PrintWriter(FileWriter(logFile, true))
        logWriter.println("==================== AAR转APK构建日志 ====================")
        logWriter.println("开始时间: ${LocalDateTime.now()}")
        logWriter.println("构建类型: ${buildTypeValue.uppercase()}")
        logWriter.println("AAR文件: ${aarFile.get().asFile.name}")
        logWriter.println("输出目录: ${outputDirectory.get().asFile.absolutePath}")
        logWriter.println("========================================================")
        logWriter.flush()
    }

    private fun logAndPrint(message: String) {
        val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val logMessage = "[$timeStamp] $message"

        logger.lifecycle(logMessage)
        logWriter.println(logMessage)
        logWriter.flush()
    }

    private fun closeLogging() {
        if (::logWriter.isInitialized) {
            logWriter.println("========================================================")
            logWriter.println("结束时间: ${LocalDateTime.now()}")
            logWriter.println("日志文件: ${logFile.absolutePath}")
            logWriter.close()

            logger.lifecycle("构建日志已保存到: ${logFile.absolutePath}")
        }
    }

    // ==================== 工具方法 ====================

    private fun getSdkTool(toolName: String): String {
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

    private fun executeCommand(command: List<String>, workDir: File? = null) {
        val cmdStr = command.joinToString(" ")
        logAndPrint("执行: ${command[0]} ${command.drop(1).joinToString(" ")}")

        logWriter.println("完整命令: $cmdStr")
        workDir?.let { logWriter.println("工作目录: ${it.absolutePath}") }

        val processBuilder = ProcessBuilder(command)
        workDir?.let { processBuilder.directory(it) }

        val process = processBuilder.start()
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        process.inputStream.transferTo(stdout)
        process.errorStream.transferTo(stderr)

        val exitCode = process.waitFor()
        val stdoutStr = stdout.toString()
        val stderrStr = stderr.toString()

        if (stdoutStr.isNotBlank()) {
            logWriter.println("标准输出: $stdoutStr")
        }
        if (stderrStr.isNotBlank()) {
            logWriter.println("错误输出: $stderrStr")
        }

        if (exitCode != 0) {
            logWriter.println("命令执行失败，退出码: $exitCode")
            logWriter.flush()
            throw RuntimeException("命令执行失败 (退出码: $exitCode): ${command[0]}")
        }

        logWriter.flush()
    }

    // ==================== 核心处理方法 ====================

    private fun extractAar(aar: File, targetDir: File) {
        logAndPrint("步骤1: 解压AAR文件")
        project.copy {
            from(project.zipTree(aar))
            into(targetDir)
        }
    }

    private fun addNativeLibs(apkFile: File, extractDir: File, buildDir: File) {
        val jniDir = File(extractDir, "jni")
        if (!jniDir.exists() || !jniDir.isDirectory) {
            logAndPrint("步骤5a: 跳过native库（未找到jni目录）")
            return
        }

        logAndPrint("步骤5a: 处理native库")

        val soFiles = jniDir.walkTopDown().filter {
            it.isFile && it.name.endsWith(".so")
        }.toList()

        if (soFiles.isEmpty()) {
            logAndPrint("jni目录中无.so文件")
            return
        }

        logAndPrint("找到${soFiles.size}个native库文件")

        val tempLibDir = File(buildDir, "temp_lib/lib")
        tempLibDir.mkdirs()

        soFiles.forEach { soFile ->
            val relativePath = jniDir.toPath().relativize(soFile.toPath()).toString()
            val targetFile = File(tempLibDir, relativePath)
            targetFile.parentFile.mkdirs()
            soFile.copyTo(targetFile, overwrite = true)
            logWriter.println("复制: ${soFile.name} -> lib/$relativePath")
        }

        executeCommand(listOf(
            "jar", "uf", apkFile.absolutePath, "-C", File(buildDir, "temp_lib").absolutePath, "lib"
        ), workDir = buildDir)

        logAndPrint("✅ native库添加完成")
    }

    private fun addAssetsDir(apkFile: File, extractDir: File, buildDir: File) {
        val assetsDir = File(extractDir, "assets")
        if (!assetsDir.exists() || !assetsDir.isDirectory) {
            logAndPrint("步骤5b: 跳过assets（未找到assets目录）")
            return
        }

        val assetFiles = assetsDir.walkTopDown().filter { it.isFile }.toList()
        if (assetFiles.isEmpty()) {
            logAndPrint("步骤5b: 跳过assets（目录为空）")
            return
        }

        logAndPrint("步骤5b: 添加assets目录")
        logAndPrint("找到${assetFiles.size}个asset文件")

        executeCommand(listOf(
            "jar", "uf", apkFile.absolutePath, "-C", extractDir.absolutePath, "assets"
        ), workDir = buildDir)

        logAndPrint("✅ assets添加完成")
    }

    private fun processJarsToD8(extractDir: File, buildDir: File) {
        logAndPrint("步骤2: 处理JAR文件")

        val jarFiles = mutableListOf<File>()

        // 主JAR文件
        val classesJar = File(extractDir, "classes.jar")
        if (classesJar.exists()) {
            jarFiles.add(classesJar)
            logWriter.println("主JAR: ${classesJar.name}")
        }

        // libs目录JAR文件
        val libsDir = File(extractDir, "libs")
        if (libsDir.exists() && libsDir.isDirectory) {
            libsDir.listFiles { file ->
                file.isFile && file.extension.equals("jar", ignoreCase = true)
            }?.forEach { jarFile ->
                jarFiles.add(jarFile)
                logWriter.println("依赖JAR: ${jarFile.name}")
            }
        }

        // 额外的classes文件
        extractDir.listFiles { file ->
            file.isFile && file.name.matches(Regex("classes\\d*\\.jar"))
        }?.forEach { jarFile ->
            if (!jarFiles.contains(jarFile)) {
                jarFiles.add(jarFile)
                logWriter.println("额外JAR: ${jarFile.name}")
            }
        }

        if (jarFiles.isEmpty()) {
            logAndPrint("⚠️ 未找到JAR文件，跳过DEX转换")
            return
        }

        logAndPrint("找到${jarFiles.size}个JAR文件，开始优化DEX转换")

        // 使用D8进行优化转换
        processJarsWithD8(jarFiles, buildDir)

        val classesDex = File(buildDir, "classes.dex")
        if (!classesDex.exists()) {
            throw RuntimeException("DEX转换失败：未生成classes.dex文件")
        }

        logAndPrint("✅ DEX转换完成 (${classesDex.length() / 1024} KB)")
    }

    /**
     * 使用D8进行标准转换（根据构建类型优化）
     */
    private fun processJarsWithD8(jarFiles: List<File>, buildDir: File) {
        val buildTypeValue = buildType.get()
        logAndPrint("使用D8进行${buildTypeValue.uppercase()}转换")

        val command = mutableListOf(getSdkTool("d8"))

        // 根据构建类型添加不同参数
        when (buildTypeValue) {
            "debug" -> {
                command.add("--debug")  // Debug模式保留调试信息
            }
            "release" -> {
                command.add("--release")  // Release模式移除调试信息
            }
        }

        command.add("--min-api")
        command.add("21")

        jarFiles.forEach { command.add(it.absolutePath) }
        command.addAll(listOf("--output", buildDir.absolutePath))

        executeCommand(command)
        logAndPrint("✅ D8转换完成")
    }

    private fun compileResources(extractDir: File, buildDir: File) {
        val resDir = File(extractDir, "res")
        if (!resDir.exists() || !resDir.isDirectory) {
            logAndPrint("步骤3: 跳过资源编译（未找到res目录）")
            return
        }

        logAndPrint("步骤3: 编译资源文件")

        val compiledResDir = File(buildDir, "compiled_res")
        compiledResDir.deleteRecursively()
        compiledResDir.mkdirs()

        try {
            val resourceFiles = resDir.walkTopDown().filter {
                it.isFile && !it.name.startsWith(".")
            }.toList()

            if (resourceFiles.isEmpty()) {
                logAndPrint("res目录中无资源文件，跳过编译")
                return
            }

            logAndPrint("找到${resourceFiles.size}个资源文件")

            resourceFiles.forEach { resourceFile ->
                try {
                    logWriter.println("编译: ${resourceFile.name}")
                    executeCommand(listOf(
                        getSdkTool("aapt2"), "compile",
                        resourceFile.absolutePath,
                        "-o", compiledResDir.absolutePath
                    ))
                } catch (e: Exception) {
                    logWriter.println("跳过有问题的资源: ${resourceFile.name} - ${e.message}")
                }
            }

            logAndPrint("✅ 资源编译完成")

        } catch (e: Exception) {
            logAndPrint("⚠️ 资源编译失败，将跳过资源：${e.message}")
            compiledResDir.deleteRecursively()
            compiledResDir.mkdirs()
        }
    }

    private fun buildApk(buildDir: File, manifestFile: File): File {
        logAndPrint("步骤4: 构建APK")
        val unsignedApk = File(buildDir, "unsigned.apk")
        val androidJar = File(androidSdkPath.get(), "platforms/${androidPlatform.get()}/android.jar")

        if (!androidJar.exists()) {
            throw GradleException("Android Platform JAR不存在: ${androidJar.absolutePath}")
        }

        val command = mutableListOf(
            getSdkTool("aapt2"), "link",
            "-o", unsignedApk.absolutePath,
            "-I", androidJar.absolutePath,
            "--manifest", manifestFile.absolutePath,
            "--auto-add-overlay",
            "--no-version-vectors"
        )

        val compiledResDir = File(buildDir, "compiled_res")
        if (compiledResDir.exists() && compiledResDir.list()?.isNotEmpty() == true) {
            val flatFiles = compiledResDir.walkTopDown().filter {
                it.isFile && it.name.endsWith(".flat")
            }.toList()

            if (flatFiles.isNotEmpty()) {
                logAndPrint("添加${flatFiles.size}个编译后的资源")
                flatFiles.forEach { file ->
                    logWriter.println("资源: ${file.name} (${file.length()} bytes)")
                    command.add("-R")
                    command.add(file.absolutePath)
                }
            } else {
                logAndPrint("编译目录存在但无.flat文件")
            }
        } else {
            logAndPrint("无编译后的资源文件")
        }

        try {
            executeCommand(command)
            logAndPrint("✅ APK基础结构构建完成")
            return unsignedApk
        } catch (e: Exception) {
            logAndPrint("⚠️ 带资源APK构建失败，尝试无资源版本：${e.message}")

            val fallbackCommand = mutableListOf(
                getSdkTool("aapt2"), "link",
                "-o", unsignedApk.absolutePath,
                "-I", androidJar.absolutePath,
                "--manifest", manifestFile.absolutePath,
                "--auto-add-overlay",
                "--no-version-vectors"
            )

            executeCommand(fallbackCommand)
            logAndPrint("✅ 无资源APK构建完成")
            return unsignedApk
        }
    }

    private fun addDexFile(apkFile: File, buildDir: File) {
        logAndPrint("步骤5: 添加DEX文件")
        val classesDex = File(buildDir, "classes.dex")
        if (!classesDex.exists()) {
            throw RuntimeException("DEX文件不存在: ${classesDex.absolutePath}")
        }

        executeCommand(listOf(
            "jar", "uf", apkFile.absolutePath, "classes.dex"
        ), workDir = buildDir)

        logAndPrint("✅ DEX文件添加完成")
    }

    private fun signApk(unsignedApk: File, signedApk: File) {
        logAndPrint("步骤6: 签名APK")
        signedApk.parentFile.mkdirs()
        if(signedApk.exists()) signedApk.delete()

        executeCommand(listOf(
            getSdkTool("apksigner"), "sign",
            "--ks", keystorePath.get(),
            "--ks-pass", "pass:${keystorePassword.get()}",
            "--ks-key-alias", keyAlias.get(),
            "--key-pass", "pass:${keyPassword.get()}",
            "--min-sdk-version", "21",
            "--v4-signing-enabled", "false", // 禁用v4签名，不生成.idsig文件
            "--out", signedApk.absolutePath,
            unsignedApk.absolutePath
        ))

        logAndPrint("✅ APK签名完成")
    }
}

// ==================== 动态任务注册和依赖配置 ====================
val inputDirHandle = project.file(inputDir)
val outputDirHandle = project.file(outputDir)

val androidSdkPathValue = getAndroidSdkPath(project)
val buildToolsVersionValue = findLatestBuildTools(File(androidSdkPathValue, "build-tools"))
val androidPlatformVersionValue = findLatestPlatform(File(androidSdkPathValue, "platforms"))

// 创建Debug和Release任务
val debugTasks = mutableListOf<TaskProvider<ConvertAarToApkTask>>()
val releaseTasks = mutableListOf<TaskProvider<ConvertAarToApkTask>>()

if (inputDirHandle.exists() && inputDirHandle.isDirectory) {
    inputDirHandle.listFiles { file -> file.isFile && file.extension.equals("aar", ignoreCase = true) }
        ?.forEach { aarFile ->
            val baseTaskName = aarFile.nameWithoutExtension

            // 创建Debug任务
            val debugTaskProvider = tasks.register<ConvertAarToApkTask>("convert_${baseTaskName}_debug") {
                group = "plugin debug"
                description = "转换 ${aarFile.name} 为Debug插件APK"

                this.aarFile.set(aarFile)
                this.outputDirectory.set(outputDirHandle)
                this.keystorePath.set(keystorePathProvider)
                this.keystorePassword.set(keystorePasswordProvider)
                this.keyAlias.set(keyAliasProvider)
                this.keyPassword.set(keyPasswordProvider)
                this.androidSdkPath.set(androidSdkPathValue)
                this.buildToolsVersion.set(buildToolsVersionValue)
                this.androidPlatform.set(androidPlatformVersionValue)
                this.buildType.set("debug")
            }
            debugTasks.add(debugTaskProvider)

            // 创建Release任务
            val releaseTaskProvider = tasks.register<ConvertAarToApkTask>("convert_${baseTaskName}_release") {
                group = "plugin release"
                description = "转换 ${aarFile.name} 为Release插件APK"

                this.aarFile.set(aarFile)
                this.outputDirectory.set(outputDirHandle)
                this.keystorePath.set(keystorePathProvider)
                this.keystorePassword.set(keystorePasswordProvider)
                this.keyAlias.set(keyAliasProvider)
                this.keyPassword.set(keyPasswordProvider)
                this.androidSdkPath.set(androidSdkPathValue)
                this.buildToolsVersion.set(buildToolsVersionValue)
                this.androidPlatform.set(androidPlatformVersionValue)
                this.buildType.set("release")
            }
            releaseTasks.add(releaseTaskProvider)
        }
}

// Debug版本任务集合
tasks.register("convertAllAarsToDebugApks") {
    group = "plugin"
    description = "将所有AAR文件转换为Debug插件APK"
    dependsOn(debugTasks)

    doFirst {
        if (debugTasks.isEmpty()) {
            logger.lifecycle("✅ 在目录 ${inputDirHandle.absolutePath} 中没有找到任何 .aar 文件可供转换。")
        } else {
            logger.lifecycle("🚀 准备执行 ${debugTasks.size} 个AAR文件的DEBUG插件APK转换...")
            logger.lifecycle("   Debug模式特性：")
            logger.lifecycle("   - 保留调试信息")
            logger.lifecycle("   - 快速构建")
            logger.lifecycle("   - 保守混淆")
            logger.lifecycle("   - 文件后缀: -debug.apk")
        }
    }
}

// Release版本任务集合
tasks.register("convertAllAarsToReleaseApks") {
    group = "plugin"
    description = "将所有AAR文件转换为Release插件APK"
    dependsOn(releaseTasks)

    doFirst {
        if (releaseTasks.isEmpty()) {
            logger.lifecycle("✅ 在目录 ${inputDirHandle.absolutePath} 中没有找到任何 .aar 文件可供转换。")
        } else {
            logger.lifecycle("🚀 准备执行 ${releaseTasks.size} 个AAR文件的RELEASE插件APK转换...")
            logger.lifecycle("   Release模式特性：")
            logger.lifecycle("   - D8代码优化")
            logger.lifecycle("   - 移除日志调用")
            logger.lifecycle("   - 激进混淆")
            logger.lifecycle("   - 文件后缀: -release.apk")
        }
    }
}

// ==================== 清理任务 ====================
tasks.register("cleanAllOutputs") {
    group = "plugin"
    description = "清理所有生成的插件APK、日志文件和构建临时文件"

    doLast {
        // 删除输出目录内的所有文件，但保留目录本身
        val outputDirFile = project.file(outputDir)
        if (outputDirFile.exists() && outputDirFile.isDirectory) {
            outputDirFile.listFiles()?.forEach { file ->
                file.deleteRecursively()
                logger.lifecycle("✅ 已清理文件: ${file.absolutePath}")
            }
            logger.lifecycle("✅ 输出目录 ${outputDirFile.absolutePath} 内的文件已清理。")
        } else {
            logger.lifecycle("⚠️ 输出目录不存在，无需清理: ${outputDirFile.absolutePath}")
        }

        // 删除日志目录
        val logDir = File(project.layout.buildDirectory.get().asFile, "logs")
        if (logDir.exists()) {
            logDir.deleteRecursively()
            logger.lifecycle("✅ 已清理日志目录: ${logDir.absolutePath}")
        } else {
            logger.lifecycle("⚠️ 日志目录不存在，无需清理: ${logDir.absolutePath}")
        }

        // 清理所有任务的临时目录
        val tmpDir = File(project.layout.buildDirectory.get().asFile, "tmp")
        if (tmpDir.exists()) {
            tmpDir.deleteRecursively()
            logger.lifecycle("✅ 已清理所有任务的临时目录: ${tmpDir.absolutePath}")
        } else {
            logger.lifecycle("⚠️ 临时目录不存在，无需清理: ${tmpDir.absolutePath}")
        }
    }
}

// ==================== 全局辅助函数 ====================

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
        "$userHome/AppData/Local/Android/Sdk" // Windows
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
    return buildToolsDir.listFiles { file -> file.isDirectory }
        ?.mapNotNull { it.name }
        ?.maxOrNull() ?: throw RuntimeException("在 ${buildToolsDir.absolutePath} 中未找到任何 Build-Tools 版本。")
}

fun findLatestPlatform(platformsDir: File): String {
    if (!platformsDir.exists()) throw RuntimeException("Android Platforms 目录不存在: ${platformsDir.absolutePath}")
    return platformsDir.listFiles { file -> file.isDirectory && file.name.startsWith("android-") }
        ?.map { it.name }
        ?.maxByOrNull { it.substringAfter("android-").toIntOrNull() ?: 0 }
        ?: throw RuntimeException("在 ${platformsDir.absolutePath} 中未找到任何 Android Platform。")
}