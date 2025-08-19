package com.combo.aar2apk.tasks

import com.combo.aar2apk.PackagingOptions
import com.combo.aar2apk.SigningConfig
import com.combo.aar2apk.internal.model.SdkInfo
import com.combo.aar2apk.internal.processor.AarExtractor
import com.combo.aar2apk.internal.processor.ApkPackager
import com.combo.aar2apk.internal.processor.ApkSigner
import com.combo.aar2apk.internal.processor.DexProcessor
import com.combo.aar2apk.internal.processor.ResourceProcessor
import com.combo.aar2apk.internal.utils.ShellExecutor
import com.combo.aar2apk.internal.utils.TaskLogger
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class ConvertAarToApkTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    // --- 输入属性 ---
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val aarFile: RegularFileProperty

    @get:Input
    abstract val pluginName: Property<String>

    @get:Nested
    abstract val signingConfig: Property<SigningConfig>

    @get:Nested
    abstract val sdkInfo: Property<SdkInfo>

    @get:Input
    abstract val buildType: Property<String>

    @get:Input
    abstract val packageId: Property<String>

    @get:Nested
    abstract val packagingOptions: Property<PackagingOptions>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val remoteDependencyAars: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localDependencyClasses: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localDependencyResDirs: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localDependencyAssets: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localDependencyJniLibs: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun execute() {
        TaskLogger(project, buildType.get(), aarFile.get().asFile.name).use { logger ->
            logger.start(this.path, outputDirectory.get().asFile)
            val workDir = temporaryDir
            val sdk = sdkInfo.get()
            val shellExecutor = ShellExecutor(execOperations, logger)
            val options = packagingOptions.get()

            logger.log("打包选项: $options")

            // --- 1. 数据准备 ---
            val extractor = AarExtractor(project)
            val mainAarExtractDir = extractor.extract(aarFile.get().asFile, workDir.resolve("main_aar"))

            // 根据配置，有条件地解压远程AAR并收集产物
            val remoteClassesJars = mutableSetOf<File>()
            val remoteAssetsDirs = mutableSetOf<File>()
            val remoteJniDirs = mutableSetOf<File>()
            if (options.isAnyDependencyIncluded()) {
                val remoteAarExtractDir = workDir.resolve("remote_aars")
                remoteDependencyAars.files.forEach { aar ->
                    val outDir = remoteAarExtractDir.resolve(aar.nameWithoutExtension)
                    extractor.extract(aar, outDir)
                    outDir.resolve("classes.jar").takeIf { it.exists() }?.let { remoteClassesJars.add(it) }
                    outDir.resolve("assets").takeIf { it.exists() && it.isDirectory }?.let { remoteAssetsDirs.add(it) }
                    outDir.resolve("jni").takeIf { it.exists() && it.isDirectory }?.let { remoteJniDirs.add(it) }
                }
            }

            // --- 2. 资源处理 ---
            val resourceProcessor = ResourceProcessor(project, shellExecutor, sdk, logger)
            val linkedResources = resourceProcessor.process(
                mainAarExtractDir,
                if (options.includeDependenciesRes.get()) remoteDependencyAars.files else emptySet(),
                if (options.includeDependenciesRes.get()) localDependencyResDirs.files else emptySet(),
                packageId.get(),
                workDir
            )
            if (linkedResources == null) {
                logger.log("模块不含资源和Manifest，处理完成。")
                return
            }

            // --- 3. DEX处理 ---
            val allClassJars = mutableSetOf<File>()
            mainAarExtractDir.resolve("classes.jar").takeIf { it.exists() }?.let { allClassJars.add(it) }

            if (options.includeDependenciesDex.get()) {
                logger.log("DEX打包: 包含依赖库的代码。")
                allClassJars.addAll(localDependencyClasses.files)
                allClassJars.addAll(remoteClassesJars)
            } else {
                logger.log("DEX打包: 仅包含主模块代码。")
            }

            val dexProcessor = DexProcessor(shellExecutor, sdk, logger)
            val dexFile = dexProcessor.process(allClassJars, linkedResources.rJavaSourcesDir, buildType.get(), workDir)

            // --- 4. 打包 ---
            val allAssetDirs = mutableSetOf<File>()
            mainAarExtractDir.resolve("assets").takeIf { it.exists() && it.isDirectory }?.let { allAssetDirs.add(it) }

            if (options.includeDependenciesAssets.get()) {
                logger.log("Assets打包: 包含依赖库的Assets。")
                allAssetDirs.addAll(localDependencyAssets.files)
                allAssetDirs.addAll(remoteAssetsDirs)
            } else {
                logger.log("Assets打包: 仅包含主模块Assets。")
            }

            val allJniDirs = mutableSetOf<File>()
            mainAarExtractDir.resolve("jni").takeIf { it.exists() && it.isDirectory }?.let { allJniDirs.add(it) }

            if (options.includeDependenciesJni.get()) {
                logger.log("JNI打包: 包含依赖库的so库。")
                allJniDirs.addAll(localDependencyJniLibs.files)
                allJniDirs.addAll(remoteJniDirs)
            } else {
                logger.log("JNI打包: 仅包含主模块so库。")
            }

            val packager = ApkPackager(shellExecutor, logger)
            packager.addDex(linkedResources.unsignedApk, dexFile)
            packager.addNativeLibs(linkedResources.unsignedApk, allJniDirs)
            packager.addAssets(linkedResources.unsignedApk, allAssetDirs)

            // --- 5. 签名 ---
            val signer = ApkSigner(shellExecutor, sdk, logger)
            val signedApk = outputDirectory.get().file("${pluginName.get()}-${buildType.get()}.apk").asFile
            signer.sign(linkedResources.unsignedApk, signedApk, signingConfig.get())

            logger.log("✅ 转换成功! APK 大小: ${signedApk.length() / 1024} KB")
        }
    }
}