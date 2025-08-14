package com.jctech.plugin.sample

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jctech.plugin.core.interfaces.IPluginEntryClass
import com.jctech.plugin.core.manager.PluginManager
import com.jctech.plugin.core.manager.PluginManager.isPluginLoaded
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class LoadingViewModel(
    context: Context
) : ViewModel() {
    @SuppressLint("StaticFieldLeak")
    private val context = context.applicationContext

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _entryClass = MutableStateFlow<IPluginEntryClass?>(null)
    val entryClass: StateFlow<IPluginEntryClass?> = _entryClass.asStateFlow()

    companion object {
        const val BASE_PATH = "plugins/base"
        const val PLUGIN_COMMON = "common"
        const val PLUGIN_HOME = "home"
    }

    init {
        init()
    }

    fun init() {
        viewModelScope.launch {
            setLoading(true)
            _entryClass.value = PluginManager.getPluginInstance(PLUGIN_HOME)
            if (entryClass.value == null || !isPluginLoaded(PLUGIN_COMMON)) {
                installPlugin(BASE_PATH)
            }
            setLoading(false)
        }
    }

    fun setLoading(isLoading: Boolean) {
        _loading.value = isLoading
    }

    fun installPlugin(assetPath: String, forceOverwrite: Boolean = false) {
        viewModelScope.launch {
            setLoading(true)
            val pluginFiles = context.assets.list(assetPath)
            pluginFiles?.forEach { fileName ->
                val pluginFile = File(context.filesDir, fileName)
                context.assets.open("$assetPath/$fileName").use { inputStream ->
                    FileOutputStream(pluginFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                PluginManager.installerManager.installPlugin(pluginFile, forceOverwrite)
            }
            PluginManager.loadEnabledPlugins()
            _entryClass.value = PluginManager.getPluginInstance(PLUGIN_HOME)
            setLoading(false)
        }
    }
}
