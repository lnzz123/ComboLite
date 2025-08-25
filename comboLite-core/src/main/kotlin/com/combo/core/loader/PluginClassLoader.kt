/*
 *
 *  * Copyright (c) 2025, 贵州君城网络科技有限公司
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.combo.core.loader

import com.combo.core.security.PluginDependencyException
import dalvik.system.DexClassLoader
import timber.log.Timber
import java.io.File

/**
 * 插件类加载器
 *
 * 该类加载器用于加载插件中的类。它继承自 DexClassLoader，
 * 并在 findClass 方法中添加了插件查找的逻辑。
 *
 * @param dexPath 插件的 dex 文件路径
 * @param optimizedDirectory 优化后的 dex 文件存储目录
 * @param librarySearchPath 库文件搜索路径
 * @param parent 父类加载器
 * @param pluginFinder 插件查找器，用于在其他插件中查找类
 */
open class PluginClassLoader(
    internal val pluginId: String,
    dexPath: String,
    optimizedDirectory: String?,
    librarySearchPath: String?,
    parent: ClassLoader?,
    private val pluginFinder: IPluginFinder?,
) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent) {

    constructor(
        pluginId: String,
        pluginFile: File,
        parent: ClassLoader,
        optimizedDirectory: String,
        librarySearchPath: String?,
        pluginFinder: IPluginFinder?
    ) : this(
        pluginId = pluginId,
        dexPath = pluginFile.absolutePath,
        parent = parent,
        optimizedDirectory = optimizedDirectory,
        librarySearchPath = librarySearchPath,
        pluginFinder = pluginFinder,
    )

    /**
     * 重写 findClass 方法，先在当前 ClassLoader 中查找类，
     * 如果未找到，再回调 pluginFinder 查找。
     */
    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*> {
        try {
            return super.findClass(name)
        } catch (e: ClassNotFoundException) {
            val result = pluginFinder?.findClass(name, this)
            if (result != null) {
                return result
            }
            throw PluginDependencyException(
                culpritPluginId = this.pluginId,
                missingClassName = name,
                cause = e
            )
        }
    }

    /**
     * 新增的公共方法：只在当前 ClassLoader 的 dex 文件中查找类。
     * 这个方法是 public 的，所以 PluginManager 可以调用它。
     * 它打破了递归链，因为它不会再回调 pluginFinder。
     */
    @Throws(ClassNotFoundException::class)
    fun findClassLocally(name: String): Class<*> = super.findClass(name)

    /**
     * 获取指定接口的实现实例
     *
     * 该方法提供类型安全的方式来获取插件中接口的具体实现。会自动处理类加载、
     * 实例化和类型转换过程，并在出现异常时记录详细日志并返回null。
     * @param T 接口类型泛型
     * @param interfaceClass 接口的Class对象
     * @param className 实现类的完整类名
     * @return 接口实现的实例，失败时返回null
     */
    fun <T> getInterface(
        interfaceClass: Class<T>,
        className: String,
    ): T? =
        try {
            val clazz = loadClass(className)

            val instance = clazz.getDeclaredConstructor().newInstance()

            if (interfaceClass.isInstance(instance)) {
                @Suppress("UNCHECKED_CAST")
                instance as T
            } else {
                Timber.e("类 $className 未实现接口 ${interfaceClass.name}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "加载接口实现类失败: $className")
            null
        }
}
