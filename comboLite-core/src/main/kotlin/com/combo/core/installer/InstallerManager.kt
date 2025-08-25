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

package com.combo.core.installer

import android.app.Application
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.XmlResourceParser
import android.os.Build
import com.combo.core.loader.PluginClassLoader
import com.combo.core.manager.PluginManager
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
 * 负责插件的安装流程，包括：
 * 1. 验证插件APK签名是否与宿主应用签名匹配
 * 2. 检查并解析AndroidManifest.xml中的插件元数据配置
 * 3. 将插件文件复制到data/data/package/plugins目录下
 * 4. 记录安装插件信息到data/data/package/files/plugins.xml
 *
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

        // 插件元数据的键名
        private const val META_PLUGIN_ID = "plugin.id"
        private const val META_PLUGIN_VERSION = "plugin.version"
        private const val META_PLUGIN_DESCRIPTION = "plugin.description"
        private const val META_PLUGIN_ENTRY_CLASS = "plugin.entryClass"
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }

    private val signatureValidator = SignatureValidator(context)
    private val pluginsDir: File by lazy {
        File(context.filesDir, PLUGINS_DIR).apply { mkdirs() }
    }

    /**
     * 插件配置信息数据类
     */
    @Serializable
    data class PluginConfig(
        val pluginId: String,
        val pluginVersion: String,
        val pluginDescription: String,
        val entryClass: String,
    )

    /**
     * 插件安装结果
     */
    sealed class InstallResult {
        data class Success(
            val pluginInfo: PluginInfo,
        ) : InstallResult()

        data class Failure(
            val reason: String,
            val exception: Throwable? = null,
        ) : InstallResult()
    }

    /**
     * 异步安装插件（支持版本检查和强制覆盖）
     *
     * @param pluginApkFile 插件APK文件
     * @param forceOverwrite 是否强制覆盖安装，默认为false
     * @return 安装结果
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
        if (existingPlugin != null && !forceOverwrite) {
            val versionComparison = compareVersions(pluginConfig.pluginVersion, existingPlugin.version)
            if (versionComparison <= 0) {
                return@withContext InstallResult.Failure(
                    "已安装更高或相同版本 (${existingPlugin.version})，新版本 (${pluginConfig.pluginVersion}) 不能覆盖"
                )
            }
            Timber.tag(TAG).i("插件版本升级: $pluginId (${existingPlugin.version} -> ${pluginConfig.pluginVersion})")
        }

        // 步骤 5: 备份旧插件目录 (如果是更新)
        val backupDir = File(pluginsDir, "$pluginId.backup")
        if (pluginDir.exists()) {
            try {
                pluginDir.copyRecursively(backupDir, overwrite = true)
                pluginDir.deleteRecursively() // 删除旧目录为新安装做准备
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
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     */
    fun uninstallPlugin(pluginId: String): Boolean {
        Timber.tag(TAG).i("开始事务性卸载插件: $pluginId")

        val pluginDir = getPluginDirectory(pluginId)

        if (!pluginDir.exists()) {
            Timber.tag(TAG).w("插件目录不存在: ${pluginDir.absolutePath}, 将仅清理XML记录。")
            if (xmlManager.getPluginById(pluginId) != null) {
                xmlManager.removePlugin(pluginId)
                xmlManager.flushToDisk()
            }
            return true
        }

        val deletingDir = File(pluginDir.parentFile, "${pluginDir.name}.deleting_${System.currentTimeMillis()}")
        if (!pluginDir.renameTo(deletingDir)) {
            Timber.tag(TAG).e("卸载失败：无法重命名插件目录以进行安全删除。请检查文件权限或占用情况。")
            return false
        }

        Timber.tag(TAG).d("插件目录已移动至临时位置: ${deletingDir.absolutePath}")

        if (deletingDir.deleteRecursively()) {
            Timber.tag(TAG).d("临时目录已成功删除。")
            if (xmlManager.removePlugin(pluginId)) {
                xmlManager.flushToDisk()
            }
            PluginManager.getOptimizedDirectory(context, pluginId).deleteRecursively()
            Timber.tag(TAG).i("插件 [$pluginId] 已完全卸载。")
            return true
        } else {
            Timber.tag(TAG).e("关键错误：在删除临时目录 ${deletingDir.name} 时失败。")
            if (deletingDir.renameTo(pluginDir)) {
                Timber.tag(TAG).i("回滚成功：插件目录已从临时位置恢复。")
            } else {
                Timber.tag(TAG).e("回滚失败！插件处于不一致状态，目录位于: ${deletingDir.absolutePath}")
            }
            return false
        }
    }
    /**
     * 比较两个版本号
     *
     * @param version1 版本1
     * @param version2 版本2
     * @return 当version1 > version2时返回正数，version1 < version2时返回负数，相等时返回0
     */
    private fun compareVersions(
        version1: String,
        version2: String,
    ): Int {
        return try {
            val parts1 = version1.split('.').map { it.toIntOrNull() ?: 0 }
            val parts2 = version2.split('.').map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLength) {
                val v1 = parts1.getOrNull(i) ?: 0
                val v2 = parts2.getOrNull(i) ?: 0

                when {
                    v1 > v2 -> return 1
                    v1 < v2 -> return -1
                }
            }

            0 // 版本相等
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "版本比较失败，使用字符串比较: $version1 vs $version2")
            version1.compareTo(version2)
        }
    }

    /**
     * 验证插件APK签名
     *
     * @param pluginApkFile 插件APK文件
     * @return 签名验证是否通过
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
     * 验证并解析AndroidManifest.xml中的插件元数据
     *
     * @param pluginApkFile 插件APK文件
     * @return 解析后的插件配置，验证失败返回null
     */
    private fun validateAndParseConfig(pluginApkFile: File): PluginConfig? {
        Timber.tag(TAG).d("开始验证插件元数据配置: ${pluginApkFile.name}")

        try {
            // 获取PackageManager并解析APK
            val pm = context.packageManager
            val packageInfo =
                pm.getPackageArchiveInfo(
                    pluginApkFile.absolutePath,
                    PackageManager.GET_META_DATA,
                )

            if (packageInfo == null) {
                Timber.tag(TAG).e("无法解析插件APK包信息: ${pluginApkFile.name}")
                return null
            }

            val metaData = packageInfo.applicationInfo?.metaData
            if (metaData == null) {
                Timber.tag(TAG).e("插件AndroidManifest.xml中未找到元数据: ${pluginApkFile.name}")
                return null
            }

            // 读取插件元数据
            val pluginId = metaData.getString(META_PLUGIN_ID)
            val pluginVersion = metaData.getString(META_PLUGIN_VERSION)
            val pluginDescription = metaData.getString(META_PLUGIN_DESCRIPTION)
            val entryClass = metaData.getString(META_PLUGIN_ENTRY_CLASS)

            // 验证必要字段
            if (pluginId.isNullOrBlank()) {
                Timber
                    .tag(TAG)
                    .e("插件ID不能为空，请在AndroidManifest.xml中设置 meta-data: $META_PLUGIN_ID")
                return null
            }

            if (pluginVersion.isNullOrBlank()) {
                Timber
                    .tag(TAG)
                    .e("插件版本不能为空，请在AndroidManifest.xml中设置 meta-data: $META_PLUGIN_VERSION")
                return null
            }

            if (entryClass.isNullOrBlank()) {
                Timber
                    .tag(TAG)
                    .e("插件入口类不能为空，请在AndroidManifest.xml中设置 meta-data: $META_PLUGIN_ENTRY_CLASS")
                return null
            }

            val pluginConfig =
                PluginConfig(
                    pluginId = pluginId,
                    pluginVersion = pluginVersion,
                    pluginDescription = pluginDescription ?: "",
                    entryClass = entryClass,
                )

            Timber.tag(TAG).d("插件元数据配置验证通过: ${pluginConfig.pluginId}")
            return pluginConfig
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "解析插件元数据配置失败: ${e.message}")
            return null
        }
    }

    /**
     * 使用 PackageManager 解析 APK 文件中的静态广播接收器信息。
     * @param apkPath 插件 APK 的文件路径。
     * @return 解析出的静态广播信息列表。
     */
    private fun parseStaticReceivers(apkPath: String): List<StaticReceiverInfo> {
        Timber.tag(TAG).d("开始解析 StaticReceivers : $apkPath")
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
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "receiver" -> {
                                inReceiverTag = true
                                currentFilters = mutableListOf()
                                currentReceiverName =
                                    parser.getAttributeValue(ANDROID_NAMESPACE, "name")
                                currentReceiverEnabled =
                                    parser.getAttributeBooleanValue(
                                        ANDROID_NAMESPACE,
                                        "enabled",
                                        true,
                                    )
                                currentReceiverExported =
                                    parser.getAttributeBooleanValue(
                                        ANDROID_NAMESPACE,
                                        "exported",
                                        false,
                                    ) // 注意：默认值根据是否有filter会变，但为了安全我们默认false
                            }

                            "intent-filter" ->
                                if (inReceiverTag) {
                                    inFilterTag = true
                                    currentActions = mutableListOf()
                                    currentCategories = mutableListOf()
                                    currentSchemes = mutableListOf()
                                }

                            "action" ->
                                if (inFilterTag) {
                                    parser
                                        .getAttributeValue(ANDROID_NAMESPACE, "name")
                                        ?.let { currentActions?.add(it) }
                                }

                            "category" ->
                                if (inFilterTag) {
                                    parser
                                        .getAttributeValue(ANDROID_NAMESPACE, "name")
                                        ?.let { currentCategories?.add(it) }
                                }

                            "data" ->
                                if (inFilterTag) {
                                    parser
                                        .getAttributeValue(ANDROID_NAMESPACE, "scheme")
                                        ?.let { currentSchemes?.add(it) }
                                }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "intent-filter" ->
                                if (inFilterTag) {
                                    inFilterTag = false
                                    currentFilters?.add(
                                        IntentFilterInfo(
                                            actions = currentActions ?: emptyList(),
                                            categories = currentCategories ?: emptyList(),
                                            schemes = currentSchemes ?: emptyList(),
                                        ),
                                    )
                                }

                            "receiver" ->
                                if (inReceiverTag) {
                                    inReceiverTag = false
                                    if (!currentReceiverName.isNullOrBlank() && !currentFilters.isNullOrEmpty()) {
                                        receivers.add(
                                            StaticReceiverInfo(
                                                className = currentReceiverName,
                                                enabled = currentReceiverEnabled,
                                                exported = currentReceiverExported,
                                                intentFilters = currentFilters,
                                            ),
                                        )
                                        Timber
                                            .tag(TAG)
                                            .d("解析到静态广播: $currentReceiverName, filters: ${currentFilters.size}")
                                    }
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
     * 使用 PackageManager 解析 APK 文件中的 ContentProvider 信息。
     * @param apkPath 插件 APK 的文件路径。
     * @return 解析出的 Provider 信息列表。
     */
    @Suppress("DEPRECATION")
    private fun parseProviders(apkPath: String): List<ProviderInfo> {
        Timber.tag(TAG).d("开始解析 ContentProvider : $apkPath")
        val pm = context.packageManager
        try {
            val packageInfo =
                pm.getPackageArchiveInfo(
                    apkPath,
                    PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA,
                )

            val providerList = mutableListOf<ProviderInfo>()
            packageInfo?.providers?.forEach { provider ->
                val authorities = provider.authority?.split(";")?.filter { it.isNotBlank() }
                if (!authorities.isNullOrEmpty()) {
                    val metaDataList = mutableListOf<MetaDataInfo>()
                    provider.metaData?.keySet()?.forEach { key ->
                        val rawValue = provider.metaData.get(key)
                        when (rawValue) {
                            is Int -> {
                                metaDataList.add(
                                    MetaDataInfo(
                                        name = key,
                                        value = null,
                                        resource = rawValue,
                                    ),
                                )
                            }

                            else -> {
                                metaDataList.add(
                                    MetaDataInfo(
                                        name = key,
                                        value = rawValue?.toString(),
                                        resource = null,
                                    ),
                                )
                            }
                        }
                    }

                    providerList.add(
                        ProviderInfo(
                            className = provider.name,
                            authorities = authorities,
                            enabled = provider.enabled,
                            exported = provider.exported,
                            metaData = metaDataList,
                        ),
                    )
                    Timber
                        .tag(TAG)
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
     * 获取指定插件的安装目录
     */
    fun getPluginDirectory(pluginId: String): File {
        return File(pluginsDir, pluginId)
    }

    /**
     * 复制 APK 文件到指定的插件目录
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
     * 解压插件 APK 中的 so 库到插件目录
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
}
