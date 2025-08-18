/*
 * Copyright (c) 2025. 深圳市德为信息技术有限公司, 深圳市诠云科技有限公司 保留所有权利。
 *
 * 根据《中华人民共和国著作权法》、《计算机软件保护条例》及相关法律法规，
 * 本软件著作权归属于深圳市德为信息技术有限公司与深圳市诠云科技有限公司共同所有，
 * 任何单位或个人未经书面授权不得复制、修改、分发或用于商业用途。
 * （本声明适用于本项目所有源代码、资源配置文件及文档资料）
 */

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("buildLibs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}
include(":aar2apk")
