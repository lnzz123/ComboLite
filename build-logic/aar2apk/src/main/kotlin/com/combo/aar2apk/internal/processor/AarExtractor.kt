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

import org.gradle.api.Project
import java.io.File

/**
 * 负责解压AAR文件
 */
internal class AarExtractor(private val project: Project) {
    fun extract(aarFile: File, workDir: File): File {
        val extractDir = File(workDir, "extracted")
        extractDir.deleteRecursively()
        extractDir.mkdirs()
        project.copy {
            from(project.zipTree(aarFile))
            into(extractDir)
        }
        return extractDir
    }
}