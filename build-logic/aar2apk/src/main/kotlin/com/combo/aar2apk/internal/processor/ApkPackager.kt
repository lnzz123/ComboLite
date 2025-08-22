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

package com.combo.aar2apk.internal.processor

import com.combo.aar2apk.internal.utils.ShellExecutor
import com.combo.aar2apk.internal.utils.TaskLogger
import java.io.File

/**
 * 负责将各类文件打包进APK
 */
internal class ApkPackager(
    private val shellExecutor: ShellExecutor,
    private val logger: TaskLogger
) {
    /**
     * 将 DEX 文件添加到 APK 的根目录。
     */
    fun addDex(apkFile: File, dexFile: File?) {
        if (dexFile == null || ! dexFile.exists()) return
        logger.log("步骤4.1: 添加DEX文件到APK") // 调整了日志步骤编号，使其更连贯
        shellExecutor.execute(
            listOf("jar", "uf", apkFile.absolutePath, "-C", dexFile.parent, dexFile.name)
        )
    }

    /**
     * 合并多个 JNI 目录，并将最终的 'lib' 目录添加到 APK 中。
     * @param apkFile 目标未签名 APK 文件。
     * @param jniDirs 一个包含多个 jni 目录的集合。每个目录应包含 ABI 子目录（如 arm64-v8a）。
     */
    fun addNativeLibs(apkFile: File, jniDirs: Set<File>) {
        if (jniDirs.isEmpty()) return
        logger.log("步骤4.2: 添加Native库到APK")

        // 1. 创建一个临时的暂存目录，用于存放合并后的 'lib' 文件夹。
        val stagingDir = File(apkFile.parentFile, "temp_jni_staging").apply { mkdirs() }
        val mergedLibDir = File(stagingDir, "lib").apply { mkdirs() }

        // 2. 遍历所有来源的 jni 目录
        jniDirs.forEach { jniDir ->
            if (jniDir.exists() && jniDir.isDirectory) {
                // 将每个 jni 目录下的 ABI 文件夹（如 arm64-v8a）复制到合并目录中
                jniDir.listFiles()?.forEach { abiDir ->
                    if (abiDir.isDirectory) {
                        val targetAbiDir = File(mergedLibDir, abiDir.name)
                        // 递归复制，overwrite=true 表示如果文件已存在则覆盖（后写入的会覆盖先写入的）
                        abiDir.copyRecursively(targetAbiDir, overwrite = true)
                    }
                }
            }
        }

        // 3. 如果合并后的 'lib' 目录不为空，则将其添加到 APK 中
        if (mergedLibDir.listFiles()?.isNotEmpty() == true) {
            // 使用 'jar' 命令将 'lib' 目录添加到 APK 的根路径
            shellExecutor.execute(
                listOf("jar", "uf", apkFile.absolutePath, "-C", stagingDir.absolutePath, "lib")
            )
        }

        // 4. 清理临时目录
        stagingDir.deleteRecursively()
    }

    /**
     * 合并多个 assets 目录，并将最终的 'assets' 目录添加到 APK 中。
     * @param apkFile 目标未签名 APK 文件。
     * @param assetDirs 一个包含多个 assets 目录的集合。
     */
    fun addAssets(apkFile: File, assetDirs: Set<File>) {
        if (assetDirs.isEmpty()) return
        logger.log("步骤4.3: 添加Assets到APK")

        // 1. 创建一个临时的暂存目录，用于存放合并后的 'assets' 文件夹。
        val stagingDir = File(apkFile.parentFile, "temp_assets_staging").apply { mkdirs() }
        val mergedAssetsDir = File(stagingDir, "assets").apply { mkdirs() }

        // 2. 遍历所有来源的 assets 目录，并将其内容复制到合并目录中
        assetDirs.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                // 递归复制，实现文件合并
                dir.copyRecursively(mergedAssetsDir, overwrite = true)
            }
        }

        // 3. 如果合并后的 'assets' 目录不为空，则将其添加到 APK 中
        if (mergedAssetsDir.listFiles()?.isNotEmpty() == true) {
            // 使用 'jar' 命令将 'assets' 目录添加到 APK 的根路径
            shellExecutor.execute(
                listOf("jar", "uf", apkFile.absolutePath, "-C", stagingDir.absolutePath, "assets")
            )
        }

        // 4. 清理临时目录
        stagingDir.deleteRecursively()
    }
}