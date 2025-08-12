/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利。
 *
 * 本软件（包括但不限于代码、文档、资源文件等）受《中华人民共和国著作权法》及相关法律法规保护。
 * 未经本公司书面授权，任何单位或个人不得：
 * 1. 以任何形式复制、传播、修改、分发本软件的全部或部分内容；
 * 2. 将本软件用于商业目的或未经授权的第三方项目；
 * 3. 删除或篡改本软件中的版权声明、商标标识及技术标识。
 *
 * 违反上述条款者，本公司将依法追究其民事及刑事责任，并有权要求赔偿因此造成的全部经济损失。
 *
 * 授权许可请联系：贵州君城网络科技有限公司法律事务部
 * 邮箱：1755858138@qq.com
 * 电话：+86-175-85074415
 */

package com.jctech.plugin.core.installer

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.util.Xml
import com.jctech.plugin.core.model.PluginInfo
import com.jctech.plugin.core.model.PluginState
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * 优化版本的插件XML管理器
 * 参考Android PackageManagerService的设计思路，提供以下优化：
 * 1. 内存缓存机制，减少频繁的文件I/O操作
 * 2. 读写锁分离，提高并发性能
 * 3. 批量操作支持，适合连续安装多个插件的场景
 * 4. 延迟写入机制，减少磁盘写入次数
 * 5. 文件完整性校验，防止数据损坏
 *
 * 注意：此类已改造为支持Koin依赖注入
 */
class XmlManager(private val context: Application) {

    companion object {
        private const val TAG = "InstallerXmlManager" // 用于日志记录的TAG
        private const val FILENAME = "plugins.xml"
        private const val BACKUP_FILENAME = "plugins.xml.bak"
        private const val TEMP_FILENAME = "plugins.xml.tmp" // 新增：用于原子写入的临时文件

        // XML标签常量
        private const val TAG_PLUGINS = "plugins"
        private const val TAG_PLUGIN = "plugin"
        private const val TAG_DESCRIPTION = "description"

        // XML属性常量
        private const val ATTR_ID = "id"
        private const val ATTR_VERSION = "version"
        private const val ATTR_ENTRY_CLASS = "entryClass"
        private const val ATTR_PATH = "path"
        private const val ATTR_STATUS = "status"
        private const val ATTR_INSTALL_TIME = "installTime"

        // 延迟写入时间（毫秒）
        private const val WRITE_DELAY_MS = 500L
    }

    private val pluginsConfigFile: File by lazy {
        File(context.filesDir, FILENAME)
    }

    private val backupConfigFile: File by lazy {
        File(context.filesDir, BACKUP_FILENAME)
    }

    private val tempConfigFile: File by lazy { // 新增：临时文件
        File(context.filesDir, TEMP_FILENAME)
    }

    // 内存缓存：使用ConcurrentHashMap提供线程安全的快速访问
    private val pluginCache = ConcurrentHashMap<String, PluginInfo>()

    // 读写锁：允许多个读操作并发执行，写操作独占
    private val rwLock = ReentrantReadWriteLock()
    private val readLock = rwLock.readLock()
    private val writeLock = rwLock.writeLock()

    // 标记缓存是否已初始化
    @Volatile
    private var cacheInitialized = false

    // 标记是否有未保存的更改
    // 使用 AtomicBoolean 确保线程安全更新，虽然有锁，但这里作为一个快速检查的标志
    private val hasUnsavedChanges = AtomicBoolean(false)

    // HandlerThread 用于后台调度写入任务
    private val handlerThread = HandlerThread("XmlWriterThread").apply { start() }
    private val writeHandler = Handler(handlerThread.looper)

    // 延迟写入任务 (使用 val，每次取消并重新 post)
    private val delayedWriteRunnable = Runnable {
        writeLock.withLock { // 直接尝试获取写锁，确保独占访问
            if (hasUnsavedChanges.get()) { // 在获取写锁后再次检查
                try {
                    Timber.tag(TAG).d("正在执行延迟写入到磁盘...")
                    writePluginsToDisk()
                    hasUnsavedChanges.set(false) // 写入成功后清除标记
                    Timber.tag(TAG).d("延迟写入成功。")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "延迟写入到磁盘时发生错误: ${e.message}")
                }
            }
        }
    }

    init {
        // 初始化时加载缓存
        initializeCache()
    }

    // 辅助函数，封装读锁操作
    private inline fun <T> read(action: () -> T): T {
        readLock.lock()
        try {
            return action()
        } finally {
            readLock.unlock()
        }
    }

    // 辅助函数，封装写锁操作
    private inline fun <T> write(action: () -> T): T {
        writeLock.lock()
        try {
            return action()
        } finally {
            writeLock.unlock()
        }
    }

    /**
     * 初始化内存缓存
     * 从磁盘加载所有插件信息到内存中
     */
    private fun initializeCache() {
        write {
            if (!cacheInitialized) {
                try {
                    val plugins = loadPluginsFromDisk()
                    pluginCache.clear()
                    plugins.forEach { plugin ->
                        pluginCache[plugin.pluginId] = plugin
                    }
                    cacheInitialized = true
                    hasUnsavedChanges.set(false)
                    Timber.tag(TAG)
                        .i("缓存已从 $FILENAME 初始化，已加载 ${pluginCache.size} 个插件。")
                } catch (e: IOException) { // 捕获更具体的 IOException
                    Timber.tag(TAG).e(
                        e,
                        "从主文件加载插件失败: ${e.message}。正在尝试从备份文件恢复。",
                    )
                    tryRestoreFromBackup()
                } catch (e: Exception) { // 捕获其他未知异常
                    Timber.tag(TAG).e(
                        e,
                        "缓存初始化过程中发生意外错误: ${e.message}。",
                    )
                    // 如果备份也失败了，就只能初始化为空缓存
                    pluginCache.clear()
                    cacheInitialized = true
                    hasUnsavedChanges.set(false)
                }
            }
        }
    }

    /**
     * 从磁盘加载插件配置
     * @param useBackup 是否使用备份文件
     * @return 插件信息列表
     * @throws IOException 如果读取或解析文件失败
     */
    private fun loadPluginsFromDisk(useBackup: Boolean = false): List<PluginInfo> {
        val targetFile = if (useBackup) backupConfigFile else pluginsConfigFile
        Timber.tag(TAG).d("正在尝试从以下位置加载插件: ${targetFile.absolutePath}")

        if (!targetFile.exists()) {
            Timber.tag(TAG).d("${targetFile.name} 不存在。返回空列表。")
            return emptyList()
        }

        val pluginList = mutableListOf<PluginInfo>()
        try {
            FileInputStream(targetFile).use { fis ->
                val parser = XmlPullParserFactory.newInstance().apply {
                    isNamespaceAware = true
                }.newPullParser()
                parser.setInput(fis, StandardCharsets.UTF_8.name())

                var eventType = parser.eventType
                var currentPlugin: PluginInfo? = null
                var lastStartTag: String? = null

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            lastStartTag = parser.name
                            if (lastStartTag == TAG_PLUGIN) {
                                currentPlugin = PluginInfo(
                                    pluginId = parser.getAttributeValue(null, ATTR_ID) ?: "",
                                    version = parser.getAttributeValue(null, ATTR_VERSION) ?: "",
                                    entryClass = parser.getAttributeValue(null, ATTR_ENTRY_CLASS) ?: "",
                                    path = parser.getAttributeValue(null, ATTR_PATH) ?: "",
                                    status = if (parser.getAttributeValue(null, ATTR_STATUS) == PluginState.Enabled.name) {
                                        PluginState.Enabled
                                    } else {
                                        PluginState.Disabled
                                    },
                                    installTime = parser.getAttributeValue(null, ATTR_INSTALL_TIME).toLongOrNull() ?: 0L,
                                    description = "",
                                )
                            }
                        }
                        XmlPullParser.TEXT -> {
                            val text = parser.text?.trim()
                            if (currentPlugin != null && !text.isNullOrEmpty()) {
                                if (lastStartTag == TAG_DESCRIPTION) {
                                    currentPlugin.description = text
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == TAG_PLUGIN && currentPlugin != null) {
                                pluginList.add(currentPlugin)
                                currentPlugin = null
                            }
                            lastStartTag = null
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            val errorMessage = "读取或解析 ${targetFile.name} 时发生错误: ${e.message}"
            Timber.tag(TAG).e(e, errorMessage)
            throw IOException(errorMessage, e)
        }
        return pluginList
    }

    /**
     * 尝试从备份文件恢复数据
     */
    private fun tryRestoreFromBackup() {
        Timber.tag(TAG)
            .w("正在尝试从备份文件恢复: ${backupConfigFile.absolutePath}")
        try {
            if (backupConfigFile.exists()) {
                val plugins = loadPluginsFromDisk(useBackup = true)
                pluginCache.clear()
                plugins.forEach { plugin ->
                    pluginCache[plugin.pluginId] = plugin
                }
                cacheInitialized = true
                hasUnsavedChanges.set(true) // 标记需要重新保存主文件
                scheduleDelayedWrite()
                Timber.tag(TAG)
                    .i("成功从备份文件恢复。已加载 ${pluginCache.size} 个插件。主文件将被重写。")
            } else {
                Timber.tag(TAG).w("备份文件不存在。无法恢复。")
                // 备份文件也不存在，初始化为空缓存
                pluginCache.clear()
                cacheInitialized = true
                hasUnsavedChanges.set(false)
            }
        } catch (e: Exception) {
            Timber.tag(TAG)
                .e(e, "从备份文件恢复失败: ${e.message}。正在初始化空缓存。")
            // 备份文件也损坏，初始化为空缓存
            pluginCache.clear()
            cacheInitialized = true
            hasUnsavedChanges.set(false)
        }
    }

    /**
     * 将插件数据写入磁盘 (原子写入)
     * @param createBackup 是否创建备份文件 (此参数在这里不直接控制原子写入，原子写入是内部逻辑)
     */
    private fun writePluginsToDisk(createBackup: Boolean = true) {
        val plugins = pluginCache.values.toList()
        Timber.tag(TAG).d("开始写入操作到磁盘。插件总数: ${plugins.size}")

        try {
            // 1. 写入到临时文件
            FileOutputStream(tempConfigFile).use { fos ->
                val writer = OutputStreamWriter(fos, StandardCharsets.UTF_8)
                val serializer = Xml.newSerializer()
                serializer.setOutput(writer)
                serializer.startDocument(StandardCharsets.UTF_8.name(), true)
                serializer.startTag(null, TAG_PLUGINS)

                plugins.forEach { plugin ->
                    serializer.startTag(null, TAG_PLUGIN)
                    serializer.attribute(null, ATTR_ID, plugin.pluginId)
                    serializer.attribute(null, ATTR_VERSION, plugin.version)
                    serializer.attribute(null, ATTR_ENTRY_CLASS, plugin.entryClass)
                    serializer.attribute(null, ATTR_PATH, plugin.path)
                    serializer.attribute(null, ATTR_STATUS, plugin.status.name)
                    serializer.attribute(null, ATTR_INSTALL_TIME, plugin.installTime.toString())
                    if (plugin.description.isNotBlank()) {
                        serializer.startTag(null, TAG_DESCRIPTION)
                        serializer.text(plugin.description)
                        serializer.endTag(null, TAG_DESCRIPTION)
                    }
                    serializer.endTag(null, TAG_PLUGIN)
                }

                serializer.endTag(null, TAG_PLUGINS)
                serializer.endDocument()
                serializer.flush()
            }
            Timber.tag(TAG)
                .d("数据已成功写入临时文件: ${tempConfigFile.absolutePath}")

            // 2. 如果主文件存在，先将其移动到备份文件
            if (createBackup && pluginsConfigFile.exists()) {
                try {
                    pluginsConfigFile.copyTo(backupConfigFile, overwrite = true)
                    Timber.tag(TAG).d("主文件已备份到: ${backupConfigFile.absolutePath}")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "创建备份文件失败: ${e.message}")
                    // 备份失败不应阻止主要操作，但应记录
                }
            }

            // 3. 将临时文件重命名（移动）为正式文件
            if (tempConfigFile.renameTo(pluginsConfigFile)) {
                Timber.tag(TAG)
                    .d("临时文件已成功重命名为主文件: ${pluginsConfigFile.absolutePath}")
                hasUnsavedChanges.set(false)
            } else {
                val errorMessage = "将临时文件重命名为主文件失败。源文件: ${tempConfigFile.absolutePath}, 目标文件: ${pluginsConfigFile.absolutePath}"
                Timber.tag(TAG).e(errorMessage)
                throw IOException(errorMessage) // 无法重命名，视为写入失败
            }
        } catch (e: Exception) {
            val errorMessage = "写入 $FILENAME 时发生错误: ${e.message}"
            Timber.tag(TAG).e(e, errorMessage)
            // 清理临时文件，以防失败后留下垃圾
            tempConfigFile.delete()
            throw IOException(errorMessage, e)
        }
    }

    /**
     * 调度延迟写入任务
     * 在短时间内的多次修改只会触发一次磁盘写入
     */
    private fun scheduleDelayedWrite() {
        // 先移除所有之前未执行的延迟写入任务
        writeHandler.removeCallbacks(delayedWriteRunnable)
        // 调度新的延迟写入任务
        writeHandler.postDelayed(delayedWriteRunnable, WRITE_DELAY_MS)
        hasUnsavedChanges.set(true) // 标记有未保存的更改
        Timber.tag(TAG).d("延迟写入已调度。")
    }

    /**
     * 立即同步所有未保存的更改到磁盘
     */
    fun flushToDisk() {
        write {
            // 确保取消任何正在等待的延迟写入任务，因为我们将立即执行
            writeHandler.removeCallbacks(delayedWriteRunnable)
            if (hasUnsavedChanges.get()) {
                try {
                    Timber.tag(TAG).d("立即将未保存的更改刷新到磁盘。")
                    writePluginsToDisk()
                    hasUnsavedChanges.set(false)
                    Timber.tag(TAG).d("刷新成功。")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "将更改刷新到磁盘时发生错误: ${e.message}")
                }
            } else {
                Timber.tag(TAG).d("没有未保存的更改需要刷新。")
            }
        }
    }

    /**
     * 获取所有插件信息
     * @return 插件信息列表的副本
     */
    fun getAllPlugins(): List<PluginInfo> {
        return read {
            if (!cacheInitialized) {
                initializeCache() // 仍然保留这个检查以防万一初始化失败或跳过
            }
            pluginCache.values.toList()
        }
    }

    /**
     * 根据插件ID获取插件信息
     * @param pluginId 插件ID
     * @return 插件信息，如果不存在则返回null
     */
    fun getPluginById(pluginId: String): PluginInfo? {
        return read {
            if (!cacheInitialized) {
                initializeCache()
            }
            pluginCache[pluginId]
        }
    }

    /**
     * 添加新插件
     * @param plugin 要添加的插件信息
     * @throws IllegalArgumentException 如果插件ID已存在
     */
    fun addPlugin(plugin: PluginInfo) {
        write {
            if (!cacheInitialized) {
                initializeCache()
            }

            if (pluginCache.containsKey(plugin.pluginId)) {
                throw IllegalArgumentException("Plugin with ID ${plugin.pluginId} already exists.")
            }

            pluginCache[plugin.pluginId] = plugin
            scheduleDelayedWrite()
            Timber.tag(TAG).d("插件信息已添加: ${plugin.pluginId}")
        }
    }

    /**
     * 更新现有插件
     * @param plugin 要更新的插件信息
     * @throws NoSuchElementException 如果插件不存在
     */
    fun updatePlugin(plugin: PluginInfo) {
        write {
            if (!cacheInitialized) {
                initializeCache()
            }

            if (!pluginCache.containsKey(plugin.pluginId)) {
                throw NoSuchElementException("Plugin with ID ${plugin.pluginId} not found for update.")
            }

            pluginCache[plugin.pluginId] = plugin
            scheduleDelayedWrite()
            Timber.tag(TAG).d("插件信息已更新: ${plugin.pluginId}")
        }
    }

    /**
     * 删除插件信息
     * @param pluginId 要删除的插件ID
     * @return 是否成功删除
     */
    fun removePlugin(pluginId: String): Boolean {
        return write {
            if (!cacheInitialized) {
                initializeCache()
            }

            val removed = pluginCache.remove(pluginId) != null
            if (removed) {
                scheduleDelayedWrite()
                Timber.tag(TAG).d("插件信息已移除: $pluginId")
            } else {
                Timber.tag(TAG).d("未找到要删除的插件: $pluginId")
            }
            removed
        }
    }
}
