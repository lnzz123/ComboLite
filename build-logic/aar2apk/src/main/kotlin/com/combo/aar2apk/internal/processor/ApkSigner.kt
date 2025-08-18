package com.combo.aar2apk.internal.processor

import com.combo.aar2apk.SigningConfig
import com.combo.aar2apk.internal.model.SdkInfo
import com.combo.aar2apk.internal.utils.ShellExecutor
import com.combo.aar2apk.internal.utils.TaskLogger
import java.io.File

/**
 * 负责对APK进行签名
 */
internal class ApkSigner(
    private val shellExecutor: ShellExecutor,
    private val sdkInfo: SdkInfo,
    private val logger: TaskLogger
) {
    fun sign(unsignedApk: File, outputFile: File, config: SigningConfig) {
        logger.log("步骤6: 签名APK")
        outputFile.parentFile.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        shellExecutor.execute(
            listOf(
                sdkInfo.getTool("apksigner"), "sign",
                "--ks", config.keystorePath.get(),
                "--ks-pass", "pass:${config.keystorePassword.get()}",
                "--ks-key-alias", config.keyAlias.get(),
                "--key-pass", "pass:${config.keyPassword.get()}",
                "--min-sdk-version", "21",
                "--v4-signing-enabled", "false",
                "--out", outputFile.absolutePath,
                unsignedApk.absolutePath
            )
        )
    }
}