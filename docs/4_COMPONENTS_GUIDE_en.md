Of course. Here is the complete English translation of your `4_COMPONENTS_GUIDE.md` document.

-----

# Four Components Guide

Welcome to the `ComboLite` Four Components Usage Guide!

`ComboLite` provides comprehensive support for Android's four major components (Activity, Service,
BroadcastReceiver, ContentProvider) through a powerful **proxy pattern**. This guide will detail the
entire process for each component, covering **host-side setup**, **plugin-side implementation**, and
**final invocation**.

### ⚠️ Core Concept: Understanding the Proxy Context

Before you begin, it is crucial to understand this most important concept:

In `ComboLite`, plugin `Activities` and `Services` are **not** standard native components. They are
regular Kotlin/Java objects that implement specific interfaces (`IPluginActivity`,
`IPluginService`).

This means that within your plugin `Activity` or `Service`, you **cannot** directly call standard
`Context` methods like `this.finish()` or `this.startService()`, because `this` refers to the plugin
object itself, not a true `Context`.

**How to solve this?**
The framework, through the `onAttach` method, injects a **host-side proxy component** (a real
`ComponentActivity` or `Service` instance) into your plugin's base class (`BasePluginActivity`,
`BasePluginService`). You can access it via the `proxyActivity` or `proxyService` property.

**You must use this proxy object to access all standard `Context` functionalities
and `Activity`/`Service` methods.**

| Incorrect Usage     | Correct Usage                      |
|:--------------------|:-----------------------------------|
| `finish()`          | `proxyActivity?.finish()`          |
| `getIntent()`       | `proxyActivity?.intent`            |
| `startService(...)` | `proxyActivity?.startService(...)` |
| `stopSelf()`        | `proxyService?.stopSelf()`         |

-----

## Table of Contents

* [**Activity**](#1-activity)
* [**Service**](#2-service)
* [**BroadcastReceiver**](#3-broadcastreceiver)
* [**ContentProvider**](#4-contentprovider)

-----

## 1. Activity

`ComboLite` allows you to define and launch `Activities` within your plugins.

### ① Host-Side Setup

The host needs to provide a "placeholder" proxy `Activity` and register it with the `PluginManager`.

**Step 1: Create an empty proxy Activity**
In your **host** `:app` module, create an empty `Activity` that inherits from `BaseHostActivity`.

```kotlin
// in :app/src/main/java/.../HostActivity.kt
package com.your.host.app

import com.combo.core.base.BaseHostActivity

// No additional logic is needed, an empty class is sufficient
class HostActivity : BaseHostActivity()
```

**Step 2: Register it in the host's `AndroidManifest.xml`**
Register this `HostActivity` in the **host's** `AndroidManifest.xml` file.

```xml

<application>

    <activity android:name=".HostActivity" android:exported="false"
        android:theme="@android:style/Theme.Translucent.NoTitleBar" />

</application>
```

**Step 3: Inform the `PluginManager`**
In your `Application` class's `onCreate` method, provide the `Class` object of this proxy `Activity`
to the `PluginManager`.

```kotlin
// in :app/src/main/java/.../MainApplication.kt
class MainApplication : BaseHostApplication() {
    override fun onCreate() {
        super.onCreate()

        // Configure the Activity proxy host
        PluginManager.proxyManager.setHostActivity(HostActivity::class.java)
    }
}
```

**Host setup is complete!**

### ② Plugin-Side Implementation

**Step 1: Inherit from `BasePluginActivity`**
In your **plugin** module, create your `Activity` class. It needs to inherit from
`BasePluginActivity`, which handles the injection and management of `proxyActivity` for you.

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

        // [Correct Usage] Access the intent via proxyActivity
        val userId = proxyActivity?.intent?.getStringExtra("userId")

        proxyActivity?.setContent {
            Text("Hello from Plugin Activity! User ID: $userId")

            Button(onClick = {
                // [Correct Usage] Call finish() via proxyActivity
                proxyActivity?.finish()
            }) {
                Text("Close Plugin Page")
            }
        }
    }
}
```

> **To reiterate**: `PluginHomeActivity` itself is not an `Activity`. All `Activity`-related
> operations, such as getting the `intent`, calling `finish()`, requesting permissions, getting the
`window`, etc., must be called through `proxyActivity?`.

### ③ Usage & Interaction

Use the `startPluginActivity` extension function of `Context` to launch your plugin `Activity` from
anywhere (host or other plugins).

```kotlin
// Call from a host Activity or anywhere else
context.startPluginActivity(PluginHomeActivity::class.java) {
    // Configure the Intent in the Lambda expression
    putExtra("userId", "12345")
}
```

### ④ UI Implementation Technology Choice: Why Prioritize Jetpack Compose

> This section is dedicated to answering technology selection questions when building the UI in a
> plugin Activity.

`ComboLite` is an **open-source plugin framework designed for Jetpack Compose**, and we highly
recommend you make **Compose your first choice for plugin UI**.

While the framework is currently compatible with loading traditional XML layouts, this should be
considered a **fallback option** for handling legacy code or special cases (like `WebView`, which
Compose does not yet perfectly support). We do not recommend using XML for new plugin features for
the following reasons:

* **Issue 1: Strong Coupling of Themes and Base Classes**

  Many XML widgets from third-party libraries like `com.google.android.material` require the
  `Activity` they are in to inherit from a specific base class (e.g., `FragmentActivity`) or use a
  specific theme (e.g., `Theme.MaterialComponents`).

  Under `ComboLite`'s proxy `Activity` mechanism, a plugin `Activity` cannot change the base class
  or theme of the host's proxy `Activity`. Forcing the use of such widgets in a plugin will cause an
  `InflateException` at runtime, crashing the application.

  > **Example Crash Log:**

  > ```text
    > FATAL EXCEPTION: main
    > Process: com.combo.plugin.sample, PID: 30083 
    > java.lang.RuntimeException: Unable to start activity ComponentInfo{...}: android.view.InflateException: Binary XML file line #9 in com.combo.plugin.sample.example:layout/activity_xml: Error inflating class com.google.android.material.appbar.AppBarLayout 
    >   at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:4409)
    >   ...
    > ```

* **Issue 2: Difficulty with Cross-Module Resource References**

  In the minimal packaging mode, plugin modules use `compileOnly` to depend on other libraries. In
  this scenario, XML layouts cannot directly reference resources from `compileOnly` dependencies at
  development time using `R.drawable.xxx` like Compose can. If you use `@drawable/ic_arrow` in XML
  to reference a resource from an external module, AGP will fail the build because it cannot find
  the resource during the compilation of the current module.

  > **Example Build Failure Log:**

  > ```text
    > Execution failed for task ':sample-plugin:example:verifyReleaseResources'.
    > > A failure occurred while executing com.android.build.gradle.tasks.VerifyLibraryResourcesTask$Action
    >    > Android resource linking failed
    >      ERROR: /.../layout/activity_xml.xml:20: AAPT: error: resource drawable/ic_arrow (aka com.combo.plugin.sample.example:drawable/ic_arrow) not found.
    > ```

  **How to solve this problem?**
  The only solution is to take a series of actions that "break the principle of minimization":

    1. In the plugin's `build.gradle.kts`, change the dependency containing the referenced resource
       from `compileOnly` to `implementation`.
    2. In the root `build.gradle.kts`'s `aar2apk` configuration, enable resource packaging for that
       plugin module:
       ```kotlin
       module(":sample-plugin:example") {
           includeDependenciesRes.set(true)
       }
       ```

  This not only increases configuration complexity but also enlarges the plugin's size.

  **Conclusion**: Unless absolutely necessary, please stick to using Jetpack Compose in your
  plugins. It is not only the future of modern Android UI but also fundamentally bypasses all the
  tricky issues mentioned above related to plugin frameworks.

-----

## 2. Service

The proxy model for `Service` is similar to `Activity`; you also need to use the `proxyService`
property to interact with the real `Service` context.

### ① Host-Side Setup

**Step 1: Create multiple empty proxy Services**
In your **host** `:app` module, create several empty `Service` classes that all inherit from
`BaseHostService`.

```kotlin
// in :app/src/main/java/.../services/
class HostService1 : BaseHostService()
class HostService2 : BaseHostService()
// ... and so on
```

**Step 2: Register them in the host's `AndroidManifest.xml`**
Register all proxy `Service` classes in the **host's** `AndroidManifest.xml`.

```xml

<application>

    <service android:name=".services.HostService1" android:enabled="true"
        android:exported="false" />
    <service android:name=".services.HostService2" android:enabled="true"
        android:exported="false" />
</application>
```

**Step 3: Provide the proxy pool to `PluginManager`**
In your `Application` class's `onCreate` method, configure this list of proxy `Service` classes as
a "pool".

```kotlin
// in :app/src/main/java/.../MainApplication.kt
class MainApplication : BaseHostApplication() {
    override fun onCreate() {
        super.onCreate()

        // Configure the Service proxy pool
        val servicePool = listOf(
            HostService1::class.java,
            HostService2::class.java,
            // ...
        )
        PluginManager.proxyManager.setServicePool(servicePool)
    }
}
```

**Host setup is complete!**

### ② Plugin-Side Implementation

**Step 1: Inherit from `BasePluginService`**
In your **plugin** module, create your `Service` class and inherit from `BasePluginService`.

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

        // Assume a notification is created here
        val notification: Notification = createMusicNotification()

        // [Correct Usage] Call startForeground() via proxyService
        proxyService?.startForeground(1, notification)

        // ... start playing music ...

        // If the task is done, you can call stopSelf()
        // [Correct Usage] Call stopSelf() via proxyService
        // proxyService?.stopSelf()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
```

**Likewise**, all `Service`-related operations, such as `startForeground()`, `stopSelf()`, getting
`applicationContext`, etc., must be called through `proxyService?`.

### ③ Usage & Interaction

Use the `startPluginService`, `bindPluginService`, and `stopPluginService` extension functions of
`Context` for interaction.

**Example: Starting a Service**

```kotlin
context.startPluginService(MusicPlayerService::class.java) {
    putExtra("SONG_URL", "http://...")
}
```

-----

## 3. BroadcastReceiver

`ComboLite` supports both **dynamic** and **static** broadcasts.

### Dynamic BroadcastReceiver

The usage of dynamic broadcasts is **exactly the same as in native development**, with no extra
configuration needed.

**Plugin-Side Implementation**:

```kotlin
// MyDynamicReceiver.kt in plugin
class MyDynamicReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { /* ... */
    }
}
```

**Usage & Interaction**:

```kotlin
// Register and unregister in a Composable or Activity
val receiver = MyDynamicReceiver()
val filter = IntentFilter("MY_CUSTOM_ACTION")
context.registerReceiver(receiver, filter)
// ...
context.unregisterReceiver(receiver)
```

### Static BroadcastReceiver

Static broadcasts allow plugins to respond to system-level events (like boot completion, network
changes), even when the app is in the background.

### ① Host-Side Setup

**Step 1: Create a master proxy Receiver**
In the **host** `:app` module, create an empty `BroadcastReceiver` that inherits from
`BaseHostReceiver`.

```kotlin
// in :app/src/main/java/.../HostReceiver.kt
import com.combo.core.base.BaseHostReceiver

// No additional logic needed
class HostReceiver : BaseHostReceiver()
```

**Step 2: Register it in the host's `AndroidManifest.xml`**
Register this master proxy in the **host's** `AndroidManifest.xml` and configure an
`<intent-filter>` for it that **includes all Actions that any plugin might be interested in**.

```xml

<application>

    <receiver android:name=".HostReceiver" android:enabled="true" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
            <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />

            <action android:name="com.yourhost.app.CUSTOM_ACTION" />
        </intent-filter>
    </receiver>

</application>
```

**Host setup is complete!**

### ② Plugin-Side Implementation

**Step 1: Implement the `IPluginReceiver` interface**
In the **plugin** module, create your static broadcast receiver. It needs to implement the
`IPluginReceiver` interface.

```kotlin
// in :your-plugin/src/main/java/.../BootCompletedReceiver.kt
import com.combo.core.interfaces.IPluginReceiver

class BootCompletedReceiver : IPluginReceiver {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            // ... perform boot-up tasks ...
        }
    }
}
```

**Step 2: Declare it in the plugin's `AndroidManifest.xml`**
Just like in a native app, declare your `Receiver` and its `<intent-filter>` in the **plugin's**
`AndroidManifest.xml`. The framework will automatically parse and register them upon plugin
installation.

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

### ③ Usage & Interaction

Use the `sendInternalBroadcast` extension function of `Context` to send in-app broadcasts.

```kotlin
// Send a custom internal broadcast
context.sendInternalBroadcast("com.yourhost.app.CUSTOM_ACTION") {
    putExtra("data", "some value")
}
```

-----

## 4. ContentProvider

`ComboLite`, through its proxy pattern, makes data sharing between plugins or between the host and
plugins safe and transparent.

### ① Host-Side Setup

**Step 1: Create a master proxy Provider**
In the **host** `:app` module, create an empty `ContentProvider` that inherits from
`BaseHostProvider`.

```kotlin
// in :app/src/main/java/.../HostProvider.kt
import com.combo.core.base.BaseHostProvider

// No additional logic needed
class HostProvider : BaseHostProvider()
```

**Step 2: Register it in the host's `AndroidManifest.xml`**
Register this master proxy in the **host's** `AndroidManifest.xml` and assign it a **globally
unique `Authority`**.

```xml
<application>
    
    <provider
        android:name=".HostProvider"
        android:authorities="com.your.host.app.provider.proxy"
        android:exported="true" />
        
</application>
```

**Step 3: Provide the Authority to `PluginManager`**
In your `Application` class's `onCreate` method, configure this `Authority`.

```kotlin
// in :app/src/main/java/.../MainApplication.kt
class MainApplication : BaseHostApplication() {
    override fun onCreate() {
        super.onCreate()

        // Configure the ContentProvider proxy's Authority
        PluginManager.proxyManager.setHostProviderAuthority("com.your.host.app.provider.proxy")
    }
}
```

**Host setup is complete!**

### ② Plugin-Side Implementation

The implementation of a `ContentProvider` on the plugin side is **identical to native development**.

**Step 1: Create the Provider class**
In the **plugin** module, create a standard `ContentProvider`.

```kotlin
// in :your-plugin/src/main/java/.../BookProvider.kt
class BookProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.example.myplugin.book.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/books")
    }

    // ... implement onCreate, query, insert, delete, update, etc. ...
}
```

**Step 2: Declare it in the plugin's `AndroidManifest.xml`**
Register your `Provider` in the **plugin's** `AndroidManifest.xml`.

```xml
<application>
    <provider
        android:name=".BookProvider"
        android:authorities="com.example.myplugin.book.provider"
        android:exported="true" />
</application>
```

### ③ Usage & Interaction

Use the `xxxPlugin` series of extension functions for `ContentResolver` to interact. The framework
will automatically handle the `Uri` proxy conversion.

```kotlin
// Use the original URI defined in the plugin to perform a query
val cursor = contentResolver.queryPlugin(
    uri = BookProvider.CONTENT_URI,
    projection = null,
    selection = null,
    selectionArgs = null,
    sortOrder = null
)
// ...
```