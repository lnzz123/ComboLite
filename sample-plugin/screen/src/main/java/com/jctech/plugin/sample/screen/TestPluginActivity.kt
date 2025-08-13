package com.jctech.plugin.sample.screen

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.jctech.plugin.core.base.BasePluginActivity

class TestPluginActivity : BasePluginActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        proxyActivity?.setContentView(R.layout.activity_fullscreen)
        val button = proxyActivity?.findViewById<Button>(R.id.dummy_button)
        button?.setOnClickListener {
            Toast.makeText(proxyActivity, "Hello World! 这是插件activity", Toast.LENGTH_SHORT)
                .show()
        }
    }
}