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

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.spotless)
    alias(libs.plugins.maven.publish.vanniktech)
}

android {
    namespace = "com.combo.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // 最小化依赖
    implementation(libs.androidx.core)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)

    // Compose核心（用于@Composable注解）
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    // 序列化支持
    implementation(libs.kotlinx.serialization.json)

    // 协程支持
    implementation(libs.kotlinx.coroutines.android)

    // Koin依赖注入（用于插件模块管理）
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Dex lib2 库
    implementation(libs.dexlib2)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    val groupId = "io.github.lnzz123"
    val artifactId = "combolite-core"
    val version = "1.0.0"
    coordinates(groupId, artifactId, version)
    pom {
        name.set("ComboLite Core")
        description.set("The next-generation Android plugin framework, born for Jetpack Compose, using 100% official APIs with 0 Hooks & 0 Reflection.")
        url.set("https://github.com/lnzz123/combolite")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("lnzz123")
                name.set("贵州君城网络科技有限公司")
                email.set("1755858138@qq.com")
                organization.set("贵州君城网络科技有限公司")
                organizationUrl.set("http://www.97network.com")
            }
        }
        scm {
            connection.set("scm:git:github.com/lnzz123/combolite.git")
            developerConnection.set("scm:git:ssh://github.com/lnzz123/combolite.git")
            url.set("https://github.com/lnzz123/combolite/tree/main")
        }
    }
}