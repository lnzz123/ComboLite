package com.combo.plugin.sample.example.activity

import android.os.Bundle
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.combo.core.base.BasePluginActivity
import com.combo.plugin.sample.example.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * XML UI 布局示例 Activity
 * 展示如何在插件中使用传统的 XML 布局和 findViewById
 */
class XmlActivity : BasePluginActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        proxyActivity?.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar)
        // 使用代理 Activity 设置布局
        proxyActivity?.setContentView(R.layout.activity_xml)

        // 使用 findViewById 获取视图引用
        toolbar = proxyActivity?.findViewById(R.id.toolbar)!!
        fab = proxyActivity?.findViewById(R.id.fab)!!

        // 设置导航
        val navController = proxyActivity?.findNavController(R.id.nav_host_fragment_content_xml)!!
        appBarConfiguration = AppBarConfiguration(navController.graph)
        
        // 设置 Toolbar 与导航控制器的关联
        toolbar.setupWithNavController(navController, appBarConfiguration)

        // 设置 FAB 点击事件
        fab.setOnClickListener {
            Toast.makeText(proxyActivity, "这是一个由插件Activity加载的 XML 布局页面", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * 处理导航返回事件
     * 由于 BasePluginActivity 不提供 onSupportNavigateUp 方法，
     * 我们需要通过其他方式处理返回导航
     */
    private fun handleNavigateUp(): Boolean {
        val navController = proxyActivity?.findNavController(R.id.nav_host_fragment_content_xml)
        return navController?.navigateUp(appBarConfiguration) == true
    }

    /**
     * 重写 onKeyDown 方法处理返回键事件
     * 当用户按返回键时，调用导航处理逻辑
     */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean? {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            return if (handleNavigateUp()) {
                true
            } else {
                super.onKeyDown(keyCode, event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}