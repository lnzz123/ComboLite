# 核心 API 指南

欢迎来到 `ComboLite` 的核心中枢！

本指南是 `ComboLite` 框架所有公开 API 的权威参考。我们将深入框架的几大核心管理器，并介绍便捷的扩展函数，助你完全掌控插件化的每一个细节。

**文档结构**

1. [**`InstallerManager` (安装器)**](#一installermanager-安装器)：负责插件的物理安装、版本校验和卸载。
2. [**`ProxyManager` (调度器)**](#二proxymanager-调度器)：负责四大组件的代理配置与生命周期管理。
3. [**`PluginResourcesManager` (资源管理器)**](#三pluginresourcesmanager-资源管理器)：负责插件资源的合并与动态管理。
4. [**`PluginManager` (总控制器)**](#四pluginmanager-总控制器)：框架的最高指挥官，负责插件的运行时生命周期、依赖关系和跨插件通信。
5. [**`Context` 扩展函数**](#五context-扩展函数便捷封装)：日常开发中最高频、最推荐使用的 API。

-----

## 一、`InstallerManager` (安装器)

插件的物理文件管理，包括安装、卸载和版本校验，都由 `PluginManager.installerManager` 负责。这些操作涉及文件
I/O，**强烈建议在后台线程中执行**。

### `installPlugin`

将一个外部 APK 文件转化为一个可用插件的核心方法。

```kotlin
suspend fun installPlugin(
    pluginApkFile: File,
    forceOverwrite: Boolean = false
): InstallResult
```

* `pluginApkFile`: 指向插件 APK 的 `File` 对象。
* `forceOverwrite`: 是否强制覆盖安装。默认为 `false`，此时如果已安装插件的版本等于或高于当前待安装版本，安装将失败。设为
  `true` 可用于强制降级或覆盖。
* **返回**: `InstallResult` 密封类，包含 `Success` 和 `Failure` 两种状态。

> **安装过程的安全性与健壮性**
> `installPlugin` 不仅仅是文件复制。它在内部执行了严格的流程：
>
> 1. **🛡️ 签名校验**: 确保插件与宿主签名一致。
> 2. **📋 元数据解析**: 验证 `AndroidManifest.xml` 配置是否完整。
> 3. **⚖️ 版本对比**: 默认禁止降级安装，除非 `forceOverwrite` 为 `true`。
> 4. **📦 组件解析**: 自动解析插件中声明的静态广播和 `ContentProvider`。
> 5. **🔄 安全覆盖**: 更新时，会先备份旧插件，复制成功后再删除备份，失败则回滚。

**示例代码**:

```kotlin
val pluginApkFile = File(context.cacheDir, "my-plugin.apk")
// ... 从网络下载插件到该文件 ...

coroutineScope.launch(Dispatchers.IO) {
    val result = PluginManager.installerManager.installPlugin(pluginApkFile)
    withContext(Dispatchers.Main) {
        when (result) {
            is InstallResult.Success -> {
                toast("插件 ${result.pluginInfo.pluginId} 安装成功！")
                // 安装成功后，通常会立即启动它
                PluginManager.launchPlugin(result.pluginInfo.pluginId)
            }
            is InstallResult.Failure -> {
                toast("安装失败: ${result.reason}")
            }
        }
    }
}
```

### `uninstallPlugin`

从系统中彻底移除一个插件及其物理文件。

```kotlin
fun uninstallPlugin(pluginId: String): Boolean
```

* `pluginId`: 要卸载的插件的唯一 ID。
* **返回**: 操作是否成功。

**示例代码**:

```kotlin
coroutineScope.launch(Dispatchers.IO) {
    val success = PluginManager.installerManager.uninstallPlugin("com.example.myplugin")
    withContext(Dispatchers.Main) {
        if (success) toast("插件已卸载") else toast("卸载失败")
    }
}
```

-----

## 二、`ProxyManager` (调度器)

`ProxyManager` 是实现四大组件插件化的“幕后功臣”。它通过**代理模式**，将插件组件的生命周期“嫁接”到在宿主
`AndroidManifest.xml` 中预先注册的代理组件上。

**在使用任何四大组件功能前，必须先进行配置。**

### 1. 配置 API (通常在 Application.onCreate 中调用)

#### `setHostActivity`

配置用于代理所有插件 `Activity` 的宿主 `Activity` 类。

```kotlin
fun setHostActivity(hostActivity: Class<out BaseHostActivity>)
```

**示例**: `PluginManager.proxyManager.setHostActivity(HostActivity::class.java)`

#### `setServicePool`

配置用于代理插件 `Service` 的“代理服务池”。

```kotlin
fun setServicePool(serviceProxyPool: List<Class<out BaseHostService>>)
```

* `serviceProxyPool`: 一个 `Class` 列表，包含所有在宿主 `Manifest` 中注册的、继承自 `BaseHostService`
  的代理 `Service`。

**示例**:

```kotlin
val pool = listOf(
    HostService1::class.java,
    HostService2::class.java,
    HostService3::class.java
)
PluginManager.proxyManager.setServicePool(pool)
```

#### `setHostProviderAuthority`

配置用于代理所有插件 `ContentProvider` 的宿主 `Provider` 的 `Authority`。

```kotlin
fun setHostProviderAuthority(authority: String)
```

* `authority`: 在宿主 `Manifest` 中为 `BaseHostProvider` 注册的 `Authority` 字符串。

**示例**: `PluginManager.proxyManager.setHostProviderAuthority("com.your.host.app.provider.proxy")`

### 2. Service 代理池 API (进阶)

这些 API 揭示了 `Service` 多实例机制的底层原理，通常被框架内部的扩展函数调用。

* `acquireServiceProxy(instanceIdentifier: String)`: 为一个插件 Service 实例请求一个可用的代理。
* `releaseServiceProxy(instanceIdentifier: String)`: 释放一个代理，使其返回可用池中。
* `getServiceProxyFor(instanceIdentifier: String)`: 获取某个实例当前占用的代理。
* `getRunningInstancesFor(serviceClassName: String)`: 获取某个 Service 类的所有正在运行的实例ID列表。
  **此 API 对于需要与后台服务状态同步的 UI 场景非常有用。**

-----

## 三、`PluginResourcesManager` (资源管理器)

`PluginResourcesManager` 实现了对所有插件资源的**合并式管理**，底层兼容了 Android 11+ 的
`ResourcesLoader` API 和低版本的反射方案。

> **对开发者而言，资源管理是完全透明的。**
> 你**不需要**也**不应该**直接调用 `PluginResourcesManager` 的任何 API。
> 只要你的 Activity 继承了 `BaseHostActivity` 或按要求重写了 `getResources()` 方法，你就可以像访问宿主自身资源一样，使用
`R.string.xxx`、`R.drawable.xxx` 等方式，无差别地访问来自**任何已加载插件**的资源。

### `getResources` (唯一需要关心的 API)

获取当前合并了所有插件资源的 `Resources` 对象。

```kotlin
fun getResources(): Resources
```

**用法**: 此方法主要用于在你**无法继承 `BaseHostActivity`** 的自定义 `Activity` 中进行手动重写，以接入
`ComboLite` 的资源管理体系。

```kotlin
override fun getResources(): Resources {
    // 始终从 PluginManager 获取最新的、合并后的资源对象
    return PluginManager.resourcesManager.getResources() ?: super.getResources()
}
```

-----

## 四、`PluginManager` (总控制器)

`PluginManager` 是框架的最高指挥官，提供对插件运行时生命周期、依赖关系和跨插件通信的全面控制。

### 1. 启动与停止

#### `launchPlugin`

启动或重启一个插件，这是**最核心、最常用**的运行时 API。

```kotlin
suspend fun launchPlugin(pluginId: String): Boolean
```

> **重要特性：链式重启 (Chain Restart)**
> 当你对一个已经被其他插件依赖的“基础插件”调用 `launchPlugin` 时（通常是为了热更新），框架会自动找出所有依赖它的上游插件，并将它们
**作为一个整体、按正确的顺序**进行卸载和重新加载，从而保证整个依赖链的数据一致性。这个过程对开发者完全透明。

#### `unloadPlugin`

将一个已加载的插件从内存中移除，并**彻底清理**其所有运行时资源。

```kotlin
suspend fun unloadPlugin(pluginId: String)
```

### 2. 状态查询与监听

#### `isPluginLoaded`

同步检查插件当前是否在内存中。

```kotlin
fun isPluginLoaded(pluginId: String): Boolean
```

#### `getPluginInstance`

获取插件入口类 `IPluginEntryClass` 的实例，用于直接交互。

#### `getAllPluginIds`

获取所有**当前已加载**的插件 ID 列表。

#### `loadedPluginsFlow` / `pluginInstancesFlow` (进阶)

以 `StateFlow` 的形式，分别订阅当前**所有已加载**插件的信息 (`LoadedPluginInfo`) 和实例 (
`IPluginEntryClass`) 的 `Map`。可用于构建能实时反应插件状态的响应式 UI。

### 3. 服务发现与依赖查询

#### `getInterface` (王牌功能)

在不感知具体实现位置的情况下，获取一个接口的实例。这是实现**跨插件服务调用**和**极致解耦**的最佳方式。

**工作原理**: `getInterface` 利用了在插件加载时建立的**全局类索引**。你只需要提供接口的 `Class`
和实现的完整类名，`PluginManager` 就会自动定位到包含该实现的插件，并返回其实例。

**示例**：

```kotlin
// 公共模块中定义接口
interface IUserService { fun getInfo(): String }

// 用户插件中实现
class UserServiceImpl : IUserService { override fun getInfo() = "UserInfo" }

// 宿主或任何其他插件中调用
val userService = PluginManager.getInterface(
    IUserService::class.java,
    "com.example.user.UserServiceImpl"
)
val info = userService?.getInfo()
```

#### `getPluginDependentsChain` / `getPluginDependenciesChain`

查询插件之间复杂的依赖网络。

* **`getPluginDependentsChain(pluginId)`**: 查询“**谁依赖我？**”，用于卸载/更新前的安全检查。
* **`getPluginDependenciesChain(pluginId)`**: 查询“**我依赖谁？**”，用于调试和诊断。

-----

## 五、`Context` 扩展函数(便捷封装)

为了简化日常开发，`ComboLite` 将上述管理器中最常用的功能封装成了一系列 `Context` 扩展函数。*
*在日常业务开发中，我们强烈建议您优先使用这些便捷的封装**。

<details>
<summary>👉 点击展开所有扩展函数 API 列表</summary>

| 分类                      | 函数签名                                                     | 描述                             |
|:------------------------|:---------------------------------------------------------|:-------------------------------|
| **Activity**            | `startPluginActivity(cls, options, block)`               | 启动一个插件 Activity。               |
| **Service**             | `startPluginService(cls, instanceId, block)`             | 启动一个插件 Service（支持多实例）。         |
|                         | `bindPluginService(cls, instanceId, conn, flags, block)` | 绑定到一个插件 Service（支持多实例）。        |
|                         | `stopPluginService(cls, instanceId, block)`              | 停止一个插件 Service（支持多-实例）。        |
| **Broadcast**           | `sendInternalBroadcast(action, block)`                   | 发送一个安全的内部广播。                   |
| **Content<br>Provider** | `queryPlugin(uri, ...)`                                  | 查询插件 `ContentProvider`。        |
|                         | `insertPlugin(uri, ...)`                                 | 插入数据到插件 `ContentProvider`。     |
|                         | `deletePlugin(uri, ...)`                                 | 从插件 `ContentProvider` 删除数据。    |
|                         | `updatePlugin(uri, ...)`                                 | 更新插件 `ContentProvider` 中的数据。   |
|                         | `callPlugin(uri, method, ...)`                           | 调用插件 `ContentProvider` 的自定义方法。 |
|                         | `registerPluginObserver(uri, ..., observer)`             | 为插件 `ContentProvider` 注册内容观察者。 |
|                         | `unregisterPluginObserver(observer)`                     | 注销内容观察者。                       |

</details>