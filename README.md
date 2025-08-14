# ComposePluginFramework

<p align="center">
  <img src="https://img.shields.io/badge/Android-7%2B-brightgreen.svg" alt="Android Version"/>
  <img src="https://img.shields.io/badge/Kotlin-2.2.0-blue.svg" alt="Kotlin Version"/>
  <img src="https://img.shields.io/badge/Compose-2025.07.00-orange.svg" alt="Compose Version"/>
  <img src="https://img.shields.io/badge/License-Commercial-red.svg" alt="License"/>
</p>

<h3 align="center">一个基于 Android Compose 的现代化插件框架</h3>
<p align="center">支持完整的四大组件插件化、依赖注入和去中心化设计</p>

---

## 🚀 核心特性

### 🎯 基于 Compose 的现代化设计
- **原生 Compose 支持**：完美集成 Android Jetpack Compose，支持现代化 UI 开发
- **声明式 UI**：插件可以直接使用 Compose 构建界面，无需传统 View 系统
- **响应式状态管理**：支持 StateFlow、LiveData 等现代状态管理方案

### 🔧 完整的四大组件插件化
- **Activity 插件化**：采用单一宿主模式，通过代理 Activity 实现插件 Activity 的完整生命周期
- **Service 插件化**：采用代理池模式，支持多个插件 Service 并发运行
- **BroadcastReceiver 插件化**：支持静态和动态广播接收器的插件化
- **ContentProvider 插件化**：统一代理机制，支持插件间数据共享

### 💉 强大的依赖注入支持
- **Koin 集成**：完美支持 Koin 依赖注入框架
- **模块化管理**：每个插件可以定义自己的依赖注入模块
- **跨插件依赖**：支持插件间的依赖注入和服务共享

### 🌐 去中心化架构设计
- **插件自治**：插件本身可以管理其他插件的安装、卸载、更新
- **空壳宿主**：支持宿主 App 为纯粹的空壳，所有业务逻辑由插件提供
- **插件间通信**：完善的插件间通信机制，支持复杂的业务场景

### 📦 灵活的插件形态
- **AAR 转 APK**：支持将 Android Library (AAR) 转换为插件 APK
- **一个应用一个插件**：每个 Application 或 Library 都可以作为独立插件
- **插件间依赖**：支持插件之间的相互依赖关系

### 🛡️ 零侵入性设计
- **0 Hook 0 反射**：完全基于 Android 官方 API，无需 Hook 或反射
- **官方 ClassLoader**：使用 Android 官方推荐的 DexClassLoader
- **官方 ResourceLoader**：基于 Android 11+ ResourcesLoader API 和兼容性方案
- **最小侵入**：集成框架只需极少的代码修改

### 🔄 广泛的兼容性
- **Android 7-16**：理论支持 Android 7.0 到 Android 16 的所有版本
- **现代工具链**：基于最新版 Android Studio、Gradle、AGP 开发
- **主流技术栈**：完美支持 Kotlin、Coroutines、Compose 等现代技术

### 🪶 轻量级架构
- **最小依赖**：除必要的 Koin 和 dexlib2 外，几乎无第三方库依赖
- **核心精简**：插件核心模块仅十几个核心类
- **高性能**：O(1) 时间复杂度的类查找机制

## 🏗️ 架构设计

### 核心组件

```
ComposePluginFramework
├── PluginManager          # 插件管理器（单例）
├── InstallerManager       # 插件安装管理
├── PluginResourcesManager # 插件资源管理
├── ProxyManager           # 四大组件代理管理
├── PluginClassLoader      # 插件类加载器
└── IPluginEntryClass      # 插件入口接口
```

### 插件生命周期

1. **安装阶段**：签名验证 → Manifest 解析 → 文件复制 → 信息记录
2. **加载阶段**：类索引建立 → 资源加载 → 依赖注入模块注册
3. **运行阶段**：实例化插件 → 启动 Compose UI → 生命周期管理
4. **卸载阶段**：资源清理 → 模块卸载 → 文件删除

### 资源管理策略

- **Android 11+**：使用 ResourcesLoader API 进行资源加载
- **Android 11-**：使用 AssetManager.addAssetPath 反射 API 兼容
- **统一管理**：PluginResourcesManager 提供统一的资源访问接口

## 🚀 快速开始

### 1. 集成插件框架

在宿主应用的 `build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation(project(":plugin-framework:plugin-core"))
}
```

### 2. 初始化框架

在 Application 中初始化插件管理器：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化插件管理器
        PluginManager.init(this)
        
        // 启动已安装的插件
        lifecycleScope.launch {
            PluginManager.launchAllPlugins()
        }
    }
}
```

### 3. 创建插件

#### 3.1 定义插件入口类

```kotlin
class PluginEntryClass : IPluginEntryClass {
    override val pluginModule: List<Module>
        get() = listOf(
            module {
                viewModel { MyViewModel() }
                single<IMyService> { MyServiceImpl() }
            }
        )

    @Composable
    override fun Content() {
        MyPluginScreen()
    }
}
```

#### 3.2 配置插件 Manifest

在插件的 `AndroidManifest.xml` 中添加元数据：

```xml
<application>
    <meta-data
        android:name="plugin.id"
        android:value="my_plugin" />
    <meta-data
        android:name="plugin.version"
        android:value="1.0.0" />
    <meta-data
        android:name="plugin.description"
        android:value="我的示例插件" />
    <meta-data
        android:name="plugin.entryClass"
        android:value="com.example.plugin.PluginEntryClass" />
</application>
```

### 4. 插件管理

```kotlin
// 安装插件
val result = PluginManager.installPlugin(pluginApkFile)
if (result is InstallResult.Success) {
    // 安装成功，启动插件
    PluginManager.launchPlugin(result.pluginInfo.pluginId)
}

// 获取插件实例
val pluginInstance = PluginManager.getPluginInstance("my_plugin")

// 跨插件获取服务
val service = PluginManager.getInterface(
    IMyService::class.java,
    "com.example.plugin.MyServiceImpl"
)
```

## 🔧 高级特性

### 插件间通信

```kotlin
// 在插件A中定义接口
interface IPluginAService {
    fun doSomething(): String
}

// 在插件B中获取插件A的服务
val serviceA = PluginManager.getInterface(
    IPluginAService::class.java,
    "com.example.plugina.ServiceImpl"
)
val result = serviceA?.doSomething()
```

### 四大组件插件化

#### Service 插件化

```kotlin
// 插件Service实现
class MyPluginService : BasePluginService() {
    override fun onCreate() {
        super.onCreate()
        // 初始化逻辑
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理启动命令
        return START_STICKY
    }
}
```

#### BroadcastReceiver 插件化

```kotlin
// 插件广播接收器
class MyPluginReceiver : IPluginReceiver {
    override fun onReceive(context: Context, intent: Intent) {
        // 处理广播
    }
}
```

### 资源访问

```kotlin
// 获取插件资源
val pluginResources = PluginManager.resourcesManager.getPluginResources("my_plugin")
val drawable = pluginResources?.getDrawable(R.drawable.my_icon)
```

## 📊 性能优势

| 特性 | 传统插件框架 | ComposePluginFramework |
|------|-------------|------------------------|
| 类查找复杂度 | O(n) | O(1) |
| Hook/反射 | 大量使用 | 完全不使用 |
| 资源加载 | 复杂兼容 | 官方API + 兼容方案 |
| UI框架 | View系统 | 现代Compose |
| 依赖注入 | 自实现/无 | 成熟Koin框架 |
| 插件通信 | 复杂实现 | 简洁接口 |

## 🆚 与其他框架对比

### vs VirtualAPK/RePlugin/Atlas

- ✅ **现代化技术栈**：基于 Compose 而非传统 View 系统
- ✅ **零 Hook 零反射**：完全基于官方 API，稳定性更高
- ✅ **去中心化设计**：插件可以管理其他插件
- ✅ **完整依赖注入**：原生支持 Koin 框架
- ✅ **插件间依赖**：支持复杂的插件依赖关系

### vs Small/QIGSAW

- ✅ **更轻量级**：核心代码更少，依赖更简洁
- ✅ **更好兼容性**：支持最新 Android 版本和工具链
- ✅ **更强扩展性**：插件可以是完整的应用
- ✅ **更简单集成**：最小侵入性设计

## 🛠️ 技术实现

### 类加载机制

```kotlin
// 全局类索引，O(1)查找
private val classIndex = ConcurrentHashMap<String, String>()

// 插件类加载器
class PluginClassLoader(
    pluginFile: File,
    parent: ClassLoader,
    private val pluginFinder: IPluginFinder
) : DexClassLoader(/* ... */) {
    
    override fun findClass(name: String): Class<*> {
        return try {
            super.findClass(name)
        } catch (e: ClassNotFoundException) {
            // 在其他插件中查找
            pluginFinder.findClass(name) ?: throw e
        }
    }
}
```

### 资源管理机制

```kotlin
// Android 11+ 使用官方 ResourcesLoader
@RequiresApi(Build.VERSION_CODES.R)
private fun addResourcesForAndroid11Plus(pluginFile: File) {
    val resourcesLoader = PluginResourcesLoader.createResourcesLoader(pluginFile)
    hostResources.addLoaders(resourcesLoader)
}

// Android 11- 使用兼容方案
private fun addResourcesForLegacy(pluginFile: File) {
    val addAssetPathMethod = AssetManager::class.java
        .getDeclaredMethod("addAssetPath", String::class.java)
    addAssetPathMethod.invoke(hostResources.assets, pluginFile.absolutePath)
}
```

### 去中心化插件管理

```kotlin
// 插件可以管理其他插件
class PluginManagerService {
    suspend fun installOtherPlugin(pluginUrl: String): InstallResult {
        val pluginFile = downloadPlugin(pluginUrl)
        return PluginManager.installPlugin(pluginFile)
    }
    
    suspend fun updatePlugin(pluginId: String): Boolean {
        val latestVersion = checkLatestVersion(pluginId)
        if (latestVersion > getCurrentVersion(pluginId)) {
            return installOtherPlugin(getDownloadUrl(pluginId, latestVersion))
                is InstallResult.Success
        }
        return false
    }
}
```

## 📱 示例项目

项目包含完整的示例代码：

- **宿主应用**：`app` 模块，展示如何集成和使用插件框架
- **示例插件**：`sample-plugin` 模块，包含多个示例插件
  - `home`：主页插件，展示插件管理功能
  - `guide`：引导插件，展示基础功能
  - `example`：示例插件，展示复杂业务逻辑
  - `setting`：设置插件，展示系统配置

### 运行示例

```bash
# 克隆项目
git clone https://github.com/your-repo/ComposePluginFramework.git

# 打开 Android Studio
# 构建并运行 app 模块
# 体验插件的安装、加载和运行
```

## 🔮 未来规划

- [ ] **热更新支持**：支持插件的热更新机制
- [ ] **插件市场**：构建插件分发和管理平台
- [ ] **性能监控**：添加插件性能监控和分析
- [ ] **安全增强**：加强插件安全验证机制
- [ ] **开发工具**：提供插件开发的 IDE 插件和工具
- [ ] **多进程支持**：支持插件在独立进程中运行
- [ ] **增量更新**：支持插件的增量更新机制

## 📚 文档

- [快速开始指南](docs/quick-start.md)
- [插件开发指南](docs/plugin-development.md)
- [API 参考文档](docs/api-reference.md)
- [最佳实践](docs/best-practices.md)
- [常见问题](docs/faq.md)

## 🤝 贡献

我们欢迎社区贡献！请阅读 [贡献指南](CONTRIBUTING.md) 了解如何参与项目开发。

### 贡献方式

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

Copyright © 2025. 贵州君城网络科技有限公司 版权所有

本项目采用商业许可证，详情请联系：
- 邮箱：1755858138@qq.com
- 电话：+86-175-85074415

## 📞 联系我们

如有任何问题或建议，请通过以下方式联系我们：

- **技术支持**：1755858138@qq.com
- **商务合作**：+86-175-85074415
- **GitHub Issues**：[提交问题](https://github.com/your-repo/issues)

## 🙏 致谢

感谢以下开源项目和技术社区的支持：

- [Android Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Koin](https://insert-koin.io/)
- [dexlib2](https://github.com/JesusFreke/smali)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

<p align="center">
  <strong>ComposePluginFramework</strong> - 让 Android 插件化开发更简单、更现代、更强大！
</p>

我的是一个基于安卓Compose的插件化框架，支持依赖注入，当然也完美支持安卓四大组件插件化，我的插件化框架采用了一个Application或一个Library库就是一个单独的插件的设计理念（支持将aar转换为插件apk）以及去中心化的设计，即插件本身也能管理自己或者其他插件的下载安装卸载更新等等，支持宿主app是一个没有任何逻辑的空壳，支持插件之间相互依赖，完全基于安卓官方推荐的classloader和resource

loader进行插件的代码与资源加载，0hook0反射，理论上支持所有安卓版本（7-16），和GitHub上那些古老框架不同，我的是一个新兴框架，基于最新版AndroidStudio、最新版gradle、agp开发，支持现如今的主流技术，无需担心兼容性问题，插件框架非常轻量化，除了必要的koin（适配不能使用hilt依赖注入）和dexlib2（用于解析dex文件建立类索引）等库以外几乎没有任何第三方库引用，插件核心模块仅十几个核心类，对原项目侵入性也很小，几乎不需要改动太多代码就能轻松集成