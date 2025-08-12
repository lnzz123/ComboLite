# [你的框架名：ArcLite]

<p align="center">
  <img src="URL_TO_YOUR_LOGO" alt="Mycelium Logo" width="150"/>
</p>

<p align="center">
    <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg"/></a>
    <a href="https://jitpack.io/#YourUsername/YourRepo"><img src="https://jitpack.io/v/YourUsername/YourRepo.svg"/></a>
    <a href="#"><img src="https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat"/></a>
    <a href="[指向你CI/CD的链接]"><img src="https://github.com/YourUsername/YourRepo/workflows/Build/badge.svg"/></a>
</p>

<h3 align="center">ArcLite: A Lightweight Plugin Framework for Android Compose</h3>

---

`[你的框架名]` 是一个革命性的安卓插件化解决方案。它拥抱原生 API，彻底告别 Hook 与反射，提供了前所未有的稳定性和兼容性。其独特的去中心化设计，赋予了每个插件管理其他插件的能力，让你的应用真正实现“万物皆可插，万物皆可管”。

### ✨ 核心亮点 (Core Features)

* 🚀 **现代化设计**: 完全为 Jetpack Compose 而生，并完美兼容安卓四大组件的插件化。
* 🛡️ **稳定可靠 (0-Hook, 0-Reflection)**: 完全基于 Android 官方推荐的 `ClassLoader` 和 `Resources` API，从根源上保证了框架的稳定性和对未来安卓版本的超强兼容性（已支持 Android 7 ~ 16）。
* 🌐 **去中心化架构**: 颠覆传统！任何插件都可以成为管理者，动态地下载、安装、更新或卸载其他插件，实现真正的“自组织”应用。
* 📦 **万物皆插件**: 支持将标准的 Android Library (AAR) 工程一键转换为插件，极大地降低了插件开发门槛。
* 🏗️ **空壳宿主支持**: 宿主 App 可以是一个没有任何业务逻辑的空壳，所有功能和UI均由插件提供，非常适合需要高度定制化和动态化的场景。
* 💉 **内置依赖注入**: 默认支持 Koin 进行依赖注入，让你的插件代码也能保持优雅和解耦。
* ⚡ **轻量且无侵入**: 核心模块仅十几个类，依赖极少。集成到现有项目非常简单，只需几行代码配置。

### 🤔 为什么选择 [你的框架名]?

相较于传统的插件化框架（如 RePlugin, VirtualAPK 等），我们提供了：

| 特性 | [你的框架名] | 传统框架 |
| :--- | :---: | :---: |
| **实现原理** | ✅ **原生 API** | ❌ 大量 Hook |
| **稳定性** | ✅ **极高** | ⚠️ 一般，随系统版本波动 |
| **Compose 支持**| ✅ **一等公民** | ❌ 支持不佳或需魔改 |
| **架构** | ✅ **去中心化** | ❌ 中心化（宿主管理一切）|
| **兼容性** | ✅ **Android 7-16+** | ⚠️ 适配新系统困难 |
| **侵入性** | ✅ **极低** | ⚠️ 较高 |

### 快速上手 (Quick Start)
一个基于安卓Compose的插件化框架，支持依赖注入，当然也支持安卓四大组件插件化，我的插件化框架采用了一个Application或一个Library库就是一个单独的插件的设计理念（支持将aar转换为插件apk）以及去中心化的设计，即插件本身也能管理自己或者其他插件的下载安装卸载更新等等，支持宿主app是一个没有任何逻辑的空壳，完全基于安卓官方推荐的classloader和resource loader进行插件的代码与资源加载，0hook0反射，理论上支持所有安卓版本（7-16），插件框架非常轻量化，除了必要的koin（适配不能使用hilt依赖注入）和dexlib2（用于解析dex文件建立类索引）等库以外几乎没有任何第三方库引用，插件核心模块仅十几个核心类，对原项目侵入性也很小，几乎不需要改动太多代码就能轻松集成