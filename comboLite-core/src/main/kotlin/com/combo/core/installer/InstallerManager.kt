/*
 * Copyright (c) 2025, 贵州君城网络科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.combo.core.installer

import android.app.Application
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.XmlResourceParser
import android.os.Build
import com.combo.core.model.IntentFilterInfo
import com.combo.core.model.MetaDataInfo
import com.combo.core.model.PluginInfo
import com.combo.core.model.ProviderInfo
import com.combo.core.model.StaticReceiverInfo
import com.combo.core.security.SignatureValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * 插件安装器
 *
 * 负责插件的安装与卸载流程。主要职责包括：
 * 1. 验证插件APK的签名，确保其与宿主应用一致。
 * 2. 解析并验证插件 `AndroidManifest.xml` 中的元数据配置。
 * 3. 将插件文件安全地复制到应用内部存储的指定目录。
 * 4. 解析插件的四大组件信息（如静态广播、ContentProvider）。
 * 5. 提取插件的 native so 库。
 * 6. 通过 [XmlManager] 持久化管理已安装插件的信息。
 *
 * @property context Application 上下文，用于访问应用资源和包管理器。
 * @property xmlManager [XmlManager] 实例，用于管理 `plugins.xml` 配置文件。
 */
class InstallerManager(
    private val context: Application,
    private val xmlManager: XmlManager,
) {

    companion object {
        private const val TAG = "PluginInstaller"

        const val PLUGINS_DIR = "plugins"
        const val PLUGIN_BASE_APK_NAME = "base.apk"
        const val NATIVE_LIBS_DIR_NAME = "lib"
        const val DEX_OPTIMIZED_DIR_NAME = "dex_opt"

        private const val META_PLUGIN_ID = "plugin.id"
        private const val META_PLUGIN_VERSION = "plugin.version"
        private const val META_PLUGIN_DESCRIPTION = "plugin.description"
        private const val META_PLUGIN_ENTRY_CLASS = "plugin.entryClass"
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }

    /**
     * 从插件 `AndroidManifest.xml` 中解析出的核心配置信息。
     *
     * @property pluginId 插件的唯一标识符。
     * @property pluginVersion 插件的版本号。
     * @property pluginDescription 插件的功能描述。
     * @property entryClass 插件的入口类全限定名。
     */
    @Serializable
    data class PluginConfig(
        val pluginId: String,
        val pluginVersion: String,
        val pluginDescription: String,
        val entryClass: String,
    )

    /**
     * 表示插件安装操作的结果。
     */
    sealed class InstallResult {
        /**
         * 表示安装成功。
         * @property pluginInfo 安装成功的插件信息。
         */
        data class Success(val pluginInfo: PluginInfo) : InstallResult()

        /**
         * 表示安装失败。
         * @property reason 失败的原因描述。
         * @property exception 导致失败的异常（可选）。
         */
        data class Failure(val reason: String, val exception: Throwable? = null) : InstallResult()
    }

    /**
     * 插件签名验证器。
     */
    private val signatureValidator = SignatureValidator(context)

    /**
     * 插件安装的根目录。
     * 懒加载，并在首次访问时自动创建目录。
     */
    private val pluginsDir: File by lazy {
        File(context.filesDir, PLUGINS_DIR).apply { mkdirs() }
    }

    /**
     * 异步安装一个插件。
     * 此方法会执行完整的安装流程，包括签名验证、版本检查、文件复制、组件解析和信息持久化。
     *
     * @param pluginApkFile 待安装的插件APK文件。
     * @param forceOverwrite 是否强制覆盖安装。如果为 `true`，则会忽略版本检查，直接覆盖现有插件。
     * 默认为 `false`。
     * @return [InstallResult] 对象，表示安装成功或失败。
     */
    suspend fun installPlugin(
        pluginApkFile: File,
        forceOverwrite: Boolean = false,
    ): InstallResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("开始安装插件: ${pluginApkFile.name}, forceOverwrite: $forceOverwrite")

        // 步骤 1 & 2: 验证文件和元数据
        if (!pluginApkFile.exists()) return@withContext InstallResult.Failure("插件文件不存在")
        val pluginConfig = validateAndParseConfig(pluginApkFile)
            ?: return@withContext InstallResult.Failure("插件配置元数据验证失败")

        // 步骤 3: 签名验证
        if (!validateSignature(pluginApkFile)) {
            return@withContext InstallResult.Failure("插件签名验证失败")
        }

        val pluginId = pluginConfig.pluginId
        val pluginDir = getPluginDirectory(pluginId)

        // 步骤 4: 版本检查
        val existingPlugin = xmlManager.getPluginById(pluginId)
        when {
            existingPlugin == null -> {
                Timber.tag(TAG).i("新插件安装: $pluginId")
            }

            forceOverwrite -> {
                Timber.tag(TAG).i("强制覆盖安装插件: $pluginId")
                clearDexOptCache(pluginId)
            }

            compareVersions(pluginConfig.pluginVersion, existingPlugin.version) <= 0 -> {
                return@withContext InstallResult.Failure(
                    "已安装更高或相同版本 (${existingPlugin.version})，新版本 (${pluginConfig.pluginVersion}) 不能覆盖"
                )
            }

            else -> {
                Timber.tag(TAG)
                    .i("插件版本升级: $pluginId (${existingPlugin.version} -> ${pluginConfig.pluginVersion})")
                clearDexOptCache(pluginId)
            }
        }

        // 步骤 5: 备份旧插件目录 (如果是更新)
        val backupDir = File(pluginsDir, "$pluginId.backup")
        if (pluginDir.exists()) {
            try {
                pluginDir.copyRecursively(backupDir, overwrite = true)
                pluginDir.deleteRecursively()
                Timber.tag(TAG).d("旧插件目录已备份至: ${backupDir.absolutePath}")
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "备份旧插件目录失败，继续安装")
            }
        }

        pluginDir.mkdirs()

        try {
            // 步骤 6: 复制 APK 到插件目录并解压 so 库
            val targetApkFile = copyPluginApk(pluginApkFile, pluginDir)
            extractNativeLibs(pluginApkFile, pluginDir)

            // 步骤 7: 解析四大组件信息
            val staticReceivers = parseStaticReceivers(targetApkFile.absolutePath)
            val providers = parseProviders(targetApkFile.absolutePath)

            // 步骤 8: 更新 plugins.xml
            val pluginInfo = PluginInfo(
                pluginId = pluginConfig.pluginId,
                version = pluginConfig.pluginVersion,
                path = targetApkFile.absolutePath,
                entryClass = pluginConfig.entryClass,
                description = pluginConfig.pluginDescription,
                enabled = existingPlugin?.enabled ?: true,
                installTime = existingPlugin?.installTime ?: System.currentTimeMillis(),
                staticReceivers = staticReceivers,
                providers = providers,
            )

            if (existingPlugin != null) {
                xmlManager.updatePlugin(pluginInfo)
            } else {
                xmlManager.addPlugin(pluginInfo)
            }
            xmlManager.flushToDisk()

            // 步骤 9: 清理备份
            backupDir.deleteRecursively()

            Timber.tag(TAG).i("插件 [$pluginId] 安装成功。")
            InstallResult.Success(pluginInfo)
        } catch (e: Exception) {
            // 如果安装过程中出错，尝试从备份恢复
            pluginDir.deleteRecursively()
            if (backupDir.exists()) {
                try {
                    backupDir.renameTo(pluginDir)
                    Timber.tag(TAG).i("从备份恢复插件目录: $pluginId")
                } catch (ex: Exception) {
                    Timber.tag(TAG).e(ex, "从备份恢复失败")
                }
            }
            val reason = "插件安装过程中发生异常: ${e.message}"
            Timber.tag(TAG).e(e, reason)
            InstallResult.Failure(reason, e)
        }
    }

    /**
     * 卸载一个插件。
     * 这是一个事务性操作，会先将插件目录重命名，删除成功后再更新配置文件，以保证操作的原子性。
     *
     * @param pluginId 要卸载的插件的唯一标识符。
     * @return `true` 如果卸载成功，否则返回 `false`。
     */
    fun uninstallPlugin(pluginId: String): Boolean {
        Timber.tag(TAG).i("开始事务性卸载插件: $pluginId")

        // 清理插件的 DexOpt 缓存
        clearDexOptCache(pluginId)

        val pluginDir = getPluginDirectory(pluginId)

        // 如果插件目录不存在，则仅清理XML记录
        if (!pluginDir.exists()) {
            Timber.tag(TAG).w("插件目录不存在: ${pluginDir.absolutePath}, 将仅清理XML记录。")
            return if (xmlManager.getPluginById(pluginId) != null) {
                xmlManager.removePlugin(pluginId)
                xmlManager.flushToDisk()
                true
            } else {
                true
            }
        }

        // 将目录重命名为一个临时名称，防止删除过程中文件被占用
        val deletingDir =
            File(pluginDir.parentFile, "${pluginDir.name}.deleting_${System.currentTimeMillis()}")
        if (!pluginDir.renameTo(deletingDir)) {
            Timber.tag(TAG).e("卸载失败：无法重命名插件目录以进行安全删除。请检查文件权限或占用情况。")
            return false
        }

        Timber.tag(TAG).d("插件目录已移动至临时位置: ${deletingDir.absolutePath}")

        // 递归删除临时目录
        if (deletingDir.deleteRecursively()) {
            Timber.tag(TAG).d("临时目录已成功删除。")
            if (xmlManager.removePlugin(pluginId)) {
                xmlManager.flushToDisk()
            }
            Timber.tag(TAG).i("插件 [$pluginId] 已完全卸载。")
            return true
        } else {
            // 如果删除失败，尝试回滚（将临时目录重命名回去）
            Timber.tag(TAG).e("关键错误：在删除临时目录 ${deletingDir.name} 时失败。")
            if (deletingDir.renameTo(pluginDir)) {
                Timber.tag(TAG).i("回滚成功：插件目录已从临时位置恢复。")
            } else {
                Timber.tag(TAG)
                    .e("回滚失败！插件处于不一致状态，目录位于: ${deletingDir.absolutePath}")
            }
            return false
        }
    }

    /**
     * 获取指定插件的安装目录。
     *
     * @param pluginId 插件的唯一标识符。
     * @return 插件的安装目录 [File] 对象。
     */
    internal fun getPluginDirectory(pluginId: String): File {
        return File(pluginsDir, pluginId)
    }

    /**
     * 获取指定插件的 DEX 优化缓存目录。
     * 此操作仅在 Android 8.0 (API 26) 以下的系统版本有效，因为更高版本由系统管理DEX优化。
     */
    internal fun getOptimizedDirectory(pluginId: String): File? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            File(File(context.cacheDir, DEX_OPTIMIZED_DIR_NAME), pluginId).apply { mkdirs() }
        } else {
            null
        }
    }

    /**
     * 验证并解析插件 `AndroidManifest.xml` 中的元数据。
     *
     * @param pluginApkFile 插件APK文件。
     * @return 如果验证通过，返回 [PluginConfig] 对象；否则返回 `null`。
     */
    private fun validateAndParseConfig(pluginApkFile: File): PluginConfig? {
        Timber.tag(TAG).d("开始验证插件元数据配置: ${pluginApkFile.name}")
        try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageArchiveInfo(
                pluginApkFile.absolutePath,
                PackageManager.GET_META_DATA,
            )

            if (packageInfo == null) {
                Timber.tag(TAG).e("无法解析插件APK包信息: ${pluginApkFile.name}")
                return null
            }

            val metaData = packageInfo.applicationInfo?.metaData
            if (metaData == null) {
                Timber.tag(TAG).e("插件AndroidManifest.xml中未找到<application>标签下的元数据。")
                return null
            }

            // 读取并校验必要的元数据
            val pluginId = metaData.getString(META_PLUGIN_ID)
            val pluginVersion = metaData.getString(META_PLUGIN_VERSION)
            val entryClass = metaData.getString(META_PLUGIN_ENTRY_CLASS)

            if (pluginId.isNullOrBlank()) {
                Timber.tag(TAG).e("元数据 '$META_PLUGIN_ID' 不能为空。")
                return null
            }
            if (pluginVersion.isNullOrBlank()) {
                Timber.tag(TAG).e("元数据 '$META_PLUGIN_VERSION' 不能为空。")
                return null
            }
            if (entryClass.isNullOrBlank()) {
                Timber.tag(TAG).e("元数据 '$META_PLUGIN_ENTRY_CLASS' 不能为空。")
                return null
            }

            val pluginDescription = metaData.getString(META_PLUGIN_DESCRIPTION) ?: ""
            val pluginConfig = PluginConfig(pluginId, pluginVersion, pluginDescription, entryClass)

            Timber.tag(TAG).d("插件元数据配置验证通过: ${pluginConfig.pluginId}")
            return pluginConfig
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "解析插件元数据配置失败: ${e.message}")
            return null
        }
    }

    /**
     * 验证插件APK的签名是否与宿主应用匹配。
     *
     * @param pluginApkFile 插件APK文件。
     * @return `true` 如果签名验证通过，否则返回 `false`。
     */
    private fun validateSignature(pluginApkFile: File): Boolean {
        Timber.tag(TAG).d("开始验证插件签名: ${pluginApkFile.name}")
        val isValid = signatureValidator.validate(context, pluginApkFile)
        if (isValid) {
            Timber.tag(TAG).d("插件签名验证通过: ${pluginApkFile.name}")
        } else {
            Timber.tag(TAG).e("插件签名验证失败: ${pluginApkFile.name}")
        }
        return isValid
    }

    /**
     * 解析插件 APK 文件中的静态广播接收器 (`<receiver>`) 信息。
     *
     * @param apkPath 插件 APK 的文件路径。
     * @return 解析出的静态广播信息列表 ([StaticReceiverInfo])。
     */
    private fun parseStaticReceivers(apkPath: String): List<StaticReceiverInfo> {
        Timber.tag(TAG).d("开始解析 StaticReceivers: $apkPath")
        var parser: XmlResourceParser? = null
        try {
            val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val addAssetPathMethod =
                AssetManager::class.java.getMethod("addAssetPath", String::class.java)
            val cookie = addAssetPathMethod.invoke(assetManager, apkPath) as Int
            if (cookie == 0) return emptyList()

            parser = assetManager.openXmlResourceParser(cookie, "AndroidManifest.xml")
            val receivers = mutableListOf<StaticReceiverInfo>()
            var eventType = parser.eventType
            var currentReceiverName: String? = null
            var currentReceiverEnabled = true
            var currentReceiverExported = false
            var currentFilters: MutableList<IntentFilterInfo>? = null
            var inReceiverTag = false
            var inFilterTag = false
            var currentActions: MutableList<String>? = null
            var currentCategories: MutableList<String>? = null
            var currentSchemes: MutableList<String>? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "receiver" -> {
                            inReceiverTag = true
                            currentFilters = mutableListOf()
                            currentReceiverName =
                                parser.getAttributeValue(ANDROID_NAMESPACE, "name")
                            currentReceiverEnabled =
                                parser.getAttributeBooleanValue(ANDROID_NAMESPACE, "enabled", true)
                            currentReceiverExported = parser.getAttributeBooleanValue(
                                ANDROID_NAMESPACE,
                                "exported",
                                false
                            )
                        }

                        "intent-filter" -> if (inReceiverTag) {
                            inFilterTag = true
                            currentActions = mutableListOf()
                            currentCategories = mutableListOf()
                            currentSchemes = mutableListOf()
                        }

                        "action" -> if (inFilterTag) {
                            parser.getAttributeValue(ANDROID_NAMESPACE, "name")
                                ?.let { currentActions?.add(it) }
                        }

                        "category" -> if (inFilterTag) {
                            parser.getAttributeValue(ANDROID_NAMESPACE, "name")
                                ?.let { currentCategories?.add(it) }
                        }

                        "data" -> if (inFilterTag) {
                            parser.getAttributeValue(ANDROID_NAMESPACE, "scheme")
                                ?.let { currentSchemes?.add(it) }
                        }
                    }

                    XmlPullParser.END_TAG -> when (parser.name) {
                        "intent-filter" -> if (inFilterTag) {
                            inFilterTag = false
                            currentFilters?.add(
                                IntentFilterInfo(
                                    actions = currentActions ?: emptyList(),
                                    categories = currentCategories ?: emptyList(),
                                    schemes = currentSchemes ?: emptyList(),
                                ),
                            )
                        }

                        "receiver" -> if (inReceiverTag) {
                            inReceiverTag = false
                            if (!currentReceiverName.isNullOrBlank() && !currentFilters.isNullOrEmpty()) {
                                receivers.add(
                                    StaticReceiverInfo(
                                        className = currentReceiverName,
                                        enabled = currentReceiverEnabled,
                                        exported = currentReceiverExported,
                                        intentFilters = currentFilters,
                                    )
                                )
                                Timber.tag(TAG)
                                    .d("解析到静态广播: $currentReceiverName, filters: ${currentFilters.size}")
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            return receivers
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "使用 AXmlResourceParser 解析静态广播失败: $apkPath")
            return emptyList()
        } finally {
            parser?.close()
        }
    }

    /**
     * 解析插件 APK 文件中的内容提供者 (`<provider>`) 信息。
     *
     * @param apkPath 插件 APK 的文件路径。
     * @return 解析出的内容提供者信息列表 ([ProviderInfo])。
     */
    @Suppress("DEPRECATION")
    private fun parseProviders(apkPath: String): List<ProviderInfo> {
        Timber.tag(TAG).d("开始解析 ContentProvider: $apkPath")
        try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageArchiveInfo(
                apkPath,
                PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA,
            )

            val providerList = mutableListOf<ProviderInfo>()
            packageInfo?.providers?.forEach { provider ->
                val authorities = provider.authority?.split(";")?.filter { it.isNotBlank() }
                if (!authorities.isNullOrEmpty()) {
                    val metaDataList = provider.metaData?.keySet()?.mapNotNull { key ->
                        val rawValue = provider.metaData.get(key)
                        when (rawValue) {
                            is Int -> MetaDataInfo(name = key, value = null, resource = rawValue)
                            else -> MetaDataInfo(
                                name = key,
                                value = rawValue?.toString(),
                                resource = null
                            )
                        }
                    } ?: emptyList()

                    providerList.add(
                        ProviderInfo(
                            className = provider.name,
                            authorities = authorities,
                            enabled = provider.enabled,
                            exported = provider.exported,
                            metaData = metaDataList,
                        ),
                    )
                    Timber.tag(TAG)
                        .d("解析到 ContentProvider: ${provider.name}, authorities: $authorities, exported: ${provider.exported}")
                }
            }
            return providerList
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "解析APK文件ContentProvider失败: $apkPath")
            return emptyList()
        }
    }

    /**
     * 将源 APK 文件复制到指定的插件目录中。
     *
     * @param sourceFile 源 APK 文件。
     * @param pluginDir 目标插件目录。
     * @return 复制后的目标文件 [File] 对象。
     * @throws IOException 如果文件复制或验证失败。
     */
    private fun copyPluginApk(sourceFile: File, pluginDir: File): File {
        val targetFile = File(pluginDir, PLUGIN_BASE_APK_NAME)
        Timber.tag(TAG).d("复制 APK: ${sourceFile.name} -> ${targetFile.absolutePath}")

        sourceFile.inputStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (!targetFile.exists() || targetFile.length() != sourceFile.length()) {
            throw IOException("APK 文件复制验证失败")
        }
        targetFile.setReadOnly()
        return targetFile
    }

    /**
     * 从插件 APK 中提取 native so 库到指定的插件目录。
     *
     * @param pluginApk 插件 APK 文件。
     * @param pluginDir 目标插件目录。
     */
    private fun extractNativeLibs(pluginApk: File, pluginDir: File) {
        val libDir = File(pluginDir, NATIVE_LIBS_DIR_NAME)
        libDir.mkdirs()

        ZipFile(pluginApk).use { zip ->
            for (entry in zip.entries()) {
                if (entry.name.startsWith("lib/") && !entry.isDirectory) {
                    val abi = entry.name.substringAfter("lib/").substringBefore('/')
                    if (Build.SUPPORTED_ABIS.contains(abi)) {
                        val abiDir = File(libDir, abi).apply { mkdirs() }
                        val outputFile = File(abiDir, entry.name.substringAfterLast('/'))
                        zip.getInputStream(entry).use { input ->
                            outputFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }
        Timber.tag(TAG).d("插件 .so 库已解压至: ${libDir.absolutePath}")
    }

    /**
     * 比较两个版本号字符串。
     * 支持 "1.0.0" 格式的版本号。
     *
     * @param version1 第一个版本号。
     * @param version2 第二个版本号。
     * @return 如果 version1 > version2 返回正数, version1 < version2 返回负数, 相等则返回 0。
     */
    private fun compareVersions(version1: String, version2: String): Int {
        return try {
            val parts1 = version1.split('.').map { it.toIntOrNull() ?: 0 }
            val parts2 = version2.split('.').map { it.toIntOrNull() ?: 0 }
            val maxLength = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLength) {
                val v1 = parts1.getOrNull(i) ?: 0
                val v2 = parts2.getOrNull(i) ?: 0
                if (v1 != v2) {
                    return v1.compareTo(v2)
                }
            }
            0
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "版本号解析失败，回退到字符串比较: $version1 vs $version2")
            version1.compareTo(version2)
        }
    }

    /**
     * 清理指定插件的 DEX 优化缓存目录。
     * 此操作仅在 Android 8.0 (API 26) 以下的系统版本有效，因为更高版本由系统管理DEX优化。
     *
     * @param pluginId 插件的唯一标识符。
     */
    private fun clearDexOptCache(pluginId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            try {
                val cacheDir = File(context.cacheDir, DEX_OPTIMIZED_DIR_NAME)
                val pluginOptDir = File(cacheDir, pluginId)
                if (pluginOptDir.exists()) {
                    if (pluginOptDir.deleteRecursively()) {
                        Timber.tag(TAG).i("已成功清理插件 [$pluginId] 的 DexOpt 缓存。")
                    } else {
                        Timber.tag(TAG).w("清理插件 [$pluginId] 的 DexOpt 缓存失败。")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "清理插件 [$pluginId] 的 DexOpt 缓存时发生错误。")
            }
        }
    }
}