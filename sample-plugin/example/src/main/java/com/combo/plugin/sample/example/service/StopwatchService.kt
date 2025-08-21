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

package com.combo.plugin.sample.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service.START_NOT_STICKY
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.combo.core.base.BasePluginService
import com.combo.core.ext.sendInternalBroadcast
import com.combo.plugin.sample.example.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class StopwatchService : BasePluginService() {

    companion object {
        // 用于Service和UI通信的广播Action
        const val ACTION_SERVICE_STARTED = "com.combo.plugin.example.SERVICE_STARTED"
        const val ACTION_SERVICE_STOPPED = "com.combo.plugin.example.SERVICE_STOPPED"
        const val EXTRA_SERVICE_ID = "service_id"

        // 用于生成唯一的服务ID
        private val idCounter = AtomicInteger(0)
    }

    private val serviceId = idCounter.incrementAndGet()
    private val TAG = "StopwatchService[#$serviceId]"

    private val NOTIFICATION_ID = 1000 + serviceId
    private val CHANNEL_ID = "StopwatchServiceChannel"

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val binder = StopwatchBinder()

    // 使用 StateFlow 来向绑定的客户端广播时间
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asStateFlow()
    private var timerJob: Job? = null

    // --- Binder, 用于客户端通信 ---
    inner class StopwatchBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }

    // --- Service 生命周期 ---
    override fun onCreate() {
        super.onCreate()
        Timber.d("$TAG: onCreate - 服务实例已创建")
        createNotificationChannel()
        startTimer()

        // 服务创建后，立即发送广播通知UI
        proxyService?.sendInternalBroadcast(ACTION_SERVICE_STARTED) {
            putExtra(EXTRA_SERVICE_ID, serviceId)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("$TAG: onStartCommand - 服务已启动")
        // [功能点] 前台服务
        // 每次启动都确保服务是前台服务
        val notification = createNotification("计时中...")
        proxyService?.startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.d("$TAG: onBind - 客户端已绑定")
        // [功能点] 绑定服务
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // 停止所有协程任务，包括计时器
        Timber.d("$TAG: onDestroy - 服务实例已销毁")

        // [功能点] 停止服务
        // 服务销毁时，发送广播通知UI
        proxyService?.sendInternalBroadcast(ACTION_SERVICE_STOPPED) {
            putExtra(EXTRA_SERVICE_ID, serviceId)
        }
    }

    // --- 核心功能 ---
    private fun startTimer() {
        val startTime = System.currentTimeMillis()
        timerJob = serviceScope.launch {
            while (isActive) {
                _elapsedTime.value = System.currentTimeMillis() - startTime
                updateNotification(formatTime(_elapsedTime.value))
                delay(50) // 每50毫秒更新一次时间
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val format = SimpleDateFormat("mm:ss.SSS", Locale.getDefault())
        return format.format(Date(millis))
    }

    // --- 通知栏 ---
    private fun updateNotification(time: String) {
        val notification = createNotification("已运行: $time")
        val manager = proxyService?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "秒表服务通知", NotificationManager.IMPORTANCE_LOW)
            val manager = proxyService?.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String) = NotificationCompat.Builder(proxyService!!, CHANNEL_ID)
        .setContentTitle("插件秒表服务 [#$serviceId]")
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_timer)
        .setOnlyAlertOnce(true)
        .build()
}