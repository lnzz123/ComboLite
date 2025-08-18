package com.combo.aar2apk.internal.utils

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.Closeable
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 封装任务日志，同时输出到控制台和文件
 */
@Suppress("NewApi")
internal class TaskLogger(
    project: Project,
    buildType: String,
    aarName: String
) : Closeable {
    private val gradleLogger: Logger = project.logger
    private val logWriter: PrintWriter
    private val logFile: File

    init {
        val logDir = project.layout.buildDirectory.dir("logs/aar2apk").get().asFile
        logDir.mkdirs()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val cleanAarName = File(aarName).nameWithoutExtension
        logFile = File(logDir, "$cleanAarName-$buildType-$timestamp.log")
        logWriter = PrintWriter(FileWriter(logFile, true))
    }

    fun start(taskName: String, outputDir: File) {
        logWriter.println("==================== [$taskName] - 构建日志 ====================")
        logWriter.println("  开始时间: ${LocalDateTime.now()}")
        logWriter.println("  输出目录: ${outputDir.absolutePath}")
        logWriter.println("======================================================================")
        logWriter.flush()
    }

    fun log(message: String, isError: Boolean = false) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val logMessage = "[$timestamp] $message"
        if (isError) {
            gradleLogger.error(logMessage)
        } else {
            gradleLogger.lifecycle(logMessage)
        }
        logWriter.println(logMessage)
        logWriter.flush()
    }

    override fun close() {
        logWriter.println("======================================================================")
        logWriter.println("  结束时间: ${LocalDateTime.now()}")
        logWriter.println("  日志文件: ${logFile.absolutePath}")
        logWriter.close()
        gradleLogger.lifecycle("✅ 日志已保存到: ${logFile.path}")
    }
}