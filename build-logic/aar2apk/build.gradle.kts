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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(buildLibs.plugins.gradle.plugin.publish)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

dependencies {
    implementation(gradleApi())
    implementation(buildLibs.android.gradle.api)
    implementation(buildLibs.kotlin.gradle.plugin)
}

gradlePlugin {
    group = "io.github.lnzz123"
    version = "1.0.0"
    website = "https://github.com/lnzz123/combolite"
    vcsUrl = "https://github.com/lnzz123/combolite.git"
    plugins {
        create("aarToApkPlugin") {
            id = "io.github.lnzz123.combolite-aar2apk"
            implementationClass = "com.combo.aar2apk.Aar2ApkPlugin"
            displayName = "AAR to APK Converter Plugin"
            tags = listOf("android", "aar", "apk")
            description = "A plugin to convert AAR files to APK files for plugin architecture"
        }
    }
}