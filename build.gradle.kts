/*
 *
 * * Copyright (c) 2025, 贵州君城网络科技有限公司
 * *
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * *
 * * http://www.apache.org/licenses/LICENSE-2.0
 * *
 * * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 *
 */

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.aar2apk)
}

aar2apk {
    modules {
        module(":sample-plugin:common")
        module(":sample-plugin:home")
        module(":sample-plugin:guide")
        module(":sample-plugin:example")
        module(":sample-plugin:setting")
    }

    // 配置签名信息
    signing {
        keystorePath.set(rootProject.file("jctech.jks").absolutePath)
        keystorePassword.set("he1755858138")
        keyAlias.set("jctech")
        keyPassword.set("he1755858138")
    }
}