/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利。
 *
 * 本软件（包括但不限于代码、文档、资源文件等）受《中华人民共和国著作权法》及相关法律法规保护。
 * 未经本公司书面授权，任何单位或个人不得：
 * 1. 以任何形式复制、传播、修改、分发本软件的全部或部分内容；
 * 2. 将本软件用于商业目的或未经授权的第三方项目；
 * 3. 删除或篡改本软件中的版权声明、商标标识及技术标识。
 *
 * 违反上述条款者，本公司将依法追究其民事及刑事责任，并有权要求赔偿因此造成的全部经济损失。
 *
 * 授权许可请联系：贵州君城网络科技有限公司法律事务部
 * 邮箱：1755858138@qq.com
 * 电话：+86-175-85074415
 */

package com.jctech.plugin.core.installer

import android.app.Application
import android.content.pm.PackageManager
import com.jctech.plugin.core.model.PluginInfo
import com.jctech.plugin.core.model.PluginState
import com.jctech.plugin.core.security.SignatureValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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
        private const val PLUGINS_DIR = "plugins"

        // 插件元数据的键名
        private const val META_PLUGIN_ID = "plugin.id"
        private const val META_PLUGIN_VERSION = "plugin.version"
        private const val META_PLUGIN_DESCRIPTION = "plugin.description"
        private const val META_PLUGIN_ENTRY_CLASS = "plugin.entryClass"
    }

    private val signatureValidator = SignatureValidator(context)
    private val pluginsDir: File by lazy {
        File(context.filesDir, PLUGINS_DIR).apply {
            if (!exists()) {
                mkdirs()
                Timber.tag(TAG).d("创建插件目录: $absolutePath")
            }
        }
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
        data class Success(val pluginInfo: PluginInfo) : InstallResult()
        data class Failure(val reason: String, val exception: Throwable? = null) : InstallResult()
    }

    /**
     * 异步安装插件（支持版本检查和强制覆盖）
     *
     * @param pluginApkFile 插件APK文件
     * @param forceOverwrite 是否强制覆盖安装，默认为false
     * @return 安装结果
     */
    suspend fun installPlugin(pluginApkFile: File, forceOverwrite: Boolean = false): InstallResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("开始异步安装插件: ${pluginApkFile.name}, forceOverwrite: $forceOverwrite")

        try {
            // 1. 检查文件是否存在
            if (!pluginApkFile.exists()) {
                val reason = "插件文件不存在: ${pluginApkFile.absolutePath}"
                Timber.tag(TAG).e(reason)
                return@withContext InstallResult.Failure(reason)
            }

            // 2. 检查并解析AndroidManifest.xml中的插件元数据（优先进行轻量级检查）
            val pluginConfig = validateAndParseConfig(pluginApkFile)
                ?: return@withContext InstallResult.Failure("插件配置元数据验证失败")

            // 3. 验证插件APK签名（较重的验证操作）
            if (!validateSignature(pluginApkFile)) {
                val reason = "插件签名验证失败: ${pluginApkFile.name}"
                Timber.tag(TAG).e(reason)
                return@withContext InstallResult.Failure(reason)
            }

            // 4. 检查插件是否已安装并进行版本比较
            val existingPlugin = xmlManager.getPluginById(pluginConfig.pluginId)
            if (existingPlugin != null && !forceOverwrite) {
                val newVersion = pluginConfig.pluginVersion
                val currentVersion = existingPlugin.version
                
                val versionComparison = compareVersions(newVersion, currentVersion)
                if (versionComparison <= 0) {
                    val reason = "插件 ${pluginConfig.pluginId} 已安装更高或相同版本 ($currentVersion)，新版本 ($newVersion) 不能覆盖安装"
                    Timber.tag(TAG).w(reason)
                    return@withContext InstallResult.Failure(reason)
                }
                
                Timber.tag(TAG).i("检测到插件版本升级: ${pluginConfig.pluginId} ($currentVersion -> $newVersion)")
            }

            // 5. 如果是更新安装，备份当前插件文件
            var backupFile: File? = null
            if (existingPlugin != null) {
                val currentPluginFile = File(existingPlugin.path)
                if (currentPluginFile.exists()) {
                    backupFile = File("${existingPlugin.path}.backup")
                    try {
                        currentPluginFile.copyTo(backupFile, overwrite = true)
                        Timber.tag(TAG).d("当前插件文件已备份: ${backupFile.absolutePath}")
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "备份当前插件文件失败，继续安装")
                        backupFile = null
                    }
                }
            }

            // 6. 异步复制插件文件到目标目录
            val targetFile = copyPluginFile(pluginApkFile, pluginConfig.pluginId)
            if (targetFile == null) {
                // 恢复备份文件
                if (backupFile?.exists() == true && existingPlugin != null) {
                    try {
                        val currentPluginFile = File(existingPlugin.path)
                        backupFile.copyTo(currentPluginFile, overwrite = true)
                        backupFile.delete()
                        Timber.tag(TAG).i("已恢复备份文件")
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "恢复备份文件失败")
                    }
                }
                return@withContext InstallResult.Failure("插件文件复制失败")
            }

            // 7. 记录或更新插件信息到plugins.xml
            val pluginInfo = PluginInfo(
                pluginId = pluginConfig.pluginId,
                version = pluginConfig.pluginVersion,
                path = targetFile.absolutePath,
                entryClass = pluginConfig.entryClass,
                description = pluginConfig.pluginDescription,
                status = existingPlugin?.status ?: PluginState.enabled,
                installTime = existingPlugin?.installTime ?: System.currentTimeMillis(), // 更新保持原安装时间
            )

            if (existingPlugin != null) {
                xmlManager.updatePlugin(pluginInfo)
                Timber.tag(TAG).i("插件异步更新成功: ${pluginConfig.pluginId} (${existingPlugin.version} -> ${pluginConfig.pluginVersion})")
            } else {
                xmlManager.addPlugin(pluginInfo)
                Timber.tag(TAG).i("插件异步安装成功: ${pluginConfig.pluginId}")
            }
            
            xmlManager.flushToDisk() // 立即保存到磁盘

            // 8. 清理备份文件
            if (backupFile?.exists() == true) {
                backupFile.delete()
                Timber.tag(TAG).d("备份文件已清理")
            }

            InstallResult.Success(pluginInfo)
        } catch (e: Exception) {
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
        Timber.tag(TAG).i("开始卸载插件: $pluginId")

        try {
            // 1. 从XML管理器中获取插件信息
            val pluginInfo = xmlManager.getPluginById(pluginId)
            if (pluginInfo == null) {
                Timber.tag(TAG).w("插件不存在: $pluginId")
                return false
            }

            // 2. 删除插件文件
            val pluginFile = File(pluginInfo.path)
            if (pluginFile.exists()) {
                val deleted = pluginFile.delete()
                if (deleted) {
                    Timber.tag(TAG).d("插件文件删除成功: ${pluginFile.absolutePath}")
                } else {
                    Timber.tag(TAG).w("插件文件删除失败: ${pluginFile.absolutePath}")
                }
            } else {
                Timber.tag(TAG).w("插件文件不存在: ${pluginFile.absolutePath}")
            }

            // 3. 从XML管理器中移除插件记录
            val removed = xmlManager.removePlugin(pluginId)
            if (removed) {
                xmlManager.flushToDisk() // 立即保存到磁盘
                Timber.tag(TAG).i("插件卸载成功: $pluginId")
                return true
            } else {
                Timber.tag(TAG).e("从XML管理器中移除插件记录失败: $pluginId")
                return false
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "卸载插件时发生异常: ${e.message}")
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
    private fun compareVersions(version1: String, version2: String): Int {
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
                Timber.tag(TAG).e("插件ID不能为空，请在AndroidManifest.xml中设置 meta-data: $META_PLUGIN_ID")
                return null
            }

            if (pluginVersion.isNullOrBlank()) {
                Timber.tag(TAG).e("插件版本不能为空，请在AndroidManifest.xml中设置 meta-data: $META_PLUGIN_VERSION")
                return null
            }

            if (entryClass.isNullOrBlank()) {
                Timber.tag(TAG).e("插件入口类不能为空，请在AndroidManifest.xml中设置 meta-data: $META_PLUGIN_ENTRY_CLASS")
                return null
            }

            val pluginConfig = PluginConfig(
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
     * 异步复制插件文件到目标目录
     *
     * @param sourceFile 源插件文件
     * @param pluginId 插件ID
     * @return 复制后的目标文件，复制失败返回null
     */
    private suspend fun copyPluginFile(sourceFile: File, pluginId: String): File? = withContext(Dispatchers.IO) {
        val targetFileName = "$pluginId.plugin"
        val targetFile = File(pluginsDir, targetFileName)

        Timber.tag(TAG).d("开始异步复制插件文件: ${sourceFile.name} -> ${targetFile.absolutePath}")

        try {
            // 如果目标文件已存在，先删除
            if (targetFile.exists()) {
                targetFile.delete()
                Timber.tag(TAG).d("删除已存在的目标文件: ${targetFile.absolutePath}")
            }

            // 异步复制文件，使用缓冲区提高效率
            val buffer = ByteArray(8192) // 8KB缓冲区
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            // 验证复制结果
            if (!targetFile.exists() || targetFile.length() != sourceFile.length()) {
                Timber.tag(TAG).e("文件复制验证失败")
                return@withContext null
            }

            // 设置文件为只读权限，防止Android系统报"Writable dex file"错误
            if (!targetFile.setReadOnly()) {
                Timber.tag(TAG).w("设置插件文件为只读失败: ${targetFile.absolutePath}")
            } else {
                Timber.tag(TAG).d("插件文件已设置为只读: ${targetFile.absolutePath}")
            }

            Timber.tag(TAG).d("插件文件异步复制成功: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "异步复制插件文件失败: ${e.message}")
            // 清理可能的不完整文件
            if (targetFile.exists()) {
                targetFile.delete()
            }
            null
        }
    }
}
