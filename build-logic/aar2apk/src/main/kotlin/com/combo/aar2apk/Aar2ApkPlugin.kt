package com.combo.aar2apk

import com.combo.aar2apk.internal.model.SdkInfo
import com.combo.aar2apk.internal.utils.SdkLocator
import com.combo.aar2apk.tasks.ConvertAarToApkTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.register

class Aar2ApkPlugin : Plugin<Project> {

    companion object {
        // [新增] 使用常量来定义任务组，方便管理和保持一致
        private const val GROUP_MAIN = "Plugin APKs"
        private const val GROUP_DEBUG_BUILDS = "Plugin APKs - Debug Builds"
        private const val GROUP_RELEASE_BUILDS = "Plugin APKs - Release Builds"
    }

    override fun apply(project: Project) {
        if (project != project.rootProject) {
            throw IllegalStateException("Aar2Apk插件只能应用在根项目(root project)上。")
        }

        val extension = project.extensions.create("aar2apk", Aar2ApkExtension::class.java)

        project.afterEvaluate {
            if (extension.modules.get().isNotEmpty()) {
                configureTasks(project, extension)
            }
        }
    }

    private fun configureTasks(project: Project, extension: Aar2ApkExtension) {
        val sdkPath = SdkLocator.getSdkPath(project)
        val buildToolsVersion = SdkLocator.findLatestBuildTools(sdkPath)
        val platformVersion = SdkLocator.findLatestPlatform(sdkPath)
        val sdkInfo = SdkInfo(sdkPath, buildToolsVersion, platformVersion)

        val pluginPackageIds = extension.modules.get().mapIndexed { index, modulePath ->
            modulePath to String.format("0x%02x", 0x80 + index)
        }.toMap()

        val debugTaskNames = mutableListOf<String>()
        val releaseTaskNames = mutableListOf<String>()

        // --- 1. 注册每个模块的转换任务，并放入子分组 ---
        extension.modules.get().forEach { modulePath ->
            val subproject = project.project(modulePath)
            val baseTaskName = modulePath.replace(":", "_").removePrefix("_")

            listOf("debug", "release").forEach { buildType ->
                val buildTypeCapitalized = buildType.replaceFirstChar { it.uppercase() }
                val taskName = "convert_${baseTaskName}_${buildType}"
                val assembleTaskName = "assemble$buildTypeCapitalized"

                // [优化] 根据构建类型动态设置任务组
                val taskGroup = if (buildType == "debug") GROUP_DEBUG_BUILDS else GROUP_RELEASE_BUILDS

                val convertTask = project.tasks.register<ConvertAarToApkTask>(taskName) {
                    group = taskGroup // <-- 应用新的子分组
                    description = "构建 ${subproject.name} 模块并转换为 $buildTypeCapitalized 插件APK"
                    dependsOn(subproject.tasks.named(assembleTaskName))

                    val aarFileName = "${subproject.name}-$buildType.aar"
                    aarFile.set(subproject.layout.buildDirectory.file("outputs/aar/$aarFileName"))
                    pluginName.set(subproject.name)
                    this.signingConfig.set(extension.signingConfig)
                    this.sdkInfo.set(sdkInfo)
                    this.buildType.set(buildType)
                    this.packageId.set(pluginPackageIds[modulePath]!!)

                    val dependencies = subproject.configurations.getByName("${buildType}RuntimeClasspath")
                        .incoming.artifactView {
                            attributes { attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar") }
                        }.files
                    dependencyAars.from(dependencies)
                    outputDirectory.set(project.layout.buildDirectory.dir("outputs/plugin-apks/$buildType"))
                }

                if (buildType == "debug") debugTaskNames.add(convertTask.name)
                else releaseTaskNames.add(convertTask.name)
            }
        }

        // --- 2. 注册聚合构建任务，放入主分组 ---
        project.tasks.register("buildAllDebugPluginApks") {
            group = GROUP_MAIN // <-- 应用主分组
            description = "一键构建所有已配置模块的Debug插件APK"
            dependsOn(debugTaskNames)
        }
        project.tasks.register("buildAllReleasePluginApks") {
            group = GROUP_MAIN // <-- 应用主分组
            description = "一键构建所有已配置模块的Release插件APK"
            dependsOn(releaseTaskNames)
        }

        // --- 3. 注册清理任务，放入主分组 ---
        project.tasks.register<Delete>("cleanAllPluginApks") {
            group = GROUP_MAIN // <-- 应用主分组
            description = "清理所有生成的插件APK、相关日志和临时工作目录"

            // [优化] 使用Delete任务类型，更符合Gradle的习惯
            delete(
                project.layout.buildDirectory.dir("outputs/plugin-apks"),
                project.layout.buildDirectory.dir("logs/aar2apk")
            )

            // 对于动态生成的tmp目录，我们仍然需要在执行阶段处理
            doLast {
                val tmpDir = project.layout.buildDirectory.get().asFile.resolve("tmp")
                if (tmpDir.exists() && tmpDir.isDirectory) {
                    tmpDir.listFiles()
                        ?.filter { it.isDirectory && it.name.startsWith("convert_") }
                        ?.forEach {
                            project.delete(it)
                            logger.lifecycle("Deleted temporary directory: ${it.path}")
                        }
                }
            }
        }
    }
}