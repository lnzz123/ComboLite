package com.jctech.plugin.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jctech.plugin.core.base.BaseHostActivity
import com.jctech.plugin.core.manager.PluginManager
import com.jctech.plugin.sample.ui.theme.ComposePluginSampleTheme

class MainActivity : BaseHostActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (super.pluginActivity == null) {
            PluginManager.setHostActivity(this::class.java)
            enableEdgeToEdge()
            setContent {
                val resources by PluginManager.getResourcesManager().mResourcesFlow.collectAsState()
                key(resources) {
                    LoadingScreen()
                }

                //ComposeContent()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposePluginSampleTheme {
        Greeting("Android")
    }
}