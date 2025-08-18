package com.combo.aar2apk.tasks

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
import javax.inject.Inject

@CacheableTask
abstract class ConvertAarToApkTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    // --- 输入/输出属性 ---
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

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dependencyAars: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun execute() {
        TaskLogger(project, buildType.get(), aarFile.get().asFile.name).use { logger ->
            logger.start(this.path, outputDirectory.get().asFile)
            val workDir = temporaryDir
            val sdk = sdkInfo.get()
            val shellExecutor = ShellExecutor(execOperations, logger)

            // 1. 解压
            val extractor = AarExtractor(project)
            val extractDir = extractor.extract(aarFile.get().asFile, workDir)

            // 2. 资源处理
            val resourceProcessor = ResourceProcessor(project, shellExecutor, sdk, logger)
            val linkedResources = resourceProcessor.process(extractDir, dependencyAars.files, packageId.get(), workDir)
            if (linkedResources == null) {
                logger.log("模块不含资源和Manifest，处理完成。")
                return
            }

            // 3. DEX处理
            val dexProcessor = DexProcessor(shellExecutor, sdk, logger)
            val dexFile = dexProcessor.process(extractDir, linkedResources.rJavaSourcesDir, buildType.get(), workDir)

            // 4. 打包
            val packager = ApkPackager(shellExecutor, logger)
            packager.addDex(linkedResources.unsignedApk, dexFile)
            packager.addNativeLibs(linkedResources.unsignedApk, extractDir)
            packager.addAssets(linkedResources.unsignedApk, extractDir)

            // 5. 签名
            val signer = ApkSigner(shellExecutor, sdk, logger)
            val signedApk = outputDirectory.get().file("${pluginName.get()}-${buildType.get()}.apk").asFile
            signer.sign(linkedResources.unsignedApk, signedApk, signingConfig.get())

            logger.log("✅ 转换成功! APK 大小: ${signedApk.length() / 1024} KB")
        }
    }
}