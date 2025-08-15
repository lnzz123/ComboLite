plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.combo.dependencies"
    compileSdk = 36
}

dependencies {

    api(libs.androidx.appcompat)
    api(libs.material)
    // ========== 基础Compose依赖（通过api暴露） ==========
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.animation)
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
