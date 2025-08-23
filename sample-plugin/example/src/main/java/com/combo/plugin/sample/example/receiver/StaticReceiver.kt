/*
 *
 *  * Copyright (c) 2025, 贵州君城网络科技有限公司
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.combo.plugin.sample.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.combo.core.interfaces.IPluginReceiver
import com.combo.plugin.sample.common.R
import com.combo.plugin.sample.example.receiver.NotificationUtil.BOOT_CHANNEL_ID
import com.combo.plugin.sample.example.receiver.NotificationUtil.BOOT_NOTIFICATION_ID
import timber.log.Timber

/**
 * 静态广播接收器
 * 实现了框架的 IPluginReceiver 接口，并在 AndroidManifest.xml 中注册。
 * 现在可以处理多种系统广播和自定义广播。
 */
class StaticReceiver : IPluginReceiver {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.d("StaticReceiver 收到广播: $action")

        val message = when (action) {
            // 自定义广播
            BroadcastLog.STATIC_ACTION -> "收到自定义静态广播 - ${intent.getStringExtra("data")}"

            // 系统启动与用户解锁
            Intent.ACTION_BOOT_COMPLETED -> {
                sendBootNotification(context)
                "设备已开机完成，已发送通知"
            }

            Intent.ACTION_USER_PRESENT -> "用户已解锁屏幕"

            // 硬件状态
            BluetoothAdapter.ACTION_STATE_CHANGED -> when (intent.getIntExtra(
                BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR
            )) {
                BluetoothAdapter.STATE_ON -> "蓝牙已开启"
                BluetoothAdapter.STATE_OFF -> "蓝牙已关闭"
                else -> "蓝牙状态变化中..."
            }

            Intent.ACTION_AIRPLANE_MODE_CHANGED -> if (intent.getBooleanExtra(
                    "state",
                    false
                )
            ) "飞行模式已开启" else "飞行模式已关闭"

            Intent.ACTION_HEADSET_PLUG -> if (intent.getIntExtra(
                    "state",
                    0
                ) == 1
            ) "耳机已插入" else "耳机已拔出"

            // 系统设置
            Intent.ACTION_LOCALE_CHANGED -> "系统语言已变更"
            Intent.ACTION_TIMEZONE_CHANGED -> "系统时区已变更: ${intent.getStringExtra("time-zone")}"
            Intent.ACTION_DATE_CHANGED -> "系统日期已变更"
            Intent.ACTION_TIME_CHANGED -> "系统时间已被设置"

            // 应用包管理
            Intent.ACTION_PACKAGE_ADDED -> "安装了新应用: ${intent.data?.schemeSpecificPart}"
            Intent.ACTION_PACKAGE_REPLACED -> "应用已更新: ${intent.data?.schemeSpecificPart}"
            Intent.ACTION_PACKAGE_REMOVED -> "应用已卸载: ${intent.data?.schemeSpecificPart}"

            else -> "收到未处理的白名单广播"
        }

        BroadcastLog.add(
            source = "静态接收器",
            action = action,
            data = message
        )
    }

    /**
     * 创建并发送开机完成通知
     */
    private fun sendBootNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, BOOT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_icon))
            .setContentTitle("插件框架已启动")
            .setContentText("点击以返回应用。")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(BOOT_NOTIFICATION_ID, notification)
        Timber.d("开机通知已发送")
    }
}