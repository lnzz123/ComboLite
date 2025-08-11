package com.jctech.plugin.core.base

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import com.jctech.plugin.core.interfaces.IPluginActivity

open class BasePluginActivity : IPluginActivity {
    /**
     * 插件Activity的代理Activity
     */
    protected var proxyActivity: ComponentActivity? = null
        private set
    /**
     * 当宿主加载插件时，会通过这个方法把自身（代理Activity）传递进来。
     */
    override fun onAttach(proxyActivity: ComponentActivity) {
        this.proxyActivity = proxyActivity
    }

    // 标准生命周期
    override fun onCreate(savedInstanceState: Bundle?) {
    }

    override fun onResume() {}

    override fun onPause() {}

    override fun onStart() {}

    override fun onStop() {}

    override fun onDestroy() {}

    override fun onRestart() {}

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {}

    override fun onNewIntent(intent: Intent?) {}

    override fun onSaveInstanceState(outState: Bundle) {}

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {}

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onWindowFocusChanged(hasFocus: Boolean) {}

    override fun onLowMemory() {}

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }
}
