import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    implementation(gradleApi())
    implementation(buildLibs.android.gradle.api)
    implementation(buildLibs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("aarToApkPlugin") {
            id = "com.combo.aar2apk"
            implementationClass = "com.combo.aar2apk.Aar2ApkPlugin"
            displayName = "AAR to APK Converter Plugin"
            description = "A plugin to convert AAR files to APK files for plugin architecture"
        }
    }
}