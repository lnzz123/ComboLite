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

package com.jctech.plugin.core.base

import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.PersistableBundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import com.jctech.plugin.core.ext.getPluginActivity
import com.jctech.plugin.core.interfaces.IPluginActivity
import com.jctech.plugin.core.manager.PluginManager

/**
 * 宿主端的代理Activity基类
 * 它负责加载插件Activity并代理其所有生命周期方法。
 */
open class BaseHostActivity : ComponentActivity() {

    protected var pluginActivity: IPluginActivity? = null
        private set

    /**
     * 提供一个给子类调用的、受保护的插件初始化方法。
     * 子类在自己的 onCreate 中调用此方法来"尝试"加载插件。
     */
    protected fun initPluginActivity(intent: Intent?) {
        try {
            intent ?: return
            pluginActivity = intent.getPluginActivity()
            pluginActivity?.onAttach(this@BaseHostActivity)
        } catch (e: Exception) {
            e.printStackTrace()
            pluginActivity = null
        }
    }

    /**
     * 重写getResources方法，返回插件资源
     */
    override fun getResources(): Resources {
        return if (PluginManager.isInitialized())
            PluginManager.resourcesManager.getResources()
        else super.getResources()
    }
    /**
     * 重写getAssets方法，返回插件资源
     */
    override fun getAssets(): AssetManager {
        return if (PluginManager.isInitialized())
            PluginManager.resourcesManager.getResources().assets
        else super.getAssets()
    }
    /**
     * 标准生命周期
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPluginActivity(intent)
        pluginActivity?.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        pluginActivity?.onStart()
    }
    override fun onResume() {
        super.onResume()
        pluginActivity?.onResume()
    }
    override fun onPause() {
        super.onPause()
        pluginActivity?.onPause()
    }
    override fun onStop() {
        super.onStop()
        pluginActivity?.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        pluginActivity?.onDestroy()
    }
    override fun onRestart() {
        super.onRestart()
        pluginActivity?.onRestart()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        pluginActivity?.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        pluginActivity?.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        pluginActivity?.onRestoreInstanceState(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        pluginActivity?.onConfigurationChanged(newConfig)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        pluginActivity?.onWindowFocusChanged(hasFocus)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return pluginActivity?.onKeyDown(keyCode, event) ?: super.onKeyDown(keyCode, event)
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return pluginActivity?.onKeyUp(keyCode, event) ?: super.onKeyUp(keyCode, event)
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return pluginActivity?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }
}
