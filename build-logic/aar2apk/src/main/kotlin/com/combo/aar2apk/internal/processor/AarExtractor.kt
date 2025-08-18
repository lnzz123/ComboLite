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