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

package com.jctech.plugin.core.loader

import dalvik.system.DexClassLoader
import timber.log.Timber
import java.io.File

/**
 * 插件类加载器
 *
 * 该类加载器用于加载插件中的类。它继承自 DexClassLoader，
 * 并在 findClass 方法中添加了插件查找的逻辑。
 *
 * @param pluginId 插件ID，用于标识插件
 * @param dexPath 插件的 dex 文件路径
 * @param optimizedDirectory 优化后的 dex 文件存储目录
 * @param librarySearchPath 库文件搜索路径
 * @param parent 父类加载器
 * @param pluginFinder 插件查找器，用于在其他插件中查找类
 */
open class PluginClassLoader(
    private val pluginId: String,
    dexPath: String,
    optimizedDirectory: String?,
    librarySearchPath: String?,
    parent: ClassLoader?,
    private val pluginFinder: IPluginFinder?
) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent) {

    constructor(
        pluginId: String,
        pluginFile: File,
        parent: ClassLoader,
        pluginFinder: IPluginFinder?
    ) : this(
        pluginId = pluginId,
        dexPath = pluginFile.absolutePath,
        optimizedDirectory = File(pluginFile.parent, "dex_opt").absolutePath.also {
            File(it).mkdirs()
        },
        librarySearchPath = null,
        parent = parent,
        pluginFinder = pluginFinder
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
            val otherPluginClass = pluginFinder?.findClass(name)
            if (otherPluginClass != null) {
                return otherPluginClass
            }
            throw e
        }
    }

    /**
     * 新增的公共方法：只在当前 ClassLoader 的 dex 文件中查找类。
     * 这个方法是 public 的，所以 PluginManager 可以调用它。
     * 它打破了递归链，因为它不会再回调 pluginFinder。
     */
    @Throws(ClassNotFoundException::class)
    fun findClassLocally(name: String): Class<*> {
        return super.findClass(name)
    }

    /**
     * 获取指定接口的实现实例
     *
     * 该方法提供类型安全的方式来获取插件中接口的具体实现。会自动处理类加载、
     * 实例化和类型转换过程，并在出现异常时记录详细日志并返回null。
     *
     * 支持的异常处理：
     * - ClassNotFoundException: 类未找到
     * - InstantiationException: 实例化失败
     * - IllegalAccessException: 访问权限不足
     * - ClassCastException: 类型转换失败
     * - NoSuchMethodException: 构造函数未找到
     *
     * @param T 接口类型泛型
     * @param interfaceClass 接口的Class对象
     * @param className 实现类的完整类名
     * @return 接口实现的实例，失败时返回null
     *
     * @throws SecurityException 当访问被安全管理器拒绝时
     */
    fun <T> getInterface(interfaceClass: Class<T>, className: String): T? {
        return try {
            // 加载指定的实现类
            val clazz = loadClass(className)

            // 使用默认构造函数创建实例
            val instance = clazz.getDeclaredConstructor().newInstance()

            // 验证实例是否实现了指定接口
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
}
