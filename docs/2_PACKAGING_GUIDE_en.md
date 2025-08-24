# Plugin Packaging Guide

Welcome to the `ComboLite` Plugin Packaging Guide!

"Packaging" is the crucial step that transforms your plugin module (whether it's a `Library` or an
`Application` type) into a standalone APK file that can be dynamically loaded, installed, and run by
the host. Mastering the correct packaging strategy is vital for creating lightweight plugins,
avoiding dependency conflicts, and improving project maintainability.

This guide will delve into the two packaging schemes supported by `ComboLite`, with a special focus
on our officially recommended workflow: packaging `Library` modules using the `aar2apk` Gradle
plugin.

-----

## Core Concepts: The Foundation of Packaging Strategy

Before diving into the specific operations, let's first grasp the two core concepts that determine
your packaging strategy.

### 1. Two Plugin Formats: Library vs. Application

`ComboLite` gives you great flexibility, allowing you to develop plugins from two different types of
Android modules:

* 📦 **Library Module (Recommended)**

    * **Essence**: A standard `com.android.library` module (output is an AAR).
    * **Characteristics**: Lightweight and non-standalone. It does not contain common dependencies
      like the Kotlin standard library or AndroidX; it assumes these will be provided by the host
      app at runtime. This is the top choice for building "Super Apps" and pursuing ultimate
      performance.
    * **Packaging Method**: It needs to be "promoted" into an installable APK using our provided
      `aar2apk` Gradle plugin.

* 📱 **Application Module**

    * **Essence**: A standard `com.android.application` module (output is an APK).
    * **Characteristics**: Standalone and self-contained. It acts like a mini-app, packaging most of
      its own dependencies inside it.
    * **Packaging Method**: Uses the standard Android official build process.

### 2. Dependency Scopes: `compileOnly` vs. `implementation`

Correctly using Gradle's dependency scopes is the magic behind creating lightweight,
dependency-sharing plugins.

* ✅ **`compileOnly` (Preferred for Plugins)**

    * **Meaning**: "Compile-time only dependency." It tells the compiler: "This library is necessary
      to **compile my plugin code**, but please **do not** package it into the final plugin APK. I
      promise that the host app will provide this library at runtime."
    * **Usage**: This is the **primary and most commonly used** scope for plugin modules. All common
      libraries you expect the host to provide (e.g., `comboLite-core`, `Kotlin`, `AndroidX`,
      `OkHttp`) should use `compileOnly`.

* ⚠️ **`implementation`**

    * **Meaning**: "Implementation dependency." It tells the compiler: "This library is needed for
      both compilation and runtime, so please make sure to **package** it into the final artifact."
    * **Usage**:
        1. When packaging an `Application` module plugin, it's used to include the plugin's private
           dependencies.
        2. When packaging a `Library` module plugin, you only need to change a dependency from
           `compileOnly` to `implementation` if you plan to use advanced features of `aar2apk` (like
           `includeDependenciesDex`) to **intentionally** package that specific dependency.

> **The Golden Rule**:
> **For plugin development, `compileOnly` is the norm, `implementation` is the exception.**

-----

## Scheme 1: Packaging Library Modules (Official Recommendation)

This is `ComboLite`'s most central and powerful packaging method. We have built-in a Gradle plugin
named `aar2apk` specifically designed to convert lightweight `Library` modules into fully-functional
plugin APKs with a single command.

### 1. Include and Apply the Plugin

First, ensure the `aar2apk` plugin itself can be found by your project.

① **Include the plugin project in your root `settings.gradle.kts`**:

```kotlin
// in your project's /settings.gradle.kts
includeBuild("../ComboLite") {
    dependencySubstitution {
        // When you depend on the aar2apk plugin in your project, Gradle will automatically 
        // replace it with the local build-logic module.
        substitute(module("com.combo.aar2apk")).using(project(":build-logic"))
    }
}
```

② **Apply the plugin in your root `build.gradle.kts`**:

```kotlin
// in your project's root /build.gradle.kts
plugins {
    // ... other plugins
    id("com.combo.aar2apk")
}
```

### 2. Configure the Packaging Task

All configuration is done within the `aar2apk` block in your root `build.gradle.kts`. You can set up
global signing information and specify individual packaging strategies for each plugin module you
need to build.

```kotlin
// in your project's root /build.gradle.kts
aar2apk {
    // a. (Optional) Configure global signing information
    // If configured, all plugins will use this signature. Release builds must be signed.
    signing {
        keystorePath.set(rootProject.file("your_keystore.jks").absolutePath)
        keystorePassword.set("your_keystore_password")
        keyAlias.set("your_key_alias")
        keyPassword.set("your_key_password")
    }

    // b. Declare all Library modules to be packaged as plugins
    modules {
        // Strategy 1: Default minimal packaging (Most common)
        // Does not package any external dependency code or resources. The plugin size is minimal.
        module(":plugin-user")
        module(":plugin-settings")

        // Strategy 2: Package partial dependencies (For special cases)
        // For example, :plugin-reader depends on a library A with drawable resources, and the host doesn't have library A.
        module(":plugin-reader") {
            includeDependenciesRes.set(true) // Only package resources from external dependencies
            // Note: Library A must be included with `implementation` in :plugin-reader's build file.
        }

        // Strategy 3: Package all dependencies (Rarely used, high risk)
        // Suitable for plugins that depend on a completely private library that neither the host nor other plugins have.
        module(":plugin-private") {
            includeAllDependencies() // Convenience method, equivalent to setting the four below to true
            
            // Or use fine-grained control
            // includeDependenciesDex.set(true)    // Package dependency code
            // includeDependenciesRes.set(true)    // Package dependency resources
            // includeDependenciesAssets.set(true) // Package dependency assets
            // includeDependenciesJni.set(true)    // Package dependency JNI libraries
        }
    }
}
```

### 3. Available Packaging Commands (Gradle Tasks)

Once configured, the `aar2apk` plugin will automatically generate a series of convenient Gradle
tasks for your project.

You can find them in the **Android Studio Gradle task panel** (usually on the right side) under the
`Plugin APKs` group. Double-click to execute them.

*(Please replace this with your actual screenshot)*

Alternatively, you can execute the following commands in your terminal:

```bash
# [Recommended] Build all configured Library plugins in one go (Release version)
./gradlew buildAllReleasePluginApks

# [Recommended] Build all configured Library plugins in one go (Debug version)
./gradlew buildAllDebugPluginApks

# Build a single plugin (Release version)
./gradlew :plugin-user:buildReleasePluginApk

# Build a single plugin (Debug version)
./gradlew :plugin-user:buildDebugPluginApk

# Clean all plugin build artifacts (APKs, logs, temporary files)
./gradlew cleanAllPluginApks
```

The packaged artifacts will be located in your project's root `build/outputs/plugin-apks/`
directory, automatically sorted into `debug` and `release` subfolders.

### 4. Pros and Cons

* ✅ **Pros**:
    * **Extremely Lightweight**: The APK size is typically only tens to hundreds of KB, making
      update and download costs very low.
    * **Eliminates Dependency Conflicts**: All plugins share the same dependencies provided by the
      host, fundamentally avoiding runtime crashes caused by version mismatches.
    * **Unified Dependency Management**: Dependency versions are upgraded and managed centrally by
      the host, reducing maintenance costs.
* ⚠️ **Cons**:
    * **Relies on Host Environment**: The plugin's execution is heavily dependent on the host. If
      the host fails to provide a dependency declared with `compileOnly`, the plugin will fail to
      start due to a `ClassNotFoundException` and will be automatically disabled by the framework's
      fusing mechanism.

-----

## Scheme 2: Packaging Application Modules (Alternative)

Using a standard `com.android.application` module as a plugin is a traditional method that is still
useful in specific scenarios.

### 1. Configuration Steps

**① Add the Core Dependency**
In the plugin module's `build.gradle.kts`, declare the framework's core library as `compileOnly`.

```kotlin
// in your-application-plugin/build.gradle.kts
dependencies {
    compileOnly(projects.comboLiteCore)
    // This gson library will be packaged into the plugin APK
    implementation("com.google.code.gson:gson:2.9.0")
}
```

**② (❗Critically Important) Configure Package ID**
To avoid resource ID conflicts (`R.java`) with the host or other plugins, you **must** manually
specify a unique `Package ID` for each `Application` plugin module.

```kotlin
// in your-application-plugin/build.gradle.kts
android {
    // ...
    aaptOptions {
        // This hexadecimal value must be unique among all Application plugins
        additionalParameters("--package-id", "0x80") 
    }
}
```

> **Note**: The valid range for `Package ID` is from `0x02` to `0x7F`. The host app's ID is `0x7F`.
> Please assign a different value from this range to each of your Application plugins. For example,
> Plugin A uses `0x7E`, Plugin B uses `0x7D`, and so on.

### 2. Execute Packaging

You can complete the packaging using the standard tasks provided by AGP.

```bash
./gradlew :your-application-plugin:assembleRelease
```

### 3. Pros and Cons

* ✅ **Pros**:
    * **Highly Independent**: The plugin is self-contained with all its dependencies, making
      deployment simple as it doesn't rely on the host's external environment.
    * **No Compatibility Worries**: No need to worry about whether the host provides the required
      libraries or versions that the plugin needs.
* ⚠️ **Cons**:
    * **Larger Size**: Since all dependencies are packaged, the plugin APK will be relatively
      bloated.
    * **Potential Dependency Risks**: If the libraries packaged in the plugin conflict with the
      versions in the host, it can lead to hard-to-debug runtime errors.

-----

## How to Choose?

| Use Case                                                                | Recommended Scheme             | Rationale                                                                                                         |
|:------------------------------------------------------------------------|:-------------------------------|:------------------------------------------------------------------------------------------------------------------|
| **UI component libraries, utility classes, common business**            | **✅ Library Module (aar2apk)** | **Preferred choice**. Single-purpose, small size, suitable for frequent updates and reuse as a shared resource.   |
| **"Super App" with a large number of plugins**                          | **✅ Library Module (aar2apk)** | Maximizes reuse of common dependencies, significantly reducing the app's total size and memory footprint.         |
| **Extreme requirements for update speed and size**                      | **✅ Library Module (aar2apk)** | The tiny package size makes the experience of dynamic delivery and hot-updating almost seamless.                  |
| **Large, independent functional modules** (e.g., shopping, game center) | **⚠️ Application Module**      | Complex business logic with many dependencies, requiring high cohesion and independence.                          |
| **Plugins provided for third-party integration**                        | **⚠️ Application Module**      | The host environment cannot be controlled, so all dependencies must be self-contained to ensure stable operation. |

-----

## Important Practices and Risk Warnings

### Deep Dive Warning: Be Cautious About Packaging Full Dependencies

Although the `aar2apk` plugin provides the capability to package dependencies (code, resources, JNI,
etc.) into a plugin via `includeAllDependencies()`, this should be considered an **alternative
solution for special cases, not a routine operation**.

> **In the vast majority of cases, we strongly recommend you use the default minimal packaging mode.
**

Packaging full dependencies can introduce a series of severe and hard-to-diagnose problems:

* **Class Duplication Conflicts**: If a plugin packages `OkHttp 4.9.0`, while the host or another
  plugin uses `OkHttp 4.10.0`, the runtime may crash with fatal errors like `NoSuchMethodError` or
  `ClassCastException` due to inconsistent class definitions.
* **Resource Duplication and Overwriting**: If multiple plugins or the host contain resources with
  the same name, the resource loaded at runtime might not be the one you expect, leading to UI
  glitches.
* **APK Size Bloat**: Repackaging the same dependency libraries repeatedly will significantly
  increase the plugin APK's size and the final application's total size.

**Rule of Thumb**: Only consider packaging a dependency if you are **absolutely certain** that it is
**exclusively private** to this plugin and will not conflict with the host or any other plugin.

### Special Note: Impact of UI Technology Choice on Packaging

In a plugin context, your choice of UI technology (Jetpack Compose or XML) will directly impact the
complexity of your packaging.

* **Jetpack Compose (Recommended)**: Perfectly compatible with `ComboLite`'s `compileOnly`
  dependency-sharing strategy, with almost no compatibility issues.
* **XML Layouts**: In `compileOnly` mode, you will face **difficulties with cross-module resource
  references**, which can lead to build failures or require more complex packaging configurations.

> **We strongly recommend you prioritize using Jetpack Compose in your plugins**.
> For an in-depth discussion of the specific problems you might encounter with XML and their
> solutions, please refer to the "UI Implementation Technology Choice" section in the *
*[[Advanced] Four Components Guide](./4_COMPONENTS_GUIDE_en.md)**.
