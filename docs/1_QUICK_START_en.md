# Quick Start: Build and Run Your First Plugin from Scratch

Welcome to your exploration of `ComboLite`! This guide will serve as your patient mentor, walking
you through the entire process of building and launching your first dynamic plugin in the time it
takes to drink a cup of coffee.

We will guide you through **host configuration**, **plugin creation**, and **plugin execution**. You
will experience the satisfaction of "lighting up" your first plugin without getting bogged down in
complex packaging details. Are you ready? Let's get started!

### Prerequisites

Before we begin, please ensure you have met the following conditions:

* You have successfully integrated the `ComboLite` core library into your Android project according
  to the main `README`'s instructions.

### Overall Process Preview

Before diving into the details, let's look at a simple flowchart to understand the journey we are
about to take:

```mermaid
graph LR
    A[🏠 Configure Host App] --> B[🧩 Create Plugin Module];
    B --> C{Package into APK};
    C --> D[📥 Place APK in Assets];
    D --> E[🚀 Write Loading Code];
    E --> F[🎉 Run & Witness the Magic];

    subgraph "Host"
        A
        D
        E
        F
    end

    subgraph "Plugin"
        B
    end
    
    linkStyle 2 stroke-dasharray: 5 5;
    click C "./2_PACKAGING_GUIDE.md" "Go to Packaging Guide"
```

-----

## Step 1: Configure the Host App

The host is the "home" for all plugins. We need to perform some basic initialization and
configuration for it.

### 1.1 Initialize the Plugin Framework

`ComboLite`'s initialization is very flexible, offering two methods:

#### Method 1: Fully Automatic Initialization (Recommended)

This is the easiest and most recommended approach. Simply have your `Application` class inherit from
`BaseHostApplication`, and the framework will automatically handle all initialization tasks for you,
including the plugin loader, resource manager, and crash handler.

**This is all the initialization code you need!**

```kotlin
// in :app/src/main/java/your/package/name/MainApplication.kt
import com.combo.core.base.BaseHostApplication

// Just inherit, and all configuration is done with one click
class MainApplication : BaseHostApplication() {
    override fun onCreate() {
        super.onCreate()
        // Your other application-level initialization logic
    }
}
```

#### Method 2: Manual Initialization (For Special Cases)

If your `Application` class cannot inherit from `BaseHostApplication` due to project constraints,
you can opt for manual initialization. Please ensure all steps are configured correctly to avoid
potential issues.

```kotlin
// in :app/src/main/java/your/package/name/MainApplication.kt
import android.app.Application
import android.util.Log
import com.combo.core.PluginManager
import com.combo.core.exception.PluginCrashHandler

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. (Important) Register the plugin crash handler
        PluginCrashHandler.initialize(this)

        // 2. Initialize the PluginManager
        PluginManager.initialize(this) {
            // 3. Asynchronously load enabled plugins
            // This block executes on a background thread
            val loadedCount = PluginManager.loadEnabledPlugins()
            Log.d("MyApp", "Successfully loaded $loadedCount plugins.")

            // The PluginManager's state is updated to initialized only after this block completes
        }
    }
}
```

### 1.2 Configure the Host Activity

To ensure that plugins can correctly access resources and be launched via proxy, your host`Activity`
needs to be configured.

Have your `MainActivity` (or any other host Activity) inherit from `BaseHostActivity`.

```kotlin
import com.combo.core.base.BaseHostActivity

class MainActivity : BaseHostActivity() {
    // ...
}
```

> **Important Note**
> `BaseHostActivity` internally overrides `getResources()` and `getAssets()` to ensure seamless
> resource access for both the host and plugins. It also contains the core logic for proxying plugin
`Activity` instances.
>
> **Exception Scenario**: If your project is a **pure Jetpack Compose single-Activity application**
> and you **do not need the plugin functionality for the four major components (specifically
> Activity)
**, you can choose **not to inherit** from `BaseHostActivity`. As an alternative, you must manually
> override the `getResources()` and `getAssets()` methods in your own Activity as follows:
>
> ```kotlin
> override fun getResources(): Resources {
>     return PluginManager.resourceManager.getMergedResources() ?: super.getResources()
> }
> ```

> override fun getAssets(): AssetManager {
> return PluginManager.resourceManager.getMergedResources()?.assets ?: super.getAssets()
> }
>
> ```
> ```

At this point, the basic configuration for the host is complete!

> **About the Four Major Components**
> If you need to use more advanced plugin features like Service, BroadcastReceiver, or
> ContentProvider, you will need to configure proxies and a proxy pool in your `Application` and
`AndroidManifest`. These are advanced, optional features that will be detailed in the *
*[[Advanced] Four Components Guide](./4_COMPONENTS_GUIDE_en.md)**.

-----

## Step 2: Create Your First Plugin

Now, let's create a real plugin module.

### 2.1 Create a New Module and Add Dependencies

In your project, create a new Android module. It can be of type `application` or `library`.

> **We highly recommend using a `library` module for your plugins.**
> **Reasons**:
>
>   * **Smaller Size**: Library modules do not include all dependencies by default, which, when
      combined with our packaging plugin, can produce extremely lightweight APKs.
>   * **Dependency Decoupling**: Plugins will rely on the host to provide common libraries, which
      avoids dependency conflicts and simplifies overall management.

After creating the module, add a **compile-time dependency** on `comboLite-core` in the new module's
`build.gradle.kts` file:

```kotlin
// in :your-plugin-module/build.gradle.kts
dependencies {
    // Use compileOnly to indicate that this dependency is needed at compile time
    // but will be provided by the host at runtime.
    compileOnly(projects.comboLiteCore)
    // ... other dependencies
}
```

### 2.2 Implement the Plugin Entry Class (IPluginEntryClass)

Each plugin requires an entry class that implements the `IPluginEntryClass` interface, serving as
the bridge between the plugin and the framework. This class contains the plugin's lifecycle
callbacks, UI entry point, and dependency injection configuration.

```kotlin
// in your plugin module
package com.example.myplugin

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.combo.core.data.PluginContext
import com.combo.core.entry.IPluginEntryClass
import org.koin.core.module.Module
import org.koin.dsl.module

class MyPluginEntry : IPluginEntryClass {

    /**
     * 1. (Optional) Declare Koin dependency injection modules provided by this plugin.
     * Internal dependencies of the plugin can be defined here, and the framework will integrate them automatically.
     */
    override val pluginModule: List<Module>
        get() = listOf(
            module {
                // e.g., single<MyPluginRepository> { MyPluginRepositoryImpl() }
            }
        )

    /**
     * 2. Implement the onLoad lifecycle callback.
     * This method is called after the plugin is loaded by the framework.
     * It is the best place to perform all initialization logic.
     */
    override fun onLoad(context: PluginContext) {
        println("Plugin [${context.pluginInfo.pluginId}] has been loaded, initializing...")
        // Initialize database, network, global listeners, etc., here.
    }

    /**
     * 3. Implement the onUnload lifecycle callback.
     * This method is called before the plugin is unloaded by the framework.
     * It is the best place to perform all resource cleanup tasks.
     */
    override fun onUnload() {
        println("Plugin [com.example.myplugin] is being unloaded, cleaning up resources...")
        // Close database connections, unregister listeners, etc., here.
    }

    /**
     * 4. Implement the Content method to provide the plugin's UI entry point.
     * This method is specifically for defining and returning the plugin's Jetpack Compose UI.
     */
    @Composable
    override fun Content() {
        Text("Hello from My First Plugin!")
    }
}
```

### 2.3 Configure Plugin Metadata in the Manifest

Finally, in the plugin module's `AndroidManifest.xml` file, use `<meta-data>` tags to provide the
framework with the plugin's "identity information."

```xml

<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <meta-data android:name="plugin.id" android:value="com.example.myplugin" />
        <meta-data android:name="plugin.version" android:value="1.0.0" />
        <meta-data android:name="plugin.entryClass"
            android:value="com.example.myplugin.MyPluginEntry" />
        <meta-data android:name="plugin.description" android:value="This is my first plugin." />
    </application>
</manifest>
```

-----

## Step 3: Load and Run the Plugin

### 3.1 Prepare the Plugin APK

At this point, your first plugin is fully developed! Next, you need to package this module into an
APK file. The specific methods and advanced strategies for packaging will be detailed in the *
*[[Core] Packaging Guide](https://www.google.com/search?q=./2_PACKAGING_GUIDE.md)**.

**For this guide, we will assume you have already obtained a file named `my-plugin-release.apk` by
packaging the plugin.**

For quick verification, we will preload this APK file in the host's `assets` directory for loading (
in a real-world scenario, it would typically be downloaded from a network).

1. Create an `assets` folder in your host's `:app` module, under the `src/main` directory.
2. Copy `my-plugin-release.apk` into it.

> ⚠️ **Please Pay Close Attention**
>
>   * **Exact Filename Match**: Ensure the APK filename in the `assets` directory (
      `my-plugin-release.apk`) is **identical** to the value of the `pluginApkName` variable defined
      in your `MainActivity.kt` code.
>   * **Exact Plugin ID Match**: Ensure the `plugin.id` declared in your plugin's
      `AndroidManifest.xml` (`com.example.myplugin`) is **identical** to the value of the `pluginId`
      variable defined in your `MainActivity.kt`.
>   * **Correct Directory Location**: The `assets` folder should be located in your `:app` module's
      `src/main/` directory, with the final path being `app/src/main/assets/`.

### 3.2 Write the Interaction Code (Loading from Assets)

Now, let's add the complete interaction logic to the host `MainActivity`.

<details>
<summary>👉 Click to expand the complete `MainActivity.kt` example code</summary>

```kotlin
// in :app/src/main/java/your/package/name/MainActivity.kt
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.combo.core.PluginManager
import com.combo.core.base.BaseHostActivity
import com.combo.core.data.InstallResult
import com.combo.core.ext.copyFileFromAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : BaseHostActivity() {

    // The unique ID of the plugin, must match the one declared in the plugin's AndroidManifest
    private val pluginId = "com.example.myplugin"

    // The filename of the plugin placed in the assets folder
    private val pluginApkName = "my-plugin-release.apk"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var pluginUi by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Host Application", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(32.dp))

                    // Area for displaying the plugin's UI
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pluginUi != null) {
                                pluginUi?.invoke()
                            } else {
                                Text("Plugin UI will be displayed here")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 1. Install Plugin button
                    Button(onClick = {
                        // Use the IO dispatcher for file operations to avoid blocking the main thread
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                // Copy the plugin APK from assets to the app's private directory as a prerequisite for installation
                                val pluginFile = File(context.filesDir, pluginApkName)
                                context.copyFileFromAssets(pluginApkName, pluginFile)

                                // Call the core API to install the plugin
                                val result =
                                    PluginManager.installerManager.installPlugin(pluginFile)

                                // After the operation is complete, switch back to the main thread to update the UI or show a toast
                                withContext(Dispatchers.Main) {
                                    when (result) {
                                        is InstallResult.Success -> {
                                            Toast.makeText(
                                                context,
                                                "Plugin [${result.pluginInfo.pluginId}] installed successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        is InstallResult.Failure -> {
                                            Toast.makeText(
                                                context,
                                                "Plugin installation failed: ${result.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Operation failed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }) {
                        Text("1. Install Plugin from Assets")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Launch Plugin and display UI button
                    Button(onClick = {
                        coroutineScope.launch {
                            // Launch the plugin (if already launched, it will perform a chain restart to ensure it's the latest state)
                            PluginManager.launchPlugin(pluginId)

                            // Get the plugin instance and assign its @Composable Content() method to the UI state
                            val pluginInstance = PluginManager.getPluginInstance(pluginId)
                            if (pluginInstance != null) {
                                pluginUi = { pluginInstance.Content() }
                                Toast.makeText(
                                    context,
                                    "Plugin [${pluginId}] launched successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Plugin [${pluginId}] not found or failed to load",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }) {
                        Text("2. Launch and Display Plugin")
                    }
                }
            }
        }
    }
}
```

</details>

### 3.3 Run and Verify

Now, run your host app and follow the on-screen button order:

1. Click the **"1. Install Plugin from Assets"** button. The app will read the APK from `assets` and
   complete the installation. You will see a "installed successfully" Toast message.
2. Click the **"2. Launch and Display Plugin"** button. The framework will load the plugin, and the
   text **"Hello from My First Plugin!"** will appear on the screen.

After completing all the steps and clicking the buttons, your app interface should look like this:

*(Please replace this path with your actual screenshot path)*

## Congratulations! & Next Steps

Fantastic! You have successfully completed the entire loop of `ComboLite`'s plugin development. This
is more than just a "Hello World"; it's the key to unlocking the door to modern, dynamic app
construction. We are proud of you!

If you encounter any issues during this process, you can always refer to the complete, runnable
quick-start sample code we have prepared for you.

Now that you've mastered the basics, it's time to dive deeper into `ComboLite`'s more powerful
features:

* **[[Core] Packaging Guide](./2_PACKAGING_GUIDE_en.md)**: Learn how to properly package your plugin
  modules into APKs.
* **[[Advanced] Core API Usage](./3_CORE_APIS_en.md)**: Master the rich interface provided by
  `PluginManager`.
* **[[Advanced] Four Components Guide](./4_COMPONENTS_GUIDE_en.md)**: Give your plugins the power of
  Activity, Service, and more.
* **[[Principles] Architecture & Design](./5_ARCHITECTURE_en.md)**: Delve into the magic behind how
  `ComboLite` works.