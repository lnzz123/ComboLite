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

package com.combo.plugin.sample.example.screen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.combo.core.ext.bindPluginService
import com.combo.core.ext.startPluginService
import com.combo.core.ext.stopPluginService
import com.combo.plugin.sample.example.service.StopwatchService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 用于在UI层追踪每个服务实例的状态
data class ServiceInfo(
    val id: Int,
    val serviceClass: Class<out StopwatchService>,
    var isBound: Boolean = false,
    var elapsedTime: Long = 0L,
    var serviceConnection: ServiceConnection? = null,
    var binder: StopwatchService.StopwatchBinder? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen() {
    val context = LocalContext.current
    val runningServices = remember { mutableStateListOf<ServiceInfo>() }
    var statusMessage by remember { mutableStateOf("点击按钮来启动服务") }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val serviceId = intent?.getIntExtra(StopwatchService.EXTRA_SERVICE_ID, -1) ?: -1
                if (serviceId == -1) return

                when (intent?.action) {
                    StopwatchService.ACTION_SERVICE_STARTED -> {
                        if (runningServices.none { it.id == serviceId }) {
                            runningServices.add(
                                ServiceInfo(id = serviceId, serviceClass = StopwatchService::class.java)
                            )
                            statusMessage = "服务实例 #$serviceId 已启动"
                        }
                    }
                    StopwatchService.ACTION_SERVICE_STOPPED -> {
                        runningServices.removeAll { it.id == serviceId }
                        statusMessage = "服务实例 #$serviceId 已停止"
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(StopwatchService.ACTION_SERVICE_STARTED)
            addAction(StopwatchService.ACTION_SERVICE_STOPPED)
        }
        // 在插件化框架中，通常需要使用 proxyActivity/Context 来注册
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("服务代理池示例", fontWeight = FontWeight.Bold) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // --- 主控制区 ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("主控制", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "你的框架支持服务代理池，允许多次启动同一个Service类来创建多个独立实例。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = {
                        val hostServiceClass = com.combo.core.manager.PluginManager.proxyManager.acquireServiceProxy(StopwatchService::class.java.name)
                        if (hostServiceClass != null) {
                            context.startPluginService(StopwatchService::class.java)
                        } else {
                            statusMessage = "启动失败：服务代理池已达上限！"
                        }
                    }) { Text("启动一个新的秒表服务") }
                    Text(statusMessage, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("当前运行的服务实例列表", style = MaterialTheme.typography.titleMedium)

            // --- 运行中的服务列表 ---
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(runningServices, key = { it.id }) { serviceInfo ->
                    ServiceControlCard(serviceInfo = serviceInfo, context = context)
                }
            }
        }
    }
}

@Composable
fun ServiceControlCard(serviceInfo: ServiceInfo, context: Context) {
    var elapsedTime by remember { mutableStateOf(serviceInfo.elapsedTime) }

    DisposableEffect(serviceInfo.id) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as StopwatchService.StopwatchBinder
                serviceInfo.binder = binder
                serviceInfo.isBound = true

                // 绑定成功后，开始收集服务中的时间流
                CoroutineScope(Dispatchers.Main).launch {
                    binder.getService().elapsedTime.collect { time ->
                        elapsedTime = time
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceInfo.binder = null
                serviceInfo.isBound = false
            }
        }
        serviceInfo.serviceConnection = connection

        onDispose {
            if (serviceInfo.isBound) {
                context.unbindService(connection)
            }
        }
    }

    val timeFormatter = remember { SimpleDateFormat("mm:ss.SSS", Locale.getDefault()) }

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("秒表服务实例 #${serviceInfo.id}", style = MaterialTheme.typography.titleMedium)
            Text(
                timeFormatter.format(Date(elapsedTime)),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (serviceInfo.isBound) {
                        serviceInfo.serviceConnection?.let { context.unbindService(it) }
                        serviceInfo.isBound = false
                    } else {
                        serviceInfo.serviceConnection?.let {
                            context.bindPluginService(serviceInfo.serviceClass, it, Context.BIND_AUTO_CREATE)
                        }
                    }
                }) {
                    Text(if (serviceInfo.isBound) "解绑" else "绑定")
                }

                Button(
                    onClick = {
                        context.stopPluginService(serviceInfo.serviceClass)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("停止此服务")
                }
            }
        }
    }
}