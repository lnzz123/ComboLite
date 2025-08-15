package com.combo.plugin.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.combo.core.base.BaseHostActivity
import com.combo.core.manager.PluginManager

class HostActivity : BaseHostActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (super.pluginActivity == null) {
            enableEdgeToEdge()
            setContent {
                val resources by PluginManager.resourcesManager.mResourcesFlow.collectAsState()
                key(resources) {
                    LoadingScreen()
                }
            }
        }
    }
}
