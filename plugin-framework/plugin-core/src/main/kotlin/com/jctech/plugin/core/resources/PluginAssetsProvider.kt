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

package com.jctech.plugin.core.resources

import android.content.res.AssetFileDescriptor
import android.content.res.loader.AssetsProvider
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * 插件 AssetsProvider 实现
 *
 * 为 ResourcesProvider 提供访问插件 assets 文件的能力
 */
@RequiresApi(Build.VERSION_CODES.R)
class PluginAssetsProvider(
    private val pluginFile: File,
) : AssetsProvider {
    override fun loadAssetFd(
        path: String,
        accessMode: Int,
    ): AssetFileDescriptor? =
        try {
            ZipFile(pluginFile).use { zipFile ->
                val assetPath = "assets/$path"
                val entry = zipFile.getEntry(assetPath)

                if (entry != null) {
                    // 将 assets 文件提取到临时文件
                    val tempFile =
                        File.createTempFile("plugin_assets_", "_${path.replace("/", "_")}")
                    tempFile.deleteOnExit()

                    zipFile.getInputStream(entry).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 使用正确的 API 创建 AssetFileDescriptor
                    val parcelFd =
                        ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    AssetFileDescriptor(parcelFd, 0, entry.size)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "插件 Assets 资源加载失败: $path")
            null
        }
}
