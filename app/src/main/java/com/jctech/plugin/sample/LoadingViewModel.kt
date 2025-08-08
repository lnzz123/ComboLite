package com.jctech.plugin.sample

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jctech.plugin.core.interfaces.IPluginEntryClass
import com.jctech.plugin.core.manager.PluginManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class LoadingViewModel(
    context: Context,
    private val pluginManager: PluginManager
) : ViewModel() {
    @SuppressLint("StaticFieldLeak")
    private val context = context.applicationContext

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _pluginCount = MutableStateFlow(0)
    val pluginCount: StateFlow<Int> = _pluginCount.asStateFlow()

    private val _entryClass = MutableStateFlow<IPluginEntryClass?>(null)
    val entryClass: StateFlow<IPluginEntryClass?> = _entryClass.asStateFlow()

    init {
        init()
    }

    fun init(){
        viewModelScope.launch {
            setLoading(true)
            _pluginCount.value = pluginManager.initialize()
            if (pluginCount.value > 0) {
                pluginManager.getPluginEntryInstance("example_common")
                _entryClass.value = pluginManager.getPluginEntryInstance("example_home")
            }
            setLoading(false)
        }
    }

    fun setLoading(isLoading: Boolean) {
        _loading.value = isLoading
    }

    fun installPlugin(assetPath: String) {
        viewModelScope.launch {
            setLoading(true)
            // 从assets安装指定路径下所有插件
            val pluginFiles = context.assets.list(assetPath)
            pluginFiles?.forEach { fileName ->
                val pluginFile = File(context.filesDir, fileName)
                context.assets.open("$assetPath/$fileName").use { inputStream ->
                    FileOutputStream(pluginFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // 安装插件
                pluginManager.installPlugin(pluginFile)
            }
            init()
        }
    }
}
