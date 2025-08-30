<p align="right"> 
   <b>简体中文</b> | <a href="./README_en.md">English</a> 
</p> 

<p align="center">
  <img src="image/banner.png" width="1280" alt="ComboLite Logo" style="pointer-events: none;">
</p>

<p align="center">
  <strong>专为 Jetpack Compose 而生，100% 官方 API，0 Hook & 0 反射 的下一代 Android 插件化框架。</strong>
  <br />
  <em>现代、稳定、灵活，助您轻松构建“万物皆可插拔”的动态化应用。</em>
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84.svg"/>
  <img alt="API" src="https://img.shields.io/badge/API-24%2B%20(Android%207.0)-blue.svg"/>
  <a href="https://kotlinlang.org/"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2.0-7F52FF.svg"/></a>
  <a href="https://developer.android.com/jetpack/compose"><img alt="Compose" src="https://img.shields.io/badge/Compose-1.9.0-FF6F00.svg"/></a>
  <a href="https://developer.android.com/studio/releases/gradle-plugin"><img alt="AGP" src="https://img.shields.io/badge/AGP-8.12.0-007BFF.svg"/></a>
  <a href="https://gradle.org/"><img alt="Gradle" src="https://img.shields.io/badge/Gradle-8.13-6C757D.svg"/></a>
  <a href="https://github.com/lnzz123/ComboLite/blob/main/LICENSE"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://github.com/lnzz123"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-lnzz123-181717.svg"/></a>
</p>

---

<details>
<summary>📚 <b>目录 (Table of Contents)</b></summary>

- [🤔 为什么选择 ComboLite？](#-为什么选择-combolite)
- [✨ 核心特性](#-核心特性)
- [🚀 开始集成](#-开始集成-getting-started)
- [📚 文档列表](#-接下来做什么)
- [🆚 框架对比与技术选型](#-框架对比与技术选型)
- [🤝 如何贡献](#-如何贡献)
- [❤️ 支持与赞助](#-支持与赞助-support--sponsor)
- [许可](#许可-license)

</details>

---

### 🤔 为什么选择 ComboLite？

随着 Android 生态的演进，诞生于 View 时代的插件化框架已显得力不从心。它们或已停止维护，或严重依赖充满风险的
**非公开API (Hook与反射)**，在面临系统频繁更新时，兼容性问题频发，接入和维护成本极高。

**`ComboLite` 的诞生，正是为了终结这一困境。** 我们回归官方、拥抱简单，以完全公开的 API 为基石，实现了 *
*0 Hook、0 反射** 的纯净架构，从根本上保证了框架的极致稳定与长远兼容性。

|          安装启动插件           |          安装启动插件2          |          示例插件页面           |
|:-------------------------:|:-------------------------:|:-------------------------:|
| ![示例图](image/figure1.jpg) | ![示例图](image/figure2.jpg) | ![示例图](image/figure3.jpg) |

|          示例插件页面2          |          去中心化管理           |         崩溃熔断与自愈提示         |
|:-------------------------:|:-------------------------:|:-------------------------:|
| ![示例图](image/figure4.jpg) | ![示例图](image/figure5.jpg) | ![示例图](image/figure6.jpg) |

> 🔗 **下载示例App**: [https://github.com/lnzz123/ComboLite/tree/master/app/release/app-release.apk](https://raw.githubusercontent.com/lnzz123/ComboLite/refs/heads/master/app/release/app-release.apk)

---

### ✨ 核心特性

#### 🎨 为 Compose 而生，拥抱现代技术栈

`ComboLite` 原生为新一代 Android UI 工具包 Jetpack Compose 设计，插件内可无缝使用 `@Composable`
函数构建界面。同时，框架完美集成 Kotlin Coroutines、StateFlow 等现代技术，并采用 Koin
进行依赖注入，让您在插件开发中也能享受最前沿、最高效的技术栈。

#### 🛡️ 极致稳定：0 Hook & 0 反射

这是 `ComboLite` 最核心的承诺。我们完全基于 Android 官方推荐的 `ClassLoader` 和代理（Proxy）模式，不使用任何非公开
API。这意味着框架拥有无与伦比的稳定性，能天然兼容从 Android 7.0 到未来所有 Android
版本，让您彻底告别因系统升级导致的兼容性噩梦。

#### 🚑 崩溃熔断与自愈机制

内置强大的 `PluginCrashHandler`，当插件因缺少依赖等问题导致崩溃时，框架会自动**禁用**
有问题的插件，防止应用陷入无限重启的循环，并引导用户到友好的错误提示页面。这个机制将一个潜在的、导致应用瘫痪的致命错误，转化为一个可隔离、可恢复的局部问题，最大限度地保障了宿主应用的稳定性。

#### 🔗 智能依赖解析与链式重启

框架拥有强大的动态依赖解析能力。插件间的依赖关系无需预先配置，会在类加载时被自动发现并动态构建成依赖图。当您需要热更新一个被其他插件依赖的核心插件时，
`ComboLite` 提供的**链式重启机制**会自动卸载并重载所有受影响的上游插件，完美修复因热更新导致的类加载器冲突问题，确保依赖链的绝对一致性。

#### 🌐 创新的去中心化架构

我们打破了传统“宿主-插件”的强中心化模式。在 `ComboLite` 中，**任何插件都拥有管理（下载、安装、更新、卸载）自身或其他插件的能力
**。这赋予了开发者前所未有的灵活性，可以轻松实现“插件商店”、“按需下载”、“插件自我更新”等高级功能，构建真正动态化的应用生态。

#### 📦 “空壳”宿主支持

得益于去中心化架构，您的宿主 App 可以做到真正的“空壳化”——即没有任何业务逻辑，完全退化为一个启动入口和插件管理容器。
**所有功能、所有 UI 均可由插件动态提供**。这种模式为应用的模块化、动态化和团队协作开发提供了极致的解耦方案。

#### ♻️ 透明化的合并式资源管理

`ComboLite` 采用合并式资源管理。插件被加载时，其所有资源（layouts, drawables, strings 等）会被动态地合并到宿主的全局
`Resources` 对象中。这意味着您**无需关心资源来自哪个插件，可以像访问宿主自身资源一样，透明地访问所有已加载插件的资源
**，极大地简化了插件的资源使用方式。

#### 🗃️ Service 多实例与服务池

`ComboLite` 创新地支持**服务实例池**。您可以通过一个唯一的 `instanceId`，将同一个插件 `Service`
类启动为多个相互隔离、独立运行的实例。这对于需要同时处理多个独立任务（如下载管理、多路视频流、并行计算等）的场景非常有用，是很多其他插件化框架所不具备的高级功能。

#### ⚡️ 闪电般的类查找性能

传统插件框架普遍存在跨插件类查找的性能瓶颈。`ComboLite` 通过在加载时为所有插件建立全局类索引，实现了 *
*`O(1)` 时间复杂度**的跨插件类查找。无论您的应用规模多庞大、插件多复杂，类查找都能瞬间完成，保证了应用的流畅运行。

---

### 🏗️ 架构概览

`ComboLite` 采用简洁而强大的微核设计，由几个核心组件协同工作，逻辑清晰，易于扩展。

```mermaid
graph TD
    subgraph "宿主应用 & 系统"
        HostApp[宿主应用代码] -- 调用 API --> PM(插件管理器)
        AndroidSystem[Android 系统] -- 与...交互 --> HostProxies["宿主代理组件<br>(HostActivity, HostService...)"]
    end

    subgraph "ComboLite 核心管理器"
        PM -- 协调 --> IM(安装器)
        PM -- 协调 --> RM(资源管理器)
        PM -- 协调 --> ProxyM(调度器)
        PM -- 协调 --> DM(依赖管理器)
    end
    
    subgraph "数据 & 状态"
        OnDiskState["磁盘状态<br>plugins.xml, APKs"]
        InMemoryState["内存状态<br>已加载插件, 类加载器, 实例"]
        ClassIndex["全局类索引<br>Map<类, 插件ID>"]
        DepGraph["依赖图<br>(正向 & 反向)"]
        MergedRes["合并后的资源"]
    end
    
    %% --- 管理器职责 ---
    IM -- "管理" --> OnDiskState
    PM -- "管理" --> InMemoryState
    PM -- "构建 & 持有" --> ClassIndex
    DM -- "构建 & 持有" --> DepGraph
    RM -- "创建 & 持有" --> MergedRes
    ProxyM -- "管理" --> HostProxies
    
    %% --- 关键交互 ---
    subgraph "关键交互: 类加载器委托"
        direction LR
        style RequesterPCL fill:#f9f,stroke:#333,stroke-width:2px
        style TargetPCL fill:#ccf,stroke:#333,stroke-width:2px
        
        RequesterPCL["请求方<br>插件类加载器"] -- "findClass()查找失败时" --> DM
        DM -- "1. 查找" --> ClassIndex
        DM -- "2. 记录依赖" --> DepGraph
        DM -- "3. 从...加载" --> TargetPCL["目标<br>插件类加载器"]
    end
    
    InMemoryState -- 包含 --> RequesterPCL
    InMemoryState -- 包含 --> TargetPCL
````

* **`PluginManager`**: 框架的中心协调器，负责插件的加载、卸载、重启和生命周期管理。
* **`InstallerManager`**: 负责插件的安装、更新和合法性校验。
* **`ResourceManager`**: 负责插件资源的加载与管理，实现宿主与插件资源的无缝合并。
* **`ProxyManager`**: 负责 Android 四大组件的代理和生命周期分发。
* **`DependencyManager`**: 负责维护插件间的动态依赖关系图和全局类索引。

-----


### 🚀 开始集成 (Getting Started)

`ComboLite` 现已正式发布至 Maven Central 及 Gradle 插件门户。现在，您可以像集成任何标准库一样，通过远程依赖将
`ComboLite` 轻松地集成到您的项目中。

#### 第 1 步: 在 `libs.versions.toml` 中定义依赖项

我们强烈建议使用 Version Catalog (`libs.versions.toml`) 来集中管理您项目的所有依赖。这种现代化的方式能让您的依赖管理更加清晰和可维护。

在您的 `gradle/libs.versions.toml` 文件中，添加以下版本、库和插件定义：

```toml
# in gradle/libs.versions.toml

[versions]
# ... 其他版本定义
combolite = "1.0.0"  # 建议替换为最新的稳定版
aar2apk = "1.0.0" # 建议替换为最新的稳定版

[libraries]
# ... 其他库定义
combolite-core = { group = "io.github.lnzz123", name = "combolite-core", version.ref = "combolite" }

[plugins]
# ... 其他插件定义
combolite-aar2apk = { id = "io.github.lnzz123.combolite-aar2apk", version.ref = "aar2apk" }

```

#### 第 2 步: 配置 Gradle 构建脚本

现在，在您的 Gradle 脚本中应用这些依赖。

**① 在项目根 `build.gradle.kts` 中应用打包插件**:

此插件仅需在项目根目录应用一次，它将负责所有已声明插件模块的打包任务。

```kotlin
// in your project's root /build.gradle.kts
plugins {
    // ... 其他插件
    alias(libs.plugins.combolite.aar2apk)
}

// 您可以在此配置 aar2apk 的打包策略，详情请参阅 [插件打包指南]
aar2apk {
    // signing { ... }
    // modules { module(":your-plugin-module") }
}
```

**② 在宿主 App 模块的 `build.gradle.kts` 中添加核心库**:

```kotlin
// in your :app/build.gradle.kts
plugins {
    // ...
}

dependencies {
    // ... 其他依赖
    implementation(libs.combolite.core)
}
```

**③ 在您的插件模块 (Library) 的 `build.gradle.kts` 中添加核心库**:

插件模块应使用 `compileOnly` 依赖核心库，表示该库在运行时由宿主提供。

```kotlin
// in your :your-plugin-module/build.gradle.kts
plugins {
    // ...
}

dependencies {
    // ... 其他依赖
    compileOnly(libs.combolite.core)
}
```

**恭喜您，集成完毕！** 您的项目现在已经具备了 `ComboLite` 插件化框架的全部能力。

-----

### 📚 接下来做什么？

环境已经搭建完毕，现在我们强烈建议您阅读以下文档，开始您的插件化开发之旅：

* [**[必读] 快速开始**](./docs/1_QUICK_START.md): 从零到一，构建并运行你的第一个插件。
* [**[核心] 插件打包指南**](./docs/2_PACKAGING_GUIDE.md): 深入了解 `aar2apk` 插件，精通两种打包策略。
* [**[进阶] 核心 API 用法**](./docs/3_CORE_APIS.md): 掌握 `PluginManager` 的所有核心功能。
* [**[进阶] 四大组件指南**](./docs/4_COMPONENTS_GUIDE.md): 学习如何在插件中使用 Activity, Service,
  BroadcastReceiver, ContentProvider。
* [**[原理] 架构与设计**](./docs/5_ARCHITECTURE.md): 探索 ComboLite 的内部工作机制。

-----

### 🆚 框架对比与技术选型

`ComboLite` 在设计之初，充分借鉴了前辈们的经验，并针对现代 Android 开发的痛点进行了革新。

| 对比维度                   | `ComboLite` (本项目)                            | `Shadow` (腾讯)                     | `RePlugin` (360)                     | 经典 Hook 方案 (VirtualAPK 等)    | Google Play Feature Delivery   |
|:-----------------------|:---------------------------------------------|:----------------------------------|:-------------------------------------|:-----------------------------|:-------------------------------|
| **核心原理**               | ✅ **官方公开 API + 代理模式**                        | 编译期代码重写 + 运行时委托                   | ClassLoader Hook + 部分系统Hook          | ❌ **重度 Hook 系统服务** (AMS/PMS) | ✅ **系统级原生支持**                  |
| **系统兼容性**              | 🥇 **极高**，无非公开 API 调用，天然适配 Android 7.0 - 16+ | 🥈 **较高**，绕开了大部分系统限制              | 🥉 **中等**，对 ClassLoader 的修改在新系统上存在风险 | 💥 **低**，对系统版本敏感，新系统上易失效     | 🥇 **极高**，官方方案                 |
| **Jetpack Compose 支持** | ✅ **原生支持**，核心设计目标                            | ❌ **不支持**                         | ❌ **不支持**                            | ❌ **不支持**                    | ✅ **原生支持**                     |
| **接入与使用成本**            | ✨ **极低**，微量核心代码，对项目几乎无侵入                     | ⚠️ **高**，依赖深度定制的 Gradle 插件，构建流程复杂 | ⚠️ **较高**，需理解其复杂的组件生命周期管理            | ⚠️ **较高**，需继承特定基类，配置繁琐       | ✨ **极低**，官方工具链原生支持             |
| **社区活跃度**              | 🚀 **活跃开发中**                                 | ⚠️ **维护放缓** (约2022年后)             | ❌ **基本停滞** (约2020年后)                 | ❌ **已停滞**                    | 🚀 **Google 官方持续迭代**           |
| **主要优势**               | **极致稳定、现代技术栈、开发体验好、去中心化架构**                  | 设计思想精巧，Activity 兼容性好              | 功能全面，曾有大规模验证                         | 特定版本下功能强大完备                  | 稳定可靠，Google Play 生态集成          |
| **主要权衡**               | 代理模式对部分冷门 `launchMode` 支持受限                  | 学习曲线陡峭，构建系统黑盒，已不兼容新版AGP           | 侵入性较强，兼容性问题随系统升级增多                   | **稳定性差，已不适用于现代开发**           | **非热更新，必须通过应用商店发布，无法加载本地 APK** |

---

* **对比 Hook 方案 (如 VirtualAPK / DroidPlugin)**

    * **它们**: 通过 Hook 系统核心服务 (AMS/PMS) 来“欺骗”系统，功能强大但极其不稳定，随着系统版本迭代已基本失效，且均已停止维护。
    * **我们**: **绝不使用 Hook**。`ComboLite` 采用官方推荐的代理模式，虽然在某些极端 Activity
      启动模式上自由度稍逊，但换来的是坚如磐石的稳定性，这是我们最重要的承诺。

* **对比编译期方案 (如 Shadow)**

    * **它们**: 设计精巧，通过编译期重写代码绕开 Hook，稳定性更高。但其构建系统复杂，学习曲线陡峭，且项目已放缓维护，对新技术栈（如
      Compose）支持滞后。
    * **我们**: **拥抱简单与现代**。`ComboLite` 保持核心逻辑的清晰透明，与最新的 AGP/Gradle/Compose
      工具链完美同步，让开发者能将精力聚焦于业务本身，而非复杂的框架底层。

* **对比 RePlugin (360)**
    * **它们**: 同样是业界经典，通过 Hook ClassLoader 实现功能，但在新版 Android 对非公开 API
      限制愈发严格的今天，其稳定性面临挑战。项目也已基本停止维护，对 Compose 等新技术栈缺乏支持。
    * **我们**: **选择面向未来的稳定路线**。`ComboLite` 彻底规避了 Hook 带来的兼容性风险，并原生为
      Jetpack Compose 设计，确保在现代技术栈下获得最佳的开发体验和长期维护性。

* **对比 Google Play Feature Delivery**
    * **它们**: 是一个**应用分发方案**，旨在减少初始安装包体积，所有模块更新仍需通过应用商店审核和下发，本质上是“冷分发”，无法实现真正的热更新。
    * **我们**: **是一个纯粹的热更新框架**。`ComboLite` 赋予 App 在运行时加载任意来源 APK
      的能力，可以完全绕开应用商店进行功能迭代和 Bug 修复，这才是动态化的核心价值。

插件化技术没有绝对完美方案，选择即是取舍。`ComboLite` 的设计哲学是在保证 **99%**
主流场景的极致稳定与简洁性的前提下，审慎对待那 **1%** 的边缘场景。

> **总而言之，如果您正在开发一个面向未来的、使用 Jetpack
Compose、且将长期稳定性和可维护性放在首位的项目，`ComboLite` 将是您的不二之选。**

-----

### 🤝 如何贡献

我们热切欢迎任何形式的贡献！无论是提交功能建议、报告 Bug、还是发起 Pull Request，都是对社区的巨大帮助。

* **报告 Bug 或提交建议**: 请通过 [GitHub Issues](https://github.com/lnzz123/ComboLite/issues) 提交。
* **贡献代码**: Fork 本仓库，完成修改后向上游发起 Pull Request。

-----

### ❤️ 支持与赞助 (Support & Sponsor)

`ComboLite` 是一个免费的开源项目，由我利用业余时间进行开发和维护。如果这个项目对您有帮助，您的支持将是我持续投入的莫大动力。

<details>
  <summary>点击查看赞助方式 (Click to see sponsorship methods)</summary>

  <br>

  <p>您的每一份支持都意义非凡。可以通过以下方式请我喝杯咖啡 ☕️：</p>

  <table>
    <tr>
      <td align="center">支付宝 (Alipay)</td>
      <td align="center">微信支付 (WeChat Pay)</td>
    </tr>
    <tr>
      <td align="center"><img src="image/alipay.jpg" alt="Alipay" width="200"></td>
      <td align="center"><img src="image/wechatpay.jpg" alt="WeChat Pay" width="200"></td>
    </tr>
  </table>
</details>

-----

### 许可 (License)

`ComboLite` 遵循 [Apache-2.0 license](https://github.com/lnzz123/ComboLite/blob/main/LICENSE)
开源许可协议。

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=lnzz123/ComboLite&type=Date)](https://www.star-history.com/#lnzz123/ComboLite&Date)
