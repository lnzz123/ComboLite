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

// 启用类型安全的项目访问器功能预览
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    includeBuild("build-logic")
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

rootProject.name = "ComboLite"
include(":app")
include(":dependencies")
// 插件框架模块
include(":comboLite-core")
include(":sample-plugin:common")
include(":sample-plugin:home")
include(":sample-plugin:guide")
include(":sample-plugin:sample")
include(":sample-plugin:setting")
include(":sample-plugin:example")
