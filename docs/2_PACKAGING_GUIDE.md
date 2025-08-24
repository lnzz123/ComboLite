# 插件打包指南

欢迎阅读 `ComboLite` 的插件打包指南！

“打包”是将你的插件模块（无论是 `Library` 还是 `Application` 类型）转化为一个可被宿主动态加载、安装和运行的独立
APK 文件的关键步骤。掌握正确的打包策略，对于实现插件的轻量化、规避依赖冲突、提升项目维护性至关重要。

本指南将深入探讨 `ComboLite` 支持的两种打包方案，并重点介绍我们官方推荐的、基于 `aar2apk` Gradle 插件的
`Library` 模块打包工作流。

-----

## 核心概念：理解打包策略的基石

在深入具体操作之前，让我们先掌握两个决定打包策略的核心概念。

### 1\. 两种插件形态：Library vs Application

`ComboLite` 赋予了你极大的灵活性，可以将两种不同形态的 Android 模块开发为插件：

* 📦 **Library 模块 (推荐)**

    * **本质**: 一个标准的 `com.android.library` 模块 (产物为 AAR)。
    * **特点**: 轻量、非独立。它不包含 Kotlin 标准库、AndroidX 等公共依赖，默认这些都由宿主 App
      在运行时提供。这是构建“超级App”和追求极致性能的首选。
    * **打包方式**: 需要通过我们提供的 `aar2apk` Gradle 插件来“升格”为一个可安装的 APK。

* 📱 **Application 模块**

    * **本质**: 一个标准的 `com.android.application` 模块 (产物为 APK)。
    * **特点**: 独立、自包含。它像一个微型 App，将自身的绝大部分依赖都打包进去。
    * **打包方式**: 使用 Android 官方的标准打包流程即可。

### 2\. 依赖作用域：`compileOnly` vs `implementation`

正确使用 Gradle 的依赖作用域，是实现插件“轻量化”和“依赖共享”的魔法核心。

* ✅ **`compileOnly` (插件首选)**

    * **含义**: “仅编译时依赖”。它告诉编译器：“这个库在**编译我的插件代码**时是必需的，但请**不要**
      将它打包进最终的插件 APK 里。我承诺，在运行时，宿主 App 会提供这个库。”
    * **用途**: 这是插件模块**最优先、最常用**的作用域。所有你期望由宿主提供的公共库（如
      `comboLite-core`、`Kotlin`、`AndroidX`、`OkHttp` 等），都应使用 `compileOnly`。

* ⚠️ **`implementation`**

    * **含义**: “实现时依赖”。它告诉编译器：“这个库不仅编译时需要，运行时也需要，请务必将它**打包**
      进最终的产物里。”
    * **用途**:
        1. 在打包 `Application` 模块插件时，用于引入该插件的私有依赖。
        2. 在打包 `Library` 模块插件时，仅当你计划使用 `aar2apk` 的高级功能（如
           `includeDependenciesDex`）来**特意**打包某个依赖时，才需要将它从 `compileOnly` 修改为
           `implementation`。

> **黄金法则**:
> **插件开发，`compileOnly` 是常态，`implementation` 是例外。**

-----

## 方案一：打包 Library 模块（官方推荐）

这是 `ComboLite` 最核心、最强大的打包方式。我们内置了一个名为 `aar2apk` 的 Gradle 插件，专门用于将轻量级的
`Library` 模块一键转化为功能完备的插件 APK。

### 1\. 引入并应用插件

首先，确保 `aar2apk` 插件本身能被你的项目找到。

① **在项目根 `settings.gradle.kts` 中引入插件项目**:

```kotlin
// in your project's /settings.gradle.kts
includeBuild("../ComboLite") {
    dependencySubstitution {
        // 当您在项目中依赖 aar2apk 插件时，Gradle 会自动将其替换为本地的 build-logic 模块
        substitute(module("com.combo.aar2apk")).using(project(":build-logic"))
    }
}
```

② **在项目根 `build.gradle.kts` 中应用插件**:

```kotlin
// in your project's root /build.gradle.kts
plugins {
    // ... 其他插件
    id("com.combo.aar2apk")
}
```

### 2\. 配置打包任务

所有配置都在项目根 `build.gradle.kts` 的 `aar2apk` 配置块中完成。你可以进行全局签名配置，并为每个需要打包的插件模块指定独立的打包策略。

```kotlin
// in your project's root /build.gradle.kts
aar2apk {
    // a. (可选) 配置全局签名信息
    // 如果配置了，所有插件都将使用此签名。release 包必须签名。
    signing {
        keystorePath.set(rootProject.file("your_keystore.jks").absolutePath)
        keystorePassword.set("your_keystore_password")
        keyAlias.set("your_key_alias")
        keyPassword.set("your_key_password")
    }

    // b. 声明所有需要打包为插件的 Library 模块
    modules {
        // 策略一：默认最小化打包 (最常用)
        // 不打包任何外部依赖的代码和资源，插件体积最小。
        module(":plugin-user")
        module(":plugin-settings")

        // 策略二：打包部分依赖 (处理特殊情况)
        // 比如 :plugin-reader 依赖了某个带 drawable 资源的库 A，且宿主没有这个库
        module(":plugin-reader") {
            includeDependenciesRes.set(true) // 只打包外部依赖的资源
            // 注意：此时库 A 在 :plugin-reader 中必须用 implementation 引入
        }

        // 策略三：打包所有依赖 (极少使用，风险高)
        // 适用于该插件依赖了宿主或其他插件都没有的、完全私有的库。
        module(":plugin-private") {
            includeAllDependencies() // 便捷方法，等同于将下面四个都设为 true
            
            // 或者精细化控制
            // includeDependenciesDex.set(true)    // 打包依赖的代码
            // includeDependenciesRes.set(true)    // 打包依赖的资源
            // includeDependenciesAssets.set(true) // 打包依赖的 assets
            // includeDependenciesJni.set(true)    // 打包依赖的 JNI 库
        }
    }
}
```

### 3\. 可用打包指令 (Gradle Tasks)

配置完成后，`aar2apk` 插件会自动为你的项目生成一系列便捷的 Gradle 任务。

你可以在 **Android Studio 的 Gradle 任务面板**（通常在右侧）的 `Plugin APKs` 分组下找到它们，双击即可执行。

*(请替换为您的实际截图)*

或者，你也可以在终端中执行以下命令：

```bash
# [推荐] 一键打包所有已配置的 Library 插件 (Release 版本)
./gradlew buildAllReleasePluginApks

# [推荐] 一键打包所有已配置的 Library 插件 (Debug 版本)
./gradlew buildAllDebugPluginApks

# 打包单个插件 (Release 版本)
./gradlew :plugin-user:buildReleasePluginApk

# 打包单个插件 (Debug 版本)
./gradlew :plugin-user:buildDebugPluginApk

# 清理所有插件构建产物 (APKs, 日志, 临时文件)
./gradlew cleanAllPluginApks
```

打包产物将位于项目根目录的 `build/outputs/plugin-apks/` 文件夹下，并已按 `debug` 和 `release` 自动分类。

### 4\. 优劣势分析

* ✅ **优点**:
    * **体积极致轻量**: APK 体积通常只有几十到几百 KB，更新和下载成本极低。
    * **杜绝依赖冲突**: 所有插件共享宿主提供的同一份依赖，从根本上避免了因版本不一致导致的运行时崩溃。
    * **统一依赖管理**: 依赖版本由宿主统一升级和管理，维护成本更低。
* ⚠️ **权衡**:
    * **依赖宿主环境**: 插件的运行强依赖于宿主。如果宿主未能提供其在 `compileOnly` 中声明的依赖，插件将因
      `ClassNotFoundException` 而启动失败，并被框架自动熔断禁用。

-----

## 方案二：打包 Application 模块（备选方案）

将标准的 `com.android.application` 模块作为插件，是一种传统的、在特定场景下仍然有用的方式。

### 1\. 配置步骤

**① 添加核心依赖**
在插件模块的 `build.gradle.kts` 中，将框架核心库声明为 `compileOnly`。

```kotlin
// in your-application-plugin/build.gradle.kts
dependencies {
    compileOnly(projects.comboLiteCore)
    // 这个 gson 库将被打包进插件 APK
    implementation("com.google.code.gson:gson:2.9.0")
}
```

**② (❗极其重要) 配置 Package ID**
为避免与宿主或其他插件的资源ID冲突（`R.java`），**必须**为每个 `Application` 插件模块手动指定一个唯一的
`Package ID`。

```kotlin
// in your-application-plugin/build.gradle.kts
android {
    // ...
    aaptOptions {
        // 这个 16 进制值必须在所有 Application 插件中唯一
        additionalParameters("--package-id", "0x80") 
    }
}
```

> **注意**: `Package ID` 的值域为 `0x02` 到 `0x7F`。宿主 App 的 ID 是 `0x7F`。请为你的每个 Application
> 插件分配一个不同的、在此范围内的值，例如插件 A 用 `0x7E`，插件 B 用 `0x7D`，以此类推。

### 2\. 执行打包

使用 AGP 提供的标准任务即可完成打包。

```bash
./gradlew :your-application-plugin:assembleRelease
```

### 3\. 优劣势分析

* ✅ **优点**:
    * **高度独立**: 插件自包含所有依赖，不依赖宿主的外部环境，部署简单。
    * **无兼容性烦恼**: 无需担心宿主是否提供了插件所需的依赖库或版本。
* ⚠️ **权衡**:
    * **体积较大**: 由于打包了所有依赖，插件 APK 的体积会相对臃肿。
    * **潜在依赖风险**: 如果插件打包的库与宿主中的版本不一致，可能引发难以排查的运行时错误。

-----

## 如何选择？

| 业务场景                      | 推荐方案                       | 理由                                  |
|:--------------------------|:---------------------------|:------------------------------------|
| **UI组件库、工具类、通用业务**        | **✅ Library 模块 (aar2apk)** | **首选方案**。功能单一，体积小，适合作为共享资源被频繁更新和复用。 |
| **拥有大量插件的“超级App”**        | **✅ Library 模块 (aar2apk)** | 最大化地复用公共依赖，显著减少应用的总体积和内存占用。         |
| **对插件更新速度和大小有极致要求**       | **✅ Library 模块 (aar2apk)** | 极小的包体积使得动态下发和热更新的体验近乎无感。            |
| **大型、独立的功能模块** (如购物、游戏中心) | **⚠️ Application 模块**      | 业务复杂，依赖众多，需要高度内聚和独立性。               |
| **提供给第三方集成的插件**           | **⚠️ Application 模块**      | 无法控制宿主环境，必须自包含所有依赖以确保稳定运行。          |

-----

## 重要实践建议与风险警告

### 深度警告：谨慎打包完整依赖

虽然 `aar2apk` 插件提供了 `includeAllDependencies()` 等将依赖打包进插件的能力，但这应该被视为一个*
*处理特殊情况的备用方案，而不是常规操作**。

> **在绝大多数情况下，我们强烈建议您采用默认的最小化打包模式。**

打包完整依赖可能会引入一系列严重且难以排查的问题：

* **类重复冲突 (Class Duplication)**: 如果插件打包了 `OkHttp 4.9.0`，而宿主或其他插件使用了
  `OkHttp 4.10.0`，运行时可能会因为类定义不一致而导致 `NoSuchMethodError`、`ClassCastException`
  等各种致命崩溃。
* **资源重复与覆盖 (Resource Duplication)**: 多个插件或宿主包含同名的资源，可能会导致运行时加载的资源非你所想，造成
  UI 错乱。
* **APK 体积冗余**: 重复打包相同的依赖库，会显著增加插件 APK 的体积和最终应用的总大小。

**经验法则**：只在你**完全确定**某个依赖库是此插件**专属私有**，且不会与宿主或其他任何插件产生冲突时，才考虑将其打包进去。

### 特别提示：UI 技术选型对打包策略的影响

在插件化场景中，您选择的 UI 构建技术（Jetpack Compose 或 XML）会直接影响到打包的复杂性。

* **Jetpack Compose (推荐)**: 与 `ComboLite` 的 `compileOnly` 依赖共享策略完美契合，几乎没有兼容性问题。
* **XML 布局**: 在 `compileOnly` 模式下，存在**跨模块资源引用困难**的问题，可能导致编译失败或需要更复杂的打包配置。

> **我们强烈建议您在插件中优先使用 Jetpack Compose**。
> 关于使用 XML 可能遇到的具体问题及其解决方案的深入探讨，请参阅 *
*[[四大组件指南](./4_COMPONENTS_GUIDE.md)](./4_COMPONENTS_GUIDE.md) 中的“UI 实现技术选型”章节**。