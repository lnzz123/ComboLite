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

package com.jctech.plugin.core.ext

import android.content.Context
import android.content.Intent
import com.jctech.plugin.core.interfaces.IPluginActivity
import com.jctech.plugin.core.manager.PluginManager
import timber.log.Timber


/**
 * 从Intent中获取插件Activity
 */
fun Intent.getPluginActivity(): IPluginActivity? {
    return getStringExtra("pluginClassName")?.let {
        PluginManager.getInterface(IPluginActivity::class.java, it)
    }
}

/**
 * 跳转插件Activity
 * @param cls 插件Activity的Class
 */
fun Context.startPluginActivity(cls: Class<out IPluginActivity>) {
    val hostActivity = PluginManager.getHostActivity()
    if (hostActivity == null) {
        Timber.e("插件Activity宿主未设置")
        return
    }
    val intent = Intent(this, hostActivity)
    intent.putExtra("pluginClassName", cls.name)
    startActivity(intent)
}
