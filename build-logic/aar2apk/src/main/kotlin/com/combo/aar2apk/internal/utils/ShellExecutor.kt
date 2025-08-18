package com.combo.aar2apk.internal.utils

import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 封装命令行执行逻辑
 * @param execOperations 通过 @Inject 注入到Task中，再传递过来
 */
internal class ShellExecutor(
    private val execOperations: ExecOperations,
    private val logger: TaskLogger
) {
    fun execute(command: List<String>, workDir: File? = null) {
        val commandStr = command.joinToString(" ")
        logger.log("  EXEC: $commandStr")

        val output = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()

        val result = execOperations.exec {
            commandLine(command)
            workDir?.let { workingDir = it }
            standardOutput = output
            errorOutput = errorStream
            isIgnoreExitValue = true // 我们手动检查退出码
        }

        val stdOut = output.toString().trim()
        val stdErr = errorStream.toString().trim()

        if (stdOut.isNotEmpty()) {
            logger.log("  STDOUT: $stdOut")
        }

        if (result.exitValue != 0) {
            if (stdErr.isNotEmpty()) {
                logger.log("  STDERR: $stdErr", isError = true)
            }
            throw GradleException("命令执行失败 (退出码: ${result.exitValue}): $commandStr")
        }
    }
}