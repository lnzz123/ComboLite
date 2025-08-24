# 四大组件指南

欢迎阅读 `ComboLite` 的四大组件使用指南！

`ComboLite` 通过强大的**代理模式**，实现了对 Android 四大组件的全面支持。本指南将逐个组件地为你详细介绍
**宿主端配置**、**插件端实现**以及**最终如何调用**的全流程。

### ⚠️ 核心概念：理解代理上下文 (Proxy Context)

在开始之前，请务必理解这个最重要的概念：

在 `ComboLite` 中，插件的 `Activity` 和 `Service` **不是**标准的原生组件。它们是实现了特定接口（
`IPluginActivity`, `IPluginService`）的普通 Kotlin/Java 对象。

这意味着，在你的插件 `Activity` 或 `Service` 内部，你**无法**直接调用 `this.finish()` 或
`this.startService()` 等标准的 `Context` 方法，因为 `this` 指向的是插件对象本身，而不是一个真正的
`Context`。

**如何解决？**
框架通过 `onAttach` 方法，会将一个**宿主端的代理组件**（一个真实的 `ComponentActivity` 或 `Service`
实例）注入到你的插件基类 (`BasePluginActivity`, `BasePluginService`) 中，你可以通过 `proxyActivity` 或
`proxyService` 属性来访问它。

**你需要通过这个代理对象来访问所有标准的 `Context` 功能和 `Activity`/`Service` 的方法。**

| 错误用法                | 正确用法                               |
|:--------------------|:-----------------------------------|
| `finish()`          | `proxyActivity?.finish()`          |
| `getIntent()`       | `proxyActivity?.intent`            |
| `startService(...)` | `proxyActivity?.startService(...)` |
| `stopSelf()`        | `proxyService?.stopSelf()`         |

-----

## 目录

* [**Activity**](#1-activity)
* [**Service**](#2-service)
* [**BroadcastReceiver**](#3-broadcastreceiver)
* [**ContentProvider**](#4-contentprovider)

-----

## 1. Activity

`ComboLite` 允许你在插件中定义和启动 `Activity`。

### ① 宿主端配置 (Host Setup)

宿主需要提供一个“占坑”的代理 `Activity`，并将其注册到 `PluginManager`。

**步骤 1: 创建一个空的代理 Activity**
在你的**宿主** `:app` 模块中，创建一个空的 `Activity`，让它继承自 `BaseHostActivity`。

```kotlin
// in :app/src/main/java/.../HostActivity.kt
package com.your.host.app

import com.combo.core.base.BaseHostActivity

// 无需任何额外逻辑，一个空的类即可
class HostActivity : BaseHostActivity()
```

**步骤 2: 在宿主 `AndroidManifest.xml` 中注册**
将这个 `HostActivity` 注册到**宿主**的 `AndroidManifest.xml` 文件中。

```xml
<application>
    
    <activity
        android:name=".HostActivity"
        android:exported="false"
        android:theme="@android:style/Theme.Translucent.NoTitleBar" />
        
</application>
```

**步骤 3: 告知 `PluginManager`**
在你的 `Application` 类的 `onCreate` 方法中，将这个代理 `Activity` 的 `Class` 对象告知
`PluginManager`。

```kotlin
// in :app/src/main/java/.../MainApplication.kt
class MainApplication : BaseHostApplication() {
    override fun onCreate() {
        super.onCreate()

        // 配置 Activity 代理宿主
        PluginManager.proxyManager.setHostActivity(HostActivity::class.java)
    }
}
```

**宿主配置完成！**

### ② 插件端实现 (Plugin Implementation)

**步骤 1: 继承 `BasePluginActivity`**
在你的**插件**模块中，创建你的 `Activity` 类。它需要继承 `BasePluginActivity`，这个基类为你处理好了
`proxyActivity` 的注入和管理。

```kotlin
// in :your-plugin/src/main/java/.../PluginHomeActivity.kt
package com.example.myplugin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.combo.core.base.BasePluginActivity

class PluginHomeActivity : BasePluginActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // [正确用法] 通过 proxyActivity 访问 intent
        val userId = proxyActivity?.intent?.getStringExtra("userId")

        proxyActivity?.setContent {
            Text("Hello from Plugin Activity! User ID: $userId")
            
            Button(onClick = {
                // [正确用法] 通过 proxyActivity 调用 finish()
                proxyActivity?.finish()
            }) {
                Text("关闭插件页面")
            }
        }
    }
}
```

> **再次强调**：`PluginHomeActivity` 本身不是 `Activity`。所有 `Activity` 相关操作，如获取 `intent`、
`finish()`、请求权限、获取 `window` 等，都必须通过 `proxyActivity?` 来调用。

### ③ 调用与交互 (Usage & Interaction)

使用 `Context` 的 `startPluginActivity` 扩展函数启动你的插件 `Activity`。

```kotlin
// 从宿主 Activity 或其他任何地方调用
context.startPluginActivity(PluginHomeActivity::class.java) {
    // 在 Lambda 表达式中配置 Intent
    putExtra("userId", "12345")
}
```

### ④ UI 实现技术选型：为什么首选 Jetpack Compose

> 这一部分，专门用于解答在插件 Activity 中构建界面时的技术选型问题。

`ComboLite` 是一个为 **Jetpack Compose 设计的开源插件化框架**，我们强烈推荐您将 **Compose 作为插件 UI
的第一选择**。

虽然框架目前也兼容加载传统的 XML 布局，但这应被视为一种处理历史遗留代码或特殊场景（如 `WebView` 等
Compose 尚未完美支持的控件）的**备用方案**。我们不推荐在新的插件功能中继续使用 XML，原因如下：

* **缺陷一：主题与基类强耦合**

  许多来自 `com.google.android.material` 等三方库的 XML 控件，强制要求其所在的 `Activity`
  必须继承自特定的基类（如 `FragmentActivity`）或使用特定的主题（如 `Theme.MaterialComponents`）。

  在 `ComboLite` 的代理 `Activity` 机制下，插件 `Activity` 无法改变宿主代理 `Activity`
  的基类和主题。如果强行在插件中使用这类控件，将在运行时抛出 `InflateException` 导致应用崩溃。

  > **崩溃日志示例:**

  > ```text
    > FATAL EXCEPTION: main
    > Process: com.combo.plugin.sample, PID: 30083 
    > java.lang.RuntimeException: Unable to start activity ComponentInfo{...}: android.view.InflateException: Binary XML file line #9 in com.combo.plugin.sample.example:layout/activity_xml: Error inflating class com.google.android.material.appbar.AppBarLayout 
    >   at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:4409)
    >   ...
    > ```

* **缺陷二：跨模块资源引用困难**

  在最小化打包模式下，插件模块使用 `compileOnly` 依赖其他库。在这种情况下，XML 布局无法像 Compose
  一样，在开发时通过 `R.drawable.xxx` 的方式直接引用 `compileOnly` 依赖中的资源。如果你在 XML 中使用
  `@drawable/ic_arrow` 来引用一个外部模块的资源，AGP 在编译当前模块时会因为找不到该资源而直接报错。

  > **编译失败日志示例:**

  > ```text
    > Execution failed for task ':sample-plugin:example:verifyReleaseResources'.
    > > A failure occurred while executing com.android.build.gradle.tasks.VerifyLibraryResourcesTask$Action
    >    > Android resource linking failed
    >      ERROR: /.../layout/activity_xml.xml:20: AAPT: error: resource drawable/ic_arrow (aka com.combo.plugin.sample.example:drawable/ic_arrow) not found.
    > ```

  **如何解决这个问题？**
  唯一的解决方案是，采取一系列“破坏最小化原则”的操作：

    1. 在插件的 `build.gradle.kts` 中，将被引用的资源所在的库从 `compileOnly` 改为 `implementation`。
    2. 在根 `build.gradle.kts` 的 `aar2apk` 配置中，为该插件模块开启资源打包：
       ```kotlin
       module(":sample-plugin:example") {
           includeDependenciesRes.set(true)
       }
       ```

  这不仅增加了配置的复杂性，也增大了插件的体积。

  **总结**: 除非有绝对必要，请在插件中坚持使用 Jetpack Compose。它不仅是现代 Android UI
  的未来，也从根本上规避了上述所有与插件化框架相关的棘手问题。

-----

## 2. Service

`Service` 的代理模式与 `Activity` 类似，你也需要通过 `proxyService` 属性来与真实的 `Service` 上下文交互。

### ① 宿主端配置 (Host Setup)

**步骤 1: 创建多个空的代理 Service**
在你的**宿主** `:app` 模块中，创建几个空的 `Service` 类，它们都继承自 `BaseHostService`。

```kotlin
// in :app/src/main/java/.../services/
class HostService1 : BaseHostService()
class HostService2 : BaseHostService()
// ... 依此类推
```

**步骤 2: 在宿主 `AndroidManifest.xml` 中注册它们**
将所有代理 `Service` 都在**宿主**的 `AndroidManifest.xml` 中进行注册。

```xml
<application>
    
    <service android:name=".services.HostService1" android:enabled="true" android:exported="false" />
    <service android:name=".services.HostService2" android:enabled="true" android:exported="false" />
    </application>
```

**步骤 3: 将代理池告知 `PluginManager`**
在你的 `Application` 类的 `onCreate` 方法中，将这个代理 `Service` 列表配置成一个“池”。

```kotlin
// in :app/src/main/java/.../MainApplication.kt
class MainApplication : BaseHostApplication() {
    override fun onCreate() {
        super.onCreate()
        
        // 配置 Service 代理池
        val servicePool = listOf(
            HostService1::class.java,
            HostService2::class.java,
            // ...
        )
        PluginManager.proxyManager.setServicePool(servicePool)
    }
}
```

**宿主配置完成！**

### ② 插件端实现 (Plugin Implementation)

**步骤 1: 继承 `BasePluginService`**
在你的**插件**模块中，创建你的 `Service` 类，并继承 `BasePluginService`。

```kotlin
// in :your-plugin/src/main/java/.../MusicPlayerService.kt
package com.example.myplugin

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import com.combo.core.base.BasePluginService

class MusicPlayerService : BasePluginService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val songUrl = intent?.getStringExtra("SONG_URL")
        
        // 假设这里创建了一个通知
        val notification: Notification = createMusicNotification()

        // [正确用法] 通过 proxyService 调用 startForeground()
        proxyService?.startForeground(1, notification)
        
        // ... 开始播放音乐 ...

        // 如果任务完成，可以调用 stopSelf()
        // [正确用法] 通过 proxyService 调用 stopSelf()
        // proxyService?.stopSelf()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
```

**同样地**，所有 `Service` 相关操作，如 `startForeground()`、`stopSelf()`、获取 `applicationContext`
等，都必须通过 `proxyService?` 来调用。

### ③ 调用与交互 (Usage & Interaction)

使用 `Context` 的 `startPluginService`, `bindPluginService`, `stopPluginService` 扩展函数进行交互。

**示例：启动一个 Service**

```kotlin
context.startPluginService(MusicPlayerService::class.java) {
    putExtra("SONG_URL", "http://...")
}
```

-----

## 3. BroadcastReceiver

`ComboLite` 同时支持**动态广播**和**静态广播**。

### 动态广播 (Dynamic BroadcastReceiver)

动态广播的使用体验与原生开发**完全一致**，无需任何额外配置。

**插件端实现**:

```kotlin
// MyDynamicReceiver.kt in plugin
class MyDynamicReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { /* ... */ }
}
```

**调用与交互**:

```kotlin
// 在 Composable 或 Activity 中注册和注销
val receiver = MyDynamicReceiver()
val filter = IntentFilter("MY_CUSTOM_ACTION")
context.registerReceiver(receiver, filter)
// ...
context.unregisterReceiver(receiver)
```

### 静态广播 (Static BroadcastReceiver)

静态广播允许插件响应系统级的事件（如开机、网络变化），即使应用处于后台。

### ① 宿主端配置 (Host Setup)

**步骤 1: 创建一个总代理 Receiver**
在**宿主** `:app` 模块中，创建一个空的 `BroadcastReceiver`，继承自 `BaseHostReceiver`。

```kotlin
// in :app/src/main/java/.../HostReceiver.kt
import com.combo.core.base.BaseHostReceiver

// 无需任何额外逻辑
class HostReceiver : BaseHostReceiver()
```

**步骤 2: 在宿主 `AndroidManifest.xml` 中注册**
在**宿主**的 `AndroidManifest.xml` 中注册这个总代理，并为它配置一个**包含了所有插件可能感兴趣的
Action** 的 `<intent-filter>`。

```xml
<application>
    
    <receiver
        android:name=".HostReceiver"
        android:enabled="true"
        android:exported="true">  <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
            <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
            
            <action android:name="com.yourhost.app.CUSTOM_ACTION" />
        </intent-filter>
    </receiver>
        
</application>
```

**宿主配置完成！**

### ② 插件端实现 (Plugin Implementation)

**步骤 1: 实现 `IPluginReceiver` 接口**
在**插件**模块中，创建你的静态广播接收器。它需要实现 `IPluginReceiver` 接口。

```kotlin
// in :your-plugin/src/main/java/.../BootCompletedReceiver.kt
import com.combo.core.interfaces.IPluginReceiver

class BootCompletedReceiver : IPluginReceiver {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            // ... 执行开机启动任务 ...
        }
    }
}
```

**步骤 2: 在插件的 `AndroidManifest.xml` 中声明**
和原生应用一样，在**插件**的 `AndroidManifest.xml` 中声明你的 `Receiver` 和它感兴趣的
`<intent-filter>`。框架会在插件安装时自动解析并注册它们。

```xml
<application>
    <receiver
        android:name=".BootCompletedReceiver"
        android:enabled="true"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
        </intent-filter>
    </receiver>
</application>
```

### ③ 调用与交互 (Usage & Interaction)

使用 `Context` 的 `sendInternalBroadcast` 扩展函数来发送应用内广播。

```kotlin
// 发送一个自定义的内部广播
context.sendInternalBroadcast("com.yourhost.app.CUSTOM_ACTION") {
    putExtra("data", "some value")
}
```

-----

## 4. ContentProvider

`ComboLite` 通过代理模式，让插件之间或宿主与插件之间的数据共享变得安全而透明。

### ① 宿主端配置 (Host Setup)

**步骤 1: 创建一个总代理 Provider**
在**宿主** `:app` 模块中，创建一个空的 `ContentProvider`，继承自 `BaseHostProvider`。

```kotlin
// in :app/src/main/java/.../HostProvider.kt
import com.combo.core.base.BaseHostProvider

// 无需任何额外逻辑
class HostProvider : BaseHostProvider()
```

**步骤 2: 在宿主 `AndroidManifest.xml` 中注册**
在**宿主**的 `AndroidManifest.xml` 中注册这个总代理，并为其指定一个**全局唯一的 `Authority`**。

```xml
<application>
    
    <provider
        android:name=".HostProvider"
        android:authorities="com.your.host.app.provider.proxy"
        android:exported="true" />
        
</application>
```

**步骤 3: 将 Authority 告知 `PluginManager`**
在你的 `Application` 类的 `onCreate` 方法中，配置这个 `Authority`。

```kotlin
// in :app/src/main/java/.../MainApplication.kt
class MainApplication : BaseHostApplication() {
    override fun onCreate() {
        super.onCreate()
        
        // 配置 ContentProvider 代理的 Authority
        PluginManager.proxyManager.setHostProviderAuthority("com.your.host.app.provider.proxy")
    }
}
```

**宿主配置完成！**

### ② 插件端实现 (Plugin Implementation)

插件端 `ContentProvider` 的实现与原生开发**完全一致**。

**步骤 1: 创建 Provider 类**
在**插件**模块中，创建一个标准的 `ContentProvider`。

```kotlin
// in :your-plugin/src/main/java/.../BookProvider.kt
class BookProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.example.myplugin.book.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/books")
    }

    // ... 实现 onCreate, query, insert, delete, update 等标准方法 ...
}
```

**步骤 2: 在插件的 `AndroidManifest.xml` 中声明**
在**插件**的 `AndroidManifest.xml` 中注册你的 `Provider`。

```xml
<application>
    <provider
        android:name=".BookProvider"
        android:authorities="com.example.myplugin.book.provider"
        android:exported="true" />
</application>
```

### ③ 调用与交互 (Usage & Interaction)

使用 `ContentResolver` 的 `xxxPlugin` 系列扩展函数进行交互。框架会自动处理 `Uri` 的代理转换。

```kotlin
// 使用插件中定义的原始 URI 进行查询
val cursor = contentResolver.queryPlugin(
    uri = BookProvider.CONTENT_URI,
    projection = null,
    selection = null,
    selectionArgs = null,
    sortOrder = null
)
// ...
```