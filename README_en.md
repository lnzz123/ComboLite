<p align="right">
  <a href="./README.md">简体中文</a> | <b>English</b>
</p>

# ComboLite: The Next-Generation Plugin Framework for Android

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

🚀 A next-generation Android plugin framework built for Jetpack Compose. A modern, stable, and
flexible plugin solution. **Core Features: Native Compose Support | 100% Official APIs | 0 Hooks & 0
Reflection | Decentralized Architecture**

-----

As the Android ecosystem evolves, many classic plugin frameworks from the View era are struggling to
keep up with modern development demands. Most of these projects are no longer maintained, and their
massive, obscure internal implementations, over-reliance on **non-public APIs (Hooks and reflection)
**, and high integration costs make them increasingly incompatible with frequent system updates.

**`ComboLite` was created to end this dilemma, providing Android developers with a modern, stable,
and flexible plugin framework.**

ComboLite is born for modern Android development. It completely abandons risky non-public API calls
from the ground up, building a pure architecture with **0 Hooks and 0 reflection** based entirely on
public APIs. Its logic is clear and integration is lightweight. It is natively designed for Jetpack
Compose and pioneers a decentralized management philosophy, granting developers unprecedented
flexibility while ensuring ultimate stability.

Whether you want to build a "shell" application where all features are pluggable or add dynamic
capabilities to an existing project, `ComboLite` is your most reliable and modern choice.

## 📸 Screenshots

Download the sample
app: [https://github.com/lnzz123/ComboLite/releases](https://github.com/lnzz123/ComboLite/releases)

|          安装启动插件           |          安装启动插件2          |          示例插件页面           |
|:-------------------------:|:-------------------------:|:-------------------------:|
| ![示例图](image/figure1.jpg) | ![示例图](image/figure2.jpg) | ![示例图](image/figure3.jpg) |

|          示例插件页面2          |          去中心化管理           |         崩溃熔断与自愈提示         |
|:-------------------------:|:-------------------------:|:-------------------------:|
| ![示例图](image/figure4.jpg) | ![示例图](image/figure5.jpg) | ![示例图](image/figure6.jpg) |

## ✨ Core Concepts and Advantages

The design philosophy of `ComboLite` is rooted in four core principles:

#### 1\. Modern by Design

* **Natively for Compose**: `ComboLite` is designed to meet the needs of the next-generation Android
  UI toolkit, Jetpack Compose. Plugins can seamlessly use `@Composable` functions to build
  interfaces, enjoying the development convenience of declarative UI.
* **Embracing the Mainstream Tech Stack**: Perfectly integrates with Kotlin Coroutines and
  StateFlow, and uses Koin for dependency injection, allowing you to use the most advanced and
  efficient technologies in plugin development.
* **Latest Toolchain**: Built on the latest versions of Android Studio, Gradle, and AGP, eliminating
  the integration nightmares caused by outdated frameworks' toolchain incompatibility.

#### 2\. Ultimate Stability & Compatibility

* **0 Hooks, 0 Reflection**: This is ComboLite's core promise. We are entirely based on the official
  Android `ClassLoader` mechanism and use no Hooks. For resource loading, we prioritize the latest
  official APIs and only use the industry-standard, stable reflection solution for compatibility
  with older systems below Android 11. This means the core framework has unparalleled stability and
  natural compatibility with future Android versions.

* **Broad System Support**: Theoretically supports all versions from Android 7.0 (API 24) to Android
  16+, freeing you from concerns about system fragmentation.

* **Intelligent Dependency Resolution & Repair**: The framework has powerful **dynamic dependency
  resolution capabilities**. Dependencies between plugins do not need to be pre-configured; they are
  automatically discovered at class-loading time and dynamically built into a dependency graph. When
  you need to update or restart a core plugin, `ComboLite`'s **chained restart mechanism**
  automatically unloads and reloads all affected upstream plugins, perfectly resolving class loader
  conflicts caused by hot updates and ensuring absolute consistency of the dependency chain.

* **Crash Circuit-Breaking & Self-Healing**: `ComboLite` has a built-in, powerful
  `PluginCrashHandler`. When a plugin causes a `ClassNotFoundException` due to a missing dependency,
  the framework catches this specific `PluginDependencyException` and performs a series of
  self-healing actions:

    * **Precise Localization**: Accurately identifies which plugin caused the crash.
    * **Automatic Circuit-Breaking**: Automatically **disables** the problematic plugin to prevent
      the app from falling into an infinite crash loop on the next launch.
    * **Graceful Degradation**: Guides the user to a friendly error page instead of crashing
      directly, significantly improving the user experience.

  This mechanism transforms a potentially fatal error that could paralyze the entire application
  into an isolatable and recoverable local issue, thus maximizing the stability of the host
  application.

#### 3\. Ultimate Flexibility & Decoupling

* **Decentralized Architecture**: Breaks the traditional "Host-Plugin" strong centralization model.
  Any plugin has the ability to manage (download, install, update, uninstall) itself or other
  plugins, making it easy to implement advanced features like a "Plugin Store" or "On-Demand
  Download."
* **"Shell" Host Support**: The host app can have no business logic and be completely reduced to a
  launch entry point. All features and UI can be provided dynamically by plugins.
* **Flexible Plugin Form**: A standard Android `Application` or `Library` (AAR) project can be
  easily packaged into a standalone plugin, greatly lowering the barrier to plugin development and
  migration.

#### 4\. Excellent Developer Experience

* **Lightweight Core**: The framework's core module contains only a dozen core classes. Besides Koin
  and dexlib2 (for class indexing), it has almost no other third-party dependencies, having a
  minimal impact on the application size.
* **Minimally Intrusive**: Integrating `ComboLite` requires almost no changes to your project's
  existing structure. Your plugin code can be written as naturally as developing a regular app or
  module.
* **Lightning-Fast Class Lookup**: By creating a global class index for all plugins upon loading,
  `ComboLite` achieves `O(1)` time complexity for cross-plugin class lookups, completely eliminating
  the class lookup performance bottlenecks common in traditional plugin frameworks.

## 🏗️ Architecture Overview

`ComboLite` uses a simple and powerful micro-kernel design, with several core components working in
concert.

```mermaid
graph TD
    subgraph Host App
        H[MyApplication] -- initializes --> PM
    end

    subgraph ComboLite Core
        PM(PluginManager)
        IM[InstallerManager]
        RM[ResourceManager]
        ProxyM[ProxyManager]
        DM[DependencyManager]
    end

    subgraph Loaded Plugins
        P1[Plugin A]
        P2[Plugin B]
    end

    PM -- delegates to --> IM & RM & ProxyM & DM
    H -- uses --> PM

    P1 -- ClassLoader --> DM
    P2 -- ClassLoader --> DM
    P1 -- depends on --> P2

    PM -- Manages Lifecycle --> P1 & P2
```

- **`PluginManager`**: The central coordinator of the framework (singleton), responsible for
  loading, unloading, restarting, and managing the lifecycle of plugins.
- **`InstallerManager`**: Responsible for the installation, updating, and validation of plugins.
- **`ResourceManager`**: Responsible for loading and managing plugin resources, compatible with both
  new and old Android versions.
- **`ProxyManager`**: Responsible for proxying the four major Android components and dispatching
  their lifecycles.
- **`DependencyManager`**: Responsible for maintaining the dynamic dependency graph and class index
  between plugins.

## 🚀 Quick Start

Integrating `ComboLite` is a simple three-step process.

### 1\. Add Dependency

In your host (or shell) application's `build.gradle.kts`, add the core library dependency:

```kotlin
dependencies {
    implementation(projects.comboLiteCore)
}
```

### 2\. Initialize the Framework

`ComboLite` provides a base class to help you complete a one-step initialization, which is our *
*recommended** approach.

#### Method 1: Inherit from Base Class (Recommended)

Simply have your `Application` class inherit from `com.combo.core.base.BaseHostApplication` to
automatically complete all initialization work, including the plugin loader, resource manager, and
crash handler.

**That's all the initialization code you need\!**

```kotlin
// Just inherit from BaseHostApplication
class MainApplication : BaseHostApplication() {
    override fun onCreate() {
        super.onCreate()
        // Your other application-level initialization logic
    }
}
```

#### Method 2: Manual Initialization (For special scenarios)

If your `Application` cannot inherit from `BaseHostApplication`, you can also initialize it
manually. Please ensure all steps are configured correctly to avoid potential issues.

```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. (Important) Register the plugin crash handler
        PluginCrashHandler.initialize(this)

        // 2. Initialize the PluginManager
        PluginManager.initialize(this)

        // 3. Asynchronously load enabled plugins
        lifecycleScope.launch {
            val loadedCount = PluginManager.loadEnabledPlugins()
            Log.d("MyApp", "Successfully loaded $loadedCount plugins.")
        }
    }
}
```

### 3\. Create Your First Plugin

A plugin can be an independent `com.android.library` or `com.android.application` module.

#### a. Implement the Plugin Entry

Create a class and implement the `IPluginEntryClass` interface.

```kotlin
class HomePluginEntry : IPluginEntryClass {

    // (Optional) Define Koin dependency injection modules for this plugin
    override val pluginModule: List<Module>
        get() = listOf(
            module {
                viewModel { HomeViewModel() }
                single<IUserService> { UserServiceImpl() }
            }
        )

    // Define the plugin's main UI
    @Composable
    override fun Content() {
        // Your Jetpack Compose screen
        HomeScreen()
    }
}
```

#### b. Declare the Plugin in `AndroidManifest.xml`

In the plugin module's `AndroidManifest.xml`, declare the plugin information using `<meta-data>`
tags.

```xml

<manifest>
    <application>
        <meta-data android:name="plugin.id" android:value="com.example.home" />
        <meta-data android:name="plugin.version" android:value="1.0.0" />
        <meta-data android:name="plugin.entryClass"
            android:value="com.example.home.HomePluginEntry" />
        <meta-data android:name="plugin.description" android:value="Home screen plugin" />

    </application>
</manifest>
```

You're done\! Now you just need to package the plugin into an APK, and you can install and launch it
via the `PluginManager`.

## 📦 Packaging Guide

The `ComboLite` framework is designed with maximum flexibility, supporting the packaging of two different types of Android modules into independently installable and loadable plugins: **Application modules** and **Library modules**. Each approach has its own technical principles and best use cases. Understanding their differences is crucial for building an efficient and maintainable plugin-based application.

Our project includes a powerful **`aar2apk` Gradle plugin** designed specifically to **one-click package** `Library` modules into lightweight plugin APKs. This is our **preferred and recommended** method.

-----

### 1\. Library Modules: One-Click Packaging with the `aar2apk` Plugin (Recommended)

Packaging a `com.android.library` module as a plugin is an advanced and lightweight approach. This type of plugin does not contain any third-party dependencies itself; it relies on the shared dependency environment provided by the host app.

#### Packaging Principle

This process is fully automated by the project's built-in `aar2apk` plugin (`com.combo.aar2apk`). Its core principles are as follows:

1.  **Automatic Task Dependency**: The plugin automatically hooks into Gradle's `assemble` task. When you trigger the packaging task, it first ensures that the module's `AAR` file is correctly built.
2.  **Deconstructing the AAR**: The task automatically unpacks the `AAR` file to access its contents, such as `classes.jar`, `res` resources, `AndroidManifest.xml`, etc.
3.  **Resource Compilation & Isolation**: Using `aapt2` from the Android build toolchain, the plugin compiles the resources of the plugin and all its dependencies. It also **automatically assigns a unique `packageId`** (e.g., `0x80`, `0x81`, ...) to each plugin based on the configuration, resolving resource ID conflicts at their source.
4.  **Code Dexing**: Using the `d8` compiler, the task converts the `classes.jar` from the AAR and the generated `R.java` file into `classes.dex`, which is executable by the Android virtual machine.
5.  **Reassembling into an APK**: Finally, it repackages the processed `AndroidManifest.xml`, resources, `classes.dex`, and other assets (like `assets`, `jniLibs`), and then signs the package using the configured signing key to produce a valid, loadable plugin APK.

#### Configuration Steps

The entire configuration process is extremely simple and only requires a few changes in the project's root directory.

**a. (Step 1) Include and Apply the Plugin**
First, ensure that your root `settings.gradle.kts` includes the build logic module.

```kotlin
// in /settings.gradle.kts
includeBuild("build-logic")
```

Next, apply the `aar2apk` plugin in your root `build.gradle.kts`.

```kotlin
// in /build.gradle.kts
plugins {
    // ... other plugins
    alias(libs.plugins.aar2apk)
}
```

**b. (Step 2) Configure the Packaging Task**
In the root `build.gradle.kts` file, use the `aar2apk` configuration block to specify the modules to be packaged and the signing information.

```kotlin
// in /build.gradle.kts
aar2apk {
    // 1. Configure all Library module paths to be packaged as plugins
    modules.set(listOf(
        ":sample-plugin:common",
        ":sample-plugin:home",
        ":sample-plugin:guide",
        ":sample-plugin:example",
        ":sample-plugin:setting"
    ))

    // 2. Configure the universal signing information
    signing {
        keystorePath.set(rootProject.file("jctech.jks").absolutePath)
        keystorePassword.set("he1755858138")
        keyAlias.set("jctech")
        keyPassword.set("he1755858138")
    }
}
```

**c. (Step 3) Declare Dependency Scopes in Plugin Modules**
In the `build.gradle.kts` of **all** Library modules intended as plugins, **all third-party dependencies must use the `compileOnly` scope**. This ensures they are used only for compilation and are not packaged into the final artifact, keeping the plugin lightweight.

```kotlin
// in your-library-plugin/build.gradle.kts
dependencies {
    // Framework core library
    compileOnly(projects.comboLiteCore)
    
    // All third-party dependencies must be compileOnly
    compileOnly("io.coil-kt:coil-compose:2.5.0")
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")

    // Only dependencies on other modules within the project can use implementation
    // implementation(project(":common-utils")) 
}
```

**d. (Step 4) One-Click Packaging**
Once configured, simply run the custom Gradle tasks automatically generated by the `aar2apk` plugin.

```bash
# One-click packaging for all configured Library plugins (Release version)
./gradlew buildAllReleasePluginApks

# One-click packaging for all configured Library plugins (Debug version)
./gradlew buildAllDebugPluginApks

# Clean all plugin build artifacts (APKs, logs, temp files)
./gradlew cleanAllPluginApks

# You can also find and run these tasks in your IDE's Gradle panel.
# They are clearly organized under the "Plugin APKs" group.
```

The packaged artifacts will be located in the project's root `build/outputs/plugin-apks/` directory, already sorted into `debug` and `release` folders.

#### Analysis of Pros and Cons

* ✅ **Advantages**:
  * **Extremely Lightweight**: APK sizes are typically only a few dozen to a few hundred KBs, making updates and downloads very cheap.
  * **Eliminates Dependency Conflicts**: All plugins share the same set of dependencies provided by the host, fundamentally avoiding version conflict issues.
  * **Centralized Dependency Management**: Dependency versions are managed and upgraded centrally by the host, reducing maintenance costs.
  * **Improved Build Speed**: Shared dependencies mean less redundant compilation.
* ⚠️ **Trade-offs**:
  * **Dependent on the Host Environment**: The plugin's execution is highly dependent on the host. If the host fails to provide a dependency required at runtime, the plugin will break the dependency chain due to a `ClassNotFoundException`, and the framework will automatically circuit-break and disable the faulty plugin.
  * **Restricted Dependency Versions**: The plugin must use the dependency versions provided by the host and cannot freely introduce specific versions of libraries internally.

-----

### 2\. Application Modules: Self-Contained Plugins (Alternative)

Packaging a standard `com.android.application` module as a plugin is a more traditional method that remains useful in certain scenarios. This type of plugin is a fully-featured, self-contained micro-application.

#### Packaging Principle

The principle is identical to building a regular Android application. Gradle's `assemble` task will package all of the module's code, resources, and third-party dependencies (included via `implementation`) into the final APK file.

#### Configuration Steps

**a. Add Core Dependencies**
In the plugin module's `build.gradle.kts`, declare the framework's core library as `compileOnly`.

```kotlin
// in your-plugin/build.gradle.kts
dependencies {
    compileOnly(projects.comboLiteCore)
    implementation("com.google.code.gson:gson:2.9.0")
}
```

**b. (Important) Configure the Package ID**
To avoid resource ID conflicts with the host or other plugins, you **must** manually specify a unique `Package ID` for each `Application` plugin module.

```kotlin
// in your-plugin/build.gradle.kts
android {
    // ...
    aaptOptions {
        additionalParameters("--package-id", "0x80")
    }
}
```

> **Note**: Each `Application` plugin must have a unique `Package ID`. For example, Plugin A uses `0x80`, Plugin B uses `0x81`, and so on.

**c. Run the Build**
Use the standard task provided by the Android Gradle Plugin (AGP) to package the APK.

```bash
./gradlew :your-plugin-module:assembleRelease
```

#### Analysis of Pros and Cons

* ✅ **Advantages**:
  * **Highly Independent**: The plugin is self-contained with all its dependencies, making deployment simple and independent of the host's environment.
  * **No Compatibility Worries**: No need to worry about whether the host provides the libraries or versions required by the plugin.
* ⚠️ **Trade-offs**:
  * **Larger File Size**: The plugin APK's size will be relatively large as it includes all dependencies.
  * **Potential Dependency Redundancy**: If multiple plugins use the same library (e.g., `OkHttp`), it will be duplicated in each plugin, increasing the overall application size.

-----

### 3\. How to Choose?

| Scenario                                                             | Recommended Approach                  | Rationale                                                                                                |
|:---------------------------------------------------------------------|:--------------------------------------|:---------------------------------------------------------------------------------------------------------|
| **UI Component Libraries, Utility Classes, Common Services**         | **Library Module (`aar2apk` plugin)** | **Preferred method**. Single-purpose, small, perfect for frequent updates and reuse.                     |
| **"Super Apps" with Many Plugins**                                   | **Library Module (`aar2apk` plugin)** | Maximizes reuse of common dependencies, significantly reducing total app size and memory.                |
| **Scenarios with Strict Requirements on Update Speed and Size**      | **Library Module (`aar2apk` plugin)** | Extremely small package size makes dynamic delivery and hot-updates nearly seamless.                     |
| **Large, Independent Feature Modules** (e.g., shopping, game center) | **Application Module**                | Complex business logic and numerous dependencies require high cohesion and independence.                 |
| **Plugins for Third-Party Integration**                              | **Application Module**                | The host environment cannot be controlled, so self-containing all dependencies is crucial for stability. |

By choosing the right packaging strategy, you can fully leverage the advantages of the `ComboLite` framework to build a flexible and robust modern Android application.

## 🔧 Core API Usage

`PluginManager` provides a rich set of APIs to manage and interact with plugins. Here are some of
the most core usage examples.

### 1\. Plugin Management: Install, Uninstall & Enable

Plugin lifecycle management is handled through `PluginManager.installerManager` and `PluginManager`
itself.

```kotlin
// --- Install a plugin ---
// Recommended to run on an IO thread
val pluginApkFile = File("path/to/your/plugin.apk")
val installResult = PluginManager.installerManager.installPlugin(pluginApkFile)

if (installResult is InstallResult.Success) {
    val pluginId = installResult.pluginInfo.pluginId
    println("Plugin [$pluginId] installed successfully!")

    // After successful installation, you can choose to launch it immediately
    PluginManager.launchPlugin(pluginId)
} else {
    println("Plugin installation failed: ${(installResult as InstallResult.Failure).message}")
}


// --- Uninstall a plugin ---
val pluginToUninstall = "com.example.home"
val uninstallSuccess = PluginManager.installerManager.uninstallPlugin(pluginToUninstall)
if (uninstallSuccess) {
    println("Plugin [$pluginToUninstall] uninstalled successfully!")
}

// --- Control plugin auto-start ---
// Prevent a plugin from being loaded automatically on the next app launch
PluginManager.setPluginEnabled("pluginId", false)
```

### 2\. Plugin Execution & Interaction

Launching a plugin and getting its entry instance is fundamental to interacting with its
functionality.

```kotlin
// --- Launch or restart a plugin ---
// If the plugin is not running, it will be started. If it is already running, a chained restart will be performed.
val success = PluginManager.launchPlugin("com.example.home")

// --- Get the plugin entry instance ---
// This allows you to directly call methods or access properties defined in the plugin entry class
val homePlugin: IPluginEntryClass? = PluginManager.getPluginInstance("com.example.home")

// For example, if your host Activity needs to display a plugin's Compose UI
// @Composable
// fun ShowPluginUI(pluginId: String) {
//     val plugin = PluginManager.getPluginInstance(pluginId)
//     plugin?.Content() // Call the plugin's @Composable method
// }
```

### 3\. Accessing Plugin Resources

If needed, you can also directly access the `Resources` object of a specific plugin.

```kotlin
// Get the resource manager for the "com.example.home" plugin
val pluginResources: Resources? = PluginManager.resourcesManager.getResources("com.example.home")

// Use the plugin's resource ID to load resources
val icon = pluginResources?.getDrawable(R.drawable.plugin_icon)
val title = pluginResources?.getString(R.string.plugin_title)
```

For more usage examples, please refer to the sample project's plugin modules.

## 🔧 Usage of the Four Major Components

`ComboLite` provides a series of elegant `Context` extension functions, making calls to plugin
functions as smooth as native ones.

### Activity Usage

Define an Activity in your plugin by inheriting from `BasePluginActivity`.

```kotlin
class HomeActivity : BasePluginActivity() { /* ... */ }
```

Start it from anywhere just like a regular Activity.

```kotlin
context.startPluginActivity(HomeActivity::class.java) {
    putExtra("USER_ID", 123)
}
```

### Service Usage

Define a Service in your plugin by inheriting from `BasePluginService`.

```kotlin
class MusicService : BasePluginService() { /* ... */ }
```

The framework manages plugin Services through a **proxy service pool**. You can start, bind, and
stop them transparently.

```kotlin
context.startPluginService(MusicService::class.java)
context.bindPluginService(MusicService::class.java, serviceConnection, Context.BIND_AUTO_CREATE)
context.stopPluginService(MusicService::class.java)
```

### BroadcastReceiver Usage

Define a Receiver in your plugin by implementing the `IPluginReceiver` interface.

```kotlin
class DownloadReceiver : IPluginReceiver { /* ... */ }
```

Send broadcasts via `sendInternalBroadcast`, and the framework will automatically route them to
registered plugin receivers.

```kotlin
context.sendInternalBroadcast("com.example.DOWNLOAD_COMPLETE") {
    putExtra("FILE_PATH", "/path/to/file")
}
```

### ContentProvider Usage

Define a Provider in your plugin by inheriting from `ContentProvider` just like native development.

```kotlin
class MyPluginProvider : ContentProvider() { /* ... */ }
```

Clients need to access the plugin Provider through a **specially formatted URI**.

```kotlin
// The conventional URI format for accessing a plugin Provider is:
// "content://[Host_Provider_Authority]/[Full_ClassName_of_Plugin_Provider]/[Original_Path]"

val pluginProviderClassName = MyPluginProvider::class.java.name
val hostAuthority =
    "com.jctech.plugin.sample.proxy.provider" // Authority of the host proxy Provider
val originalPath = "items/1"

val proxyUri = Uri.parse("content://$hostAuthority/$pluginProviderClassName/$originalPath")

// Use the proxyUri to perform a query
context.contentResolver.query(proxyUri, null, null, null, null)
```

## 🛠️ Implementation Principles of the Four Major Components

* **Activity**: A single transparent `BaseHostActivity` is registered in the host's`AndroidManifest`
  as a proxy "placeholder". When starting a plugin Activity, the framework launches this proxy
  Activity and passes the real plugin Activity's class name via the `Intent`.`BaseHostActivity` then
  instantiates the plugin Activity internally and binds its lifecycle to its own for dispatching.
* **Service**: An innovative **"proxy service pool"** model is used. You pre-register multiple
  `BaseHostService` instances in the host's `AndroidManifest`. When a plugin Service is started,
  `ProxyManager` finds an available proxy Service from the pool to bind with, associating the plugin
  Service's lifecycle with that proxy. This allows multiple plugin Services to run concurrently and
  independently.
* **BroadcastReceiver**: When a plugin is loaded, the framework parses the static receivers
  registered in its `AndroidManifest.xml` and manages them centrally through `ProxyManager`. When a
  system broadcast arrives, it is received by the proxy `BaseHostReceiver` in the host, which then
  asynchronously dispatches it to all matching plugin `IPluginReceiver`s based on the `Action` and
  other information.
* **ContentProvider**: A single proxy `BaseHostProvider` is pre-registered in the host to serve as
  the sole entry point for all plugin `Provider`s. External access is done through the specially
  formatted `Uri` mentioned above. The proxy `Provider` parses the target plugin `Provider`'s class
  name from the first path segment of the `Uri`, instantiates it, and forwards the request along
  with a rewritten `Uri` that matches the plugin's original `Authority`. The proxy layer is also
  responsible for security checks on the plugin `Provider`'s `exported` status.

## 🆚 Comparison with Other Frameworks

Plugin technology has evolved over many years, giving rise to many excellent open-source projects.
`ComboLite` was designed by drawing upon the experience of its predecessors and innovating to
address the pain points of modern Android development.

We have chosen to compare it with several well-known and representative frameworks in the industry
to help you make a better-informed technical decision.

### Detailed Comparison Table

| Dimension                   | `ComboLite` (Ours)                                   | `Shadow` (Tencent)                                              | `VirtualAPK` / `DroidPlugin`                               |
|:----------------------------|:-----------------------------------------------------|:----------------------------------------------------------------|:-----------------------------------------------------------|
| **Implementation**          | ✅ **Official API + Proxy Pattern**                   | 🌓 **Compile-time Code Rewriting + Runtime Delegate**           | 💥 **Hook System Services (AMS/PMS)**                      |
| **Community Activity**      | 🚀 **Actively Developed** (2025)                     | ⚠️ **Maintenance Slowed** (Last active \~2022)                  | ❌ **Stagnant** (Last active \~2015-2018)                   |
| **Dev Env Compatibility**   | ✅ **Native support for latest AGP/Gradle**           | ❌ **Requires old versions or custom forks**                     | ❌ **Severely incompatible, practically unusable**          |
| **Jetpack Compose**         | ✅ **Native Support, Core Feature**                   | ❌ **Not Supported**                                             | ❌ **Not Supported**                                        |
| **Hot Update Support**      | ✅ **Supported** (UI/Code/Resources)                  | ✅ **Supported** (Activity/Code/Resources)                       | ✅ **Supported**                                            |
| **Four Components Support** | ✅ **Full Support** (Proxy Pattern)                   | ✅ **Good Activity support**, others complex                     | ✅ **Full Support** (Hook Pattern)                          |
| **Framework Complexity**    | ✨ **Very Low** (\~12 core classes)                   | ⚠️ **High** (Requires understanding its build plugin & runtime) | ⚠️ **Extremely High** (Involves many system service hooks) |
| **Integration Difficulty**  | ✨ **Very Low** (A few lines of config)               | ⚠️ **High** (Requires custom build process)                     | ⚠️ **High** (Requires changing base classes & Application) |
| **Intrusiveness**           | ✨ **Minimal**                                        | ⚠️ **High** (Heavy reliance on its Gradle plugin)               | ⚠️ **High** (Requires inheriting specific base classes)    |
| **Architecture Model**      | 🌐 **Decentralized** (Plugins can manage each other) | 🏠 **Centralized** (Host manages plugins)                       | 🏠 **Centralized** (Host manages plugins)                  |
| **DI Support**              | ✅ **Native Koin**                                    | ❌ **Requires custom implementation**                            | ❌ **Requires custom implementation**                       |
| **App Compatibility**       | 🥇 **Excellent** (No reliance on non-public APIs)    | 🥈 **Good** (Bypasses most system restrictions)                 | 🥉 **Poor** (Breaks frequently with system updates)        |

### Analysis of Different Solutions

#### About VirtualAPK / DroidPlugin

These frameworks are **outstanding representatives of the Hook-based approach**. In their time, they
achieved a near-perfect ability to "deceive" the system by hooking core services (
`ActivityManagerService`, `PackageManagerService`, etc.), making them very feature-complete.

* **Pros**: Powerful and supports most native app features on specific system versions.
* **Trade-offs**: Their **fatal flaw** is the strong coupling to the Android system's
  implementation. With rapid Android version iterations and deep customizations by manufacturers,
  these hook points based on non-public APIs are easily broken, leading to severe compatibility and
  stability issues. Today, they are **no longer maintained** and cannot keep up with the modern
  Android development toolchain (like AGP 8.x), making them unsuitable for new projects.

#### About Shadow

`Shadow` is a framework with an **extremely ingenious design**. It takes a different path by
rewriting code at compile time and delegating plugin component calls to a separate "runtime"process,
thereby avoiding direct hooks on system services.

* **Pros**: Excellent Activity compatibility and much higher stability than hook-based solutions.
* **Trade-offs**: `Shadow`'s power comes from its **complex build system**. Developers need to
  deeply rely on its custom Gradle plugin and understand its unique "Host-Runtime-Plugin" model,
  which has a steep learning curve. Additionally, the project's **maintenance has slowed**, and it
  lags in support for the latest Android features and toolchains.

#### About ComboLite (Ours)

`ComboLite` chooses the path of **returning to official standards and embracing simplicity**. We
believe that for the vast majority of business scenarios, stability and maintainability are far more
important than achieving certain edge functionalities through "black magic".

* **Pros**:
    * **Ultimate Stability**: Based entirely on the official public Proxy pattern and `ClassLoader`
      mechanism, without touching any non-public APIs. It naturally has cross-version and
      cross-manufacturer compatibility.
    * **Embracing Modernity**: Natively designed for Jetpack Compose, actively maintained, and
      always keeping up with the latest development toolchains.
    * **Simple and Transparent**: The core framework has very little code, clear logic, and is very
      easy to integrate and extend.
    * **Flexible Architecture**: The unique decentralized design provides more possibilities for
      application architecture.
* **Trade-offs** (Revised Description):
  While the proxy pattern is extremely stable, compared to hook-based solutions that can deeply
  modify system behavior, its freedom is relatively limited in handling complex *
  *Activity `launchMode` and task affinity management**. This is because the proxy Activity's own
  `launchMode` and task behavior are declared in the host's manifest, and plugins cannot completely
  change them dynamically at runtime. `ComboLite`'s choice is to provide the purest and simplest
  solution for 99% of mainstream scenarios, rather than sacrificing the entire framework's
  reliability for 1% of edge cases.

**In summary, if you are developing a future-oriented project that uses a modern tech stack like
Jetpack Compose and prioritizes long-term stability and maintainability, `ComboLite` is your best
choice.**

## How to Contribute

We eagerly welcome contributions of any kind to make `ComboLite` better\! Whether it's submitting
feature suggestions, reporting bugs, creating pull requests, or helping to improve the
documentation, every contribution is a huge help to the community.

* **Report Bugs or Suggest Features**: Please submit them
  through [GitHub Issues](https://github.com/lnzz123/ComboLite/issues). This is the best channel for
  public discussion and tracking issues.
* **Contribute Code**: Please fork this repository, make your changes on your branch, and then
  create a pull request to the upstream. Please ensure your code follows the project's existing
  coding standards.

If you have more complex ideas to discuss or wish to contact us privately (e.g., to report a
security issue), you are also welcome to contact us via email.

📧 **Contact Email**: `1755858138@qq.com`

## ❤️ Support & Sponsor

`ComboLite` is a free, open-source project developed and maintained in my spare time. If this
project is helpful to you, your support would be a great motivation for my continued efforts.

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

## License

`ComboLite` is licensed under the **Apache-2.0 License**.

This means you are free to:

* **Use commercially**: Use `ComboLite` for free in your commercial products.
* **Distribute**: Freely distribute your application, whether it includes `ComboLite` or not.
* **Modify**: You can modify the source code of `ComboLite` to meet your needs.

You only need to comply with the following simple conditions:

* **Retain notice**: You must retain the original copyright and license notices in your code and
  distribution packages.
* **State changes**: If you modify the source code, you must include a notice in the modified files.

For the full license text, please refer to
the [LICENSE](https://github.com/lnzz123/ComboLite/blob/main/LICENSE) file in the project's root
directory.

