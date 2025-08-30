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

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.combo.dependencies"
    compileSdk = 36
}

dependencies {
    api(libs.material)
    api(libs.androidx.appcompat)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.ui.ktx)
    // ========== 基础Compose依赖（通过api暴露） ==========
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.material)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)

    // ========== Compose相关库 ==========
    api(libs.androidx.activity.compose)
    api(libs.androidx.navigation.compose)
    api(libs.androidx.lifecycle.viewModelCompose)
    api(libs.androidx.lifecycle.process)
    api(libs.androidx.material3.adaptive.navigation.suite)

    // ========== 生命周期相关依赖 ==========
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.savedstate.ktx)

    // ========== 图片加载和UI增强 ==========
    api(libs.landscapist.glide)
    api(libs.landscapist.animation)
    api(libs.landscapist.placeholder)
    api(libs.landscapist.palette)
    api(libs.shimmer)
    api(libs.lottie)
    api(libs.coil.kt)
    api(libs.coil.okhttp)
    api(libs.coil.kt.compose)
    api(libs.coil.kt.svg)
    api(libs.coil.kt.gif)

    // ========== 网络和序列化 ==========
    api(platform(libs.retrofit.bom))
    api(platform(libs.okhttp.bom))
    api(libs.retrofit)
    api(libs.retrofit.kotlinx.serialization)
    api(libs.okhttp.logging.interceptor)
    api(libs.sandwich)
    api(libs.kotlinx.serialization.json)
    api(libs.converter.gson)

    // ========== 工具库 ==========
    api(libs.timber)
    api(libs.accompanist.permissions)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.immutable.collection)

    // ========== 数据库 ==========
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)

    // ========== 依赖注入 ==========
    api(libs.koin.android)
    api(libs.koin.androidx.compose)

    // ========== 其他常用库 ==========
    api(libs.androidx.foundation.android)
}
