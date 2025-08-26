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