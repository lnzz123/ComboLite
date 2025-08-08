package com.jctech.plugin.core.loader

interface IPluginFinder {
    fun findClass(pluginId: String, className: String): Class<*>?
}