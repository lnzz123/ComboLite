package com.combo.core.loader

interface IPluginFinder {
    /**
     * 在所有插件中查找类。
     * @param className 类的完整名称
     * @return 如果找到，则返回 Class 对象，否则返回 null。
     */
    fun findClass(className: String, requester: PluginClassLoader): Class<*>?
}