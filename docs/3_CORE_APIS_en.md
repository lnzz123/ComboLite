Of course. Here is the complete English translation of your `3_CORE_APIS.md` document.

-----

# Core API Guide

Welcome to the central hub of `ComboLite`!

This guide serves as the authoritative reference for all public APIs within the `ComboLite`
framework. We will dive deep into the several core managers and introduce convenient extension
functions to help you gain complete control over every detail of pluginization.

**Document Structure**

1. [**`InstallerManager` (The Installer)**](#i-installermanager-the-installer): Responsible for the
   physical installation, version verification, and uninstallation of plugins.
2. [**`ProxyManager` (The Dispatcher)**](#ii-proxymanager-the-dispatcher): Responsible for the proxy
   configuration and lifecycle management of the four major components.
3. [**`PluginResourcesManager` (The Resource Manager)
   **](#iii-pluginresourcesmanager-the-resource-manager): Responsible for the merging and dynamic
   management of plugin resources.
4. [**`PluginManager` (The Master Controller)**](#iv-pluginmanager-the-master-controller): The
   framework's supreme commander, responsible for the runtime lifecycle, dependency relationships,
   and cross-plugin communication.
5. [**`Context` Extension Functions (Convenient Wrappers)
   **](#v-context-extension-functions-convenient-wrappers): The most frequently used and highly
   recommended APIs for daily development.

-----

## I. `InstallerManager` (The Installer)

The physical file management of plugins, including installation, uninstallation, and version
verification, is handled by `PluginManager.installerManager`. These operations involve file I/O and
are **highly recommended to be executed on a background thread**.

### `installPlugin`

The core method for transforming an external APK file into a usable plugin.

```kotlin
suspend fun installPlugin(
    pluginApkFile: File,
    forceOverwrite: Boolean = false
): InstallResult
```

* `pluginApkFile`: A `File` object pointing to the plugin APK.
* `forceOverwrite`: Whether to force an overwrite installation. Defaults to `false`, in which case
  the installation will fail if the currently installed plugin's version is equal to or higher than
  the one being installed. Set to `true` to allow for forced downgrades or overwrites.
* **Returns**: An `InstallResult` sealed class, containing either a `Success` or `Failure` state.

> **Security and Robustness of the Installation Process**
> `installPlugin` is more than just a file copy. It performs a strict internal process:
>
> 1. **🛡️ Signature Validation**: Ensures the plugin's signature matches the host's.
> 2. **📋 Metadata Parsing**: Verifies the completeness of the `AndroidManifest.xml` configuration.
> 3. **⚖️ Version Comparison**: By default, prevents downgrades unless `forceOverwrite` is `true`.
> 4. **📦 Component Parsing**: Automatically parses static broadcasts and `ContentProvider`
     declarations within the plugin.
> 5. **🔄 Safe Overwrite**: When updating, it first backs up the old plugin, deletes the backup only
     after a successful copy, and rolls back on failure.

**Code Example**:

```kotlin
val pluginApkFile = File(context.cacheDir, "my-plugin.apk")
// ... download plugin to this file from the network ...

coroutineScope.launch(Dispatchers.IO) {
    val result = PluginManager.installerManager.installPlugin(pluginApkFile)
    withContext(Dispatchers.Main) {
        when (result) {
            is InstallResult.Success -> {
                toast("Plugin ${result.pluginInfo.pluginId} installed successfully!")
                // After successful installation, it's common to launch it immediately
                PluginManager.launchPlugin(result.pluginInfo.pluginId)
            }
            is InstallResult.Failure -> {
                toast("Installation failed: ${result.reason}")
            }
        }
    }
}
```

### `uninstallPlugin`

Completely removes a plugin and its physical file from the system.

```kotlin
fun uninstallPlugin(pluginId: String): Boolean
```

* `pluginId`: The unique ID of the plugin to be uninstalled.
* **Returns**: `true` if the operation was successful, `false` otherwise.

**Code Example**:

```kotlin
coroutineScope.launch(Dispatchers.IO) {
    val success = PluginManager.installerManager.uninstallPlugin("com.example.myplugin")
    withContext(Dispatchers.Main) {
        if (success) toast("Plugin uninstalled") else toast("Uninstallation failed")
    }
}
```

-----

## II. `ProxyManager` (The Dispatcher)

`ProxyManager` is the "behind-the-scenes hero" that enables the pluginization of the four major
components. It uses the **proxy pattern** to "graft" the lifecycle of plugin components onto proxy
components that are pre-registered in the host's `AndroidManifest.xml`.

**Configuration is required before using any of the four major component features.**

### 1. Configuration APIs (Typically called in Application.onCreate)

#### `setHostActivity`

Configures the host `Activity` class that will be used to proxy all plugin `Activity` instances.

```kotlin
fun setHostActivity(hostActivity: Class<out BaseHostActivity>)
```

**Example**: `PluginManager.proxyManager.setHostActivity(HostActivity::class.java)`

#### `setServicePool`

Configures the "proxy service pool" for proxying plugin `Service` instances.

```kotlin
fun setServicePool(serviceProxyPool: List<Class<out BaseHostService>>)
```

* `serviceProxyPool`: A `List` of `Class` objects, containing all the proxy `Service` classes that
  inherit from `BaseHostService` and are registered in the host's `Manifest`.

**Example**:

```kotlin
val pool = listOf(
    HostService1::class.java,
    HostService2::class.java,
    HostService3::class.java
)
PluginManager.proxyManager.setServicePool(pool)
```

#### `setHostProviderAuthority`

Configures the `Authority` of the host `Provider` that will be used to proxy all plugin
`ContentProvider` instances.

```kotlin
fun setHostProviderAuthority(authority: String)
```

* `authority`: The `Authority` string that was registered for your `BaseHostProvider` in the host's
  `Manifest`.

**Example**:
`PluginManager.proxyManager.setHostProviderAuthority("com.your.host.app.provider.proxy")`

### 2. Service Proxy Pool APIs (Advanced)

These APIs reveal the underlying mechanics of the multi-instance `Service` feature and are typically
called by the framework's internal extension functions.

* `acquireServiceProxy(instanceIdentifier: String)`: Requests an available proxy for a plugin
  Service instance.
* `releaseServiceProxy(instanceIdentifier: String)`: Releases a proxy, returning it to the available
  pool.
* `getServiceProxyFor(instanceIdentifier: String)`: Gets the proxy currently assigned to a specific
  instance.
* `getRunningInstancesFor(serviceClassName: String)`: Gets a list of all currently running instance
  IDs for a given Service class. **This API is very useful for UI scenarios that need to synchronize
  with the state of background services.**

-----

## III. `PluginResourcesManager` (The Resource Manager)

`PluginResourcesManager` implements a **merged management** system for all plugin resources,
supporting Android 11+'s `ResourcesLoader` API and reflection-based solutions for older versions
under the hood.

> **For developers, resource management is completely transparent.**
> You **do not need** and **should not** call any APIs of `PluginResourcesManager` directly.
> As long as your Activity inherits from `BaseHostActivity` or you override `getResources()` as
> required, you can access resources from **any loaded plugin** seamlessly using `R.string.xxx`,
`R.drawable.xxx`, etc., just as you would with the host's own resources.

### `getResources` (The only API you need to know)

Gets the current `Resources` object, which has been merged with all plugin resources.

```kotlin
fun getResources(): Resources
```

**Usage**: This method is primarily intended for you to manually override in a custom `Activity`
that **cannot inherit from `BaseHostActivity`**, allowing you to integrate with `ComboLite`'s
resource management system.

```kotlin
override fun getResources(): Resources {
    // Always get the latest, merged resources object from PluginManager
    return PluginManager.resourcesManager.getResources() ?: super.getResources()
}
```

-----

## IV. `PluginManager` (The Master Controller)

`PluginManager` is the framework's supreme commander, providing complete control over the runtime
lifecycle, dependency relationships, and cross-plugin communication.

### 1. Starting and Stopping

#### `launchPlugin`

Starts or restarts a plugin. This is the **most central and commonly used** runtime API.

```kotlin
suspend fun launchPlugin(pluginId: String): Boolean
```

> **Key Feature: Chain Restart**
> When you call `launchPlugin` on a "base plugin" that is already a dependency for other loaded
> plugins (typically for a hot-update), the framework automatically identifies all of its upstream
> dependents. It then unloads and reloads them **as a single unit and in the correct order**,
> ensuring
> the data consistency of the entire dependency chain. This process is completely transparent to the
> developer.

#### `unloadPlugin`

Removes a loaded plugin from memory and **thoroughly cleans up** all of its runtime resources.

```kotlin
suspend fun unloadPlugin(pluginId: String)
```

### 2. State Querying and Listening

#### `isPluginLoaded`

A synchronous check to determine if a plugin is currently in a loaded state in memory.

```kotlin
fun isPluginLoaded(pluginId: String): Boolean
```

#### `getPluginInstance`

Gets the instance of the plugin's entry class (`IPluginEntryClass`), used for direct interaction.

#### `getAllPluginIds`

Gets a list of all **currently loaded** plugin IDs.

#### `loadedPluginsFlow` / `pluginInstancesFlow` (Advanced)

Subscribe to a `StateFlow` of the `Map` of all **currently loaded** plugins' information (
`LoadedPluginInfo`) and instances (`IPluginEntryClass`), respectively. This can be used to build
reactive UIs that respond to plugin state changes.

### 3. Service Discovery and Dependency Querying

#### `getInterface` (Ace Feature)

Gets an instance of an interface without being aware of its implementation's location. This is the
best way to achieve **cross-plugin service calls** and **ultimate decoupling**.

**How it works**: `getInterface` utilizes the **global class index** built when plugins are loaded.
You only need to provide the interface's `Class` and the implementation's fully qualified class
name, and `PluginManager` will automatically locate the plugin containing that implementation and
return its instance.

**Example**:

```kotlin
// Define an interface in a common module
interface IUserService {
    fun getInfo(): String
}

// Implement the interface in a user plugin
class UserServiceImpl : IUserService {
    override fun getInfo() = "UserInfo"
}

// Call from the host or any other plugin
val userService = PluginManager.getInterface(
    IUserService::class.java,
    "com.example.user.UserServiceImpl"
)
val info = userService?.getInfo()
```

#### `getPluginDependentsChain` / `getPluginDependenciesChain`

Query the complex dependency network between plugins.

* **`getPluginDependentsChain(pluginId)`**: Asks "**Who depends on me?**" Used for safety checks
  before uninstalling or updating.
* **`getPluginDependenciesChain(pluginId)`**: Asks "**On whom do I depend?**" Used for debugging and
  diagnostics.

-----

## V. `Context` Extension Functions (Convenient Wrappers)

To simplify daily development, `ComboLite` wraps the most common functionalities of the managers
above into a series of `Context` extension functions. **In your daily business logic development, we
strongly recommend that you prioritize using these convenient wrappers**.

<details>
<summary>👉 Click to expand the full list of extension function APIs</summary>

| Category                | Function Signature                                       | Description                                                  |
|:------------------------|:---------------------------------------------------------|:-------------------------------------------------------------|
| **Activity**            | `startPluginActivity(cls, options, block)`               | Starts a plugin Activity.                                    |
| **Service**             | `startPluginService(cls, instanceId, block)`             | Starts a plugin Service (supports multi-instance).           |
|                         | `bindPluginService(cls, instanceId, conn, flags, block)` | Binds to a plugin Service (supports multi-instance).         |
|                         | `stopPluginService(cls, instanceId, block)`              | Stops a plugin Service (supports multi-instance).            |
| **Broadcast**           | `sendInternalBroadcast(action, block)`                   | Sends a secure internal broadcast.                           |
| **Content<br>Provider** | `queryPlugin(uri, ...)`                                  | Queries a plugin `ContentProvider`.                          |
|                         | `insertPlugin(uri, ...)`                                 | Inserts data into a plugin `ContentProvider`.                |
|                         | `deletePlugin(uri, ...)`                                 | Deletes data from a plugin `ContentProvider`.                |
|                         | `updatePlugin(uri, ...)`                                 | Updates data in a plugin `ContentProvider`.                  |
|                         | `callPlugin(uri, method, ...)`                           | Calls a custom method on a plugin `ContentProvider`.         |
|                         | `registerPluginObserver(uri, ..., observer)`             | Registers a content observer for a plugin `ContentProvider`. |
|                         | `unregisterPluginObserver(observer)`                     | Unregisters a content observer.                              |

</details>