package com.combo.aar2apk

import com.combo.aar2apk.internal.model.SdkInfo
import com.combo.aar2apk.internal.utils.SdkLocator
import com.combo.aar2apk.tasks.ConvertAarToApkTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.register

class Aar2ApkPlugin : Plugin<Project> {

    companion object {
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

        extension.modules.get().forEach { modulePath ->
            val subproject = project.project(modulePath)
            val baseTaskName = modulePath.replace(":", "_").removePrefix("_")

            listOf("debug", "release").forEach { buildType ->
                val buildTypeCapitalized = buildType.replaceFirstChar { it.uppercase() }
                val taskName = "convert_${baseTaskName}_${buildType}"
                val assembleTaskName = "assemble$buildTypeCapitalized"
                val taskGroup = if (buildType == "debug") GROUP_DEBUG_BUILDS else GROUP_RELEASE_BUILDS

                project.tasks.register<ConvertAarToApkTask>(taskName) {
                    group = taskGroup
                    description = "构建 ${subproject.name} 模块并转换为 $buildTypeCapitalized 插件APK"
                    dependsOn(subproject.tasks.named(assembleTaskName))

                    val aarFileName = "${subproject.name}-$buildType.aar"
                    aarFile.set(subproject.layout.buildDirectory.file("outputs/aar/$aarFileName"))
                    pluginName.set(subproject.name)
                    this.signingConfig.set(extension.signingConfig)
                    this.sdkInfo.set(sdkInfo)
                    this.buildType.set(buildType)
                    this.packageId.set(pluginPackageIds[modulePath]!!)

                    val config = subproject.configurations.getByName("${buildType}RuntimeClasspath")

                    val remoteAars = config.incoming.artifactView {
                        attributes {
                            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
                        }
                        lenient(true)
                    }.files

                    val projectAars = config.allDependencies
                        .filterIsInstance<ProjectDependency>()
                        .map { projectDep ->
                            val dependencyProject = projectDep.dependencyProject
                            val dependencyAssembleTask = dependencyProject.tasks.named("assemble${buildTypeCapitalized}")
                            dependsOn(dependencyAssembleTask)
                            val dependencyAarFileName = "${dependencyProject.name}-$buildType.aar"
                            dependencyProject.layout.buildDirectory.file("outputs/aar/$dependencyAarFileName").get().asFile
                        }

                    dependencyAars.from(remoteAars, projectAars)

                    outputDirectory.set(project.layout.buildDirectory.dir("outputs/plugin-apks/$buildType"))
                }

                // 将注册好的任务名添加到对应的列表中
                if (buildType == "debug") {
                    debugTaskNames.add(taskName)
                } else {
                    releaseTaskNames.add(taskName)
                }
            }
        }

        // --- 2. 注册聚合构建任务 ---
        project.tasks.register("buildAllDebugPluginApks") {
            group = GROUP_MAIN
            description = "一键构建所有已配置模块的Debug插件APK"
            dependsOn(debugTaskNames)
        }
        project.tasks.register("buildAllReleasePluginApks") {
            group = GROUP_MAIN
            description = "一键构建所有已配置模块的Release插件APK"
            dependsOn(releaseTaskNames)
        }

        // --- 3. 注册清理任务 ---
        project.tasks.register<Delete>("cleanAllPluginApks") {
            group = GROUP_MAIN
            description = "清理所有生成的插件APK、相关日志和临时工作目录"

            delete(
                project.layout.buildDirectory.dir("outputs/plugin-apks"),
                project.layout.buildDirectory.dir("logs/aar2apk")
            )

            doLast {
                val tmpDir = project.layout.buildDirectory.get().asFile.resolve("tmp")
                if (tmpDir.exists() && tmpDir.isDirectory) {
                    tmpDir.listFiles()
                        ?.filter { it.isDirectory && it.name.startsWith("convert_") }
                        ?.forEach {
                            project.delete(it)
                            project.logger.lifecycle("Deleted temporary directory: ${it.path}")
                        }
                }
            }
        }
    }
}