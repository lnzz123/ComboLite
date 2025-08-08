import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jctech.plugin.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jctech.plugin.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile =
                file("/Users/heshenglong/AndroidStudioProjects/ComposePluginSample/jctech.jks")
            storePassword = "he1755858138"
            keyAlias = "jctech"
            keyPassword = "he1755858138"
        }
        val properties = Properties()
        val localPropertyFile = project.rootProject.file("local.properties")
        if (localPropertyFile.canRead()) {
            properties.load(FileInputStream("$rootDir/local.properties"))
        }
        create("release") {
            storeFile =
                file("/Users/heshenglong/AndroidStudioProjects/ComposePluginSample/jctech.jks")
            keyAlias = "jctech"
            keyPassword = "he1755858138"
            storePassword = "he1755858138"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")

            //noinspection WrongGradleMethod
            kotlinOptions {
                freeCompilerArgs +=
                    listOf(
                        "-Xno-param-assertions",
                        "-Xno-call-assertions",
                        "-Xno-receiver-assertions",
                    )
            }

            packaging {
                resources {
                    excludes +=
                        listOf(
                            "DebugProbesKt.bin",
                            "kotlin-tooling-metadata.json",
                            "kotlin/**",
                        )
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(projects.pluginFramework.pluginCore)
    // common模块包含了所有依赖，构建fat-aar容易出现bug。
    implementation(projects.samplePlugin.common)
}