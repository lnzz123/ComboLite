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

package com.combo.core.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import timber.log.Timber
import java.io.File

/**
 * 插件签名校验器
 *
 * 用于检测插件APK文件的签名是否与宿主应用使用相同的JKS签名
 * 确保插件的安全性和完整性
 */
class SignatureValidator(
    context: Context,
) {
    var hostSignatures: MutableSet<Signature>?

    init {
        hostSignatures = getSigningSignatures(context, context.packageName)
    }

    /**
     * 校验插件APK的签名是否与宿主App一致。
     * 这是主要的校验方法。
     *
     * @param context 上下文
     * @param pluginApkFile 插件APK文件
     * @return true 如果签名一致，否则false
     */
    fun validate(
        context: Context,
        pluginApkFile: File,
    ): Boolean {
        if (!pluginApkFile.exists()) {
            Timber.w("Plugin file does not exist: %s", pluginApkFile.absolutePath)
            return false
        }

        // 1. 获取缓存的宿主签名
        val hostSigns = hostSignatures
        if (hostSigns.isNullOrEmpty()) {
            Timber.e("Could not get host signatures. Validation aborted.")
            return false
        }

        // 2. 获取插件的签名
        val pluginSigns =
            getSigningSignatures(context, pluginApkFile.absolutePath, isApkFile = true)
        if (pluginSigns.isNullOrEmpty()) {
            Timber.e(
                "Could not get plugin signatures from %s. Validation failed.",
                pluginApkFile.name,
            )
            return false
        }

        // 3. 比对签名集合
        val isValid = hostSigns.containsAll(pluginSigns)
        Timber.i(
            "Validation result for '%s': %s",
            pluginApkFile.name,
            if (isValid) "SUCCESS" else "FAILED",
        )

        return isValid
    }

    /**
     * 获取签名信息的核心方法，适配新旧API。
     *
     * @param context 上下文
     * @param source  包名或APK文件路径
     * @param isApkFile 标记source是APK文件还是包名
     * @return 签名集合，获取失败则返回null
     */
    @Suppress("DEPRECATION")
    private fun getSigningSignatures(
        context: Context,
        source: String,
        isApkFile: Boolean = false,
    ): MutableSet<Signature>? {
        val packageManager = context.packageManager
        return try {
            val packageInfo: PackageInfo? =
                if (isApkFile) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageManager.getPackageArchiveInfo(
                            source,
                            PackageManager.GET_SIGNING_CERTIFICATES,
                        )
                    } else {
                        packageManager.getPackageArchiveInfo(source, PackageManager.GET_SIGNATURES)
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageManager.getPackageInfo(
                            source,
                            PackageManager.GET_SIGNING_CERTIFICATES,
                        )
                    } else {
                        packageManager.getPackageInfo(source, PackageManager.GET_SIGNATURES)
                    }
                }

            if (packageInfo == null) return null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                if (signingInfo == null) return null
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.toMutableSet()
                } else {
                    signingInfo.signingCertificateHistory?.firstOrNull()?.let { mutableSetOf(it) }
                }
            } else {
                packageInfo.signatures?.toMutableSet()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get signatures for source: %s", source)
            null
        }
    }
}
