// 启用类型安全的项目访问器功能预览
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            content {
                includeGroupByRegex(".*") // 从阿里云获取所有其他插件
            }
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            mavenContent {
                snapshotsOnly()
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            content {
                includeGroupByRegex(".*") // 尝试从阿里云获取所有其他依赖
            }
        }
        maven { url = uri("https://jitpack.io") }

        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            mavenContent {
                snapshotsOnly()
            }
        }
    }
}

rootProject.name = "ArcLite"
include(":app")
// 插件框架模块
include(":plugin-framework:plugin-core")
include(":sample-plugin:common")
include(":sample-plugin:home")
include(":sample-plugin:screen")
include(":dependencies")
