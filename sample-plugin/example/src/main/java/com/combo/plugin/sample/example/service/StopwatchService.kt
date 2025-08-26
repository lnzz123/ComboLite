/*
 * Copyright (c) 2025, 贵州君城网络科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.combo.plugin.sample.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service.START_NOT_STICKY
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.combo.core.base.BasePluginService
import com.combo.core.ext.sendInternalBroadcast
import com.combo.plugin.sample.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StopwatchService : BasePluginService() {

    companion object {
        private var TAG = "StopwatchService"
        private const val CHANNEL_ID = "StopwatchServiceChannel"
        const val ACTION_SERVICE_STARTED = "com.combo.plugin.example.SERVICE_STARTED"
        const val ACTION_SERVICE_STOPPED = "com.combo.plugin.example.SERVICE_STOPPED"
        const val EXTRA_SERVICE_ID = "extra_service_id"
    }

    private var instanceId: String? = null
    private val notificationId get() = instanceId.hashCode()

    private var creationTime = 0L

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val binder = StopwatchBinder()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asStateFlow()
    private var uiTimerJob: Job? = null
    private var notificationUpdateJob: Job? = null

    inner class StopwatchBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Timber.d("A new StopwatchService instance object has been created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val receivedId = intent?.getStringExtra(EXTRA_SERVICE_ID)
        if (receivedId == null) {
            Timber.e("Service started without a valid instanceId. Stopping self.")
            proxyService?.stopSelf()
            return START_NOT_STICKY
        }

        if (this.instanceId == null) {
            this.instanceId = receivedId
            TAG = "StopwatchService[#$instanceId]"
            Timber.d("$TAG: Initializing service instance.")

            this.creationTime = System.currentTimeMillis()

            startTimers()

            proxyService?.sendInternalBroadcast(ACTION_SERVICE_STARTED) {
                putExtra(EXTRA_SERVICE_ID, instanceId)
            }
        }

        Timber.d("$TAG: onStartCommand received.")

        // 首次启动时，创建一个包含系统计时器的通知
        val notification = createNotification("计时准备中...")
        proxyService?.startForeground(notificationId, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.d("$TAG: onBind - A client has bound.")
        if (this.instanceId == null) {
            val receivedId = intent?.getStringExtra(EXTRA_SERVICE_ID)
            if (receivedId != null) {
                this.instanceId = receivedId
                TAG = "StopwatchService[#$instanceId]"
                if (creationTime == 0L) {
                    this.creationTime = System.currentTimeMillis()
                }
                Timber.d("$TAG: Initialized via onBind.")
                startTimers()
            }
        }
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Timber.d("$TAG: onDestroy - Service instance is being destroyed.")

        proxyService?.sendInternalBroadcast(ACTION_SERVICE_STOPPED) {
            putExtra(EXTRA_SERVICE_ID, instanceId)
        }
    }

    /**
     * 2. 拆分为两个独立的计时器循环
     */
    private fun startTimers() {
        if (uiTimerJob?.isActive != true) {
            uiTimerJob = serviceScope.launch {
                while (isActive) {
                    _elapsedTime.value = System.currentTimeMillis() - creationTime
                }
            }
        }

        // 计时器二：低频更新，专门给通知栏使用，避免系统节流
        if (notificationUpdateJob?.isActive != true) {
            notificationUpdateJob = serviceScope.launch {
                while (isActive) {
                    updateNotification(formatTime(_elapsedTime.value))
                    delay(1000)
                }
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val format = SimpleDateFormat("mm:ss", Locale.getDefault())
        return format.format(Date(millis))
    }

    private fun updateNotification(time: String) {
        if (instanceId == null) return
        val notification = createNotification("计时: $time")
        val manager =
            proxyService?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, "秒表服务通知", NotificationManager.IMPORTANCE_LOW)
            val manager = proxyService?.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val iconResId = R.drawable.ic_timer

        val builder = NotificationCompat.Builder(proxyService!!, CHANNEL_ID)
            .setContentTitle("插件秒表服务 [#$instanceId]")
            .setContentText(contentText)
            .setWhen(creationTime)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)

        try {
            val drawable = ContextCompat.getDrawable(proxyService!!, iconResId)
            val bitmap = drawable?.toBitmap(width = 128, height = 128)

            if (bitmap != null) {
                builder.setSmallIcon(IconCompat.createWithBitmap(bitmap))
            } else {
                builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            }
        } catch (e: Exception) {
            Timber.e(e, "加载插件图标资源失败，使用默认系统图标")
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
        }

        return builder.build()
    }
}