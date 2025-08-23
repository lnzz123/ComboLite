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

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.combo.core.ext.bindPluginService
import com.combo.core.ext.startPluginService
import com.combo.core.ext.stopPluginService
import com.combo.core.manager.PluginManager
import com.combo.plugin.sample.example.service.StopwatchService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class ServiceInfo(
    val id: String,
    val serviceClass: Class<out StopwatchService>,
    val elapsedTime: MutableStateFlow<Long> = MutableStateFlow(0L),
    var isBound: Boolean = false,
    var serviceConnection: ServiceConnection? = null,
    var binder: StopwatchService.StopwatchBinder? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen() {
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("服务代理池示例", fontWeight = FontWeight.Bold) }) }
    ) { paddingValues ->
        if (hasNotificationPermission) {
            ServiceControlContent(paddingValues)
        } else {
            PermissionRequestContent(paddingValues)
        }
    }
}

/**
 * 当用户未授予通知权限时显示的界面
 */
@Composable
private fun PermissionRequestContent(paddingValues: PaddingValues) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "需要通知权限",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "此功能需要通知权限才能启动前台服务并实时显示秒表状态。请在应用设置中开启通知权限。",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(intent)
        }) {
            Text("前往设置")
        }
    }
}

/**
 * 拥有权限后显示的主控制界面
 */
@Composable
private fun ServiceControlContent(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val runningServices = remember { mutableStateListOf<ServiceInfo>() }
    var statusMessage by remember { mutableStateOf("正在同步服务状态...") }
    val serviceIdCounter = remember { AtomicInteger(0) }

    fun createServiceInfoFor(id: String): ServiceInfo {
        val serviceInfo = ServiceInfo(id = id, serviceClass = StopwatchService::class.java)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as StopwatchService.StopwatchBinder
                serviceInfo.binder = binder
                serviceInfo.isBound = true

                CoroutineScope(Dispatchers.Main).launch {
                    binder.getService().elapsedTime.collect { time ->
                        serviceInfo.elapsedTime.value = time
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceInfo.binder = null
                serviceInfo.isBound = false
            }
        }
        serviceInfo.serviceConnection = connection
        return serviceInfo
    }

    LaunchedEffect(Unit) {
        val serviceClassName = StopwatchService::class.java.name
        val runningInstanceIds = PluginManager.proxyManager.getRunningInstancesFor(serviceClassName)

        runningServices.clear()
        var maxIdNumber = -1

        runningInstanceIds.forEach { fullIdentifier ->
            val shortId = fullIdentifier.substringAfter(':', fullIdentifier)
            val serviceInfo = createServiceInfoFor(shortId)
            runningServices.add(serviceInfo)

            val idNumber = shortId.substringAfter("stopwatch-").toIntOrNull()
            if (idNumber != null && idNumber > maxIdNumber) {
                maxIdNumber = idNumber
            }
        }
        serviceIdCounter.set(maxIdNumber + 1)
        statusMessage = "服务状态同步完成，当前有 ${runningServices.size} 个实例在运行。"
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val serviceId = intent?.getStringExtra(StopwatchService.EXTRA_SERVICE_ID) ?: return

                when (intent.action) {
                    StopwatchService.ACTION_SERVICE_STARTED -> {
                        if (runningServices.none { it.id == serviceId }) {
                            runningServices.add(createServiceInfoFor(serviceId))
                            statusMessage = "服务实例 #$serviceId 已启动"
                        }
                    }

                    StopwatchService.ACTION_SERVICE_STOPPED -> {
                        val toRemove = runningServices.find { it.id == serviceId }
                        if (toRemove != null) {
                            if (toRemove.isBound) {
                                toRemove.serviceConnection?.let {
                                    try {
                                        context?.unbindService(it)
                                    } catch (e: Exception) {
                                        Timber.e(e, "解绑服务异常")
                                    }
                                }
                            }
                            runningServices.remove(toRemove)
                            statusMessage = "服务实例 #$serviceId 已停止"
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(StopwatchService.ACTION_SERVICE_STARTED)
            addAction(StopwatchService.ACTION_SERVICE_STOPPED)
        }
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

    DisposableEffect(Unit) {
        onDispose {
            runningServices.forEach { serviceInfo ->
                if (serviceInfo.isBound) {
                    serviceInfo.serviceConnection?.let {
                        try {
                            context.unbindService(it)
                            serviceInfo.isBound = false
                        } catch (e: Exception) {
                            Timber.e(e, "离开页面时解绑失败")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("主控制", style = MaterialTheme.typography.titleLarge)
                Text(
                    "框架支持服务代理池，允许多次启动同一个Service类来创建多个独立实例。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = {
                    val newServiceId = "stopwatch-${serviceIdCounter.getAndIncrement()}"
                    val instanceIdentifier = "${StopwatchService::class.java.name}:$newServiceId"

                    val proxyClass =
                        PluginManager.proxyManager.acquireServiceProxy(instanceIdentifier)

                    if (proxyClass != null) {
                        PluginManager.proxyManager.releaseServiceProxy(instanceIdentifier)

                        context.startPluginService(
                            cls = StopwatchService::class.java,
                            instanceId = newServiceId
                        ) {
                            putExtra(StopwatchService.EXTRA_SERVICE_ID, newServiceId)
                        }
                        statusMessage = "正在尝试启动服务实例 #$newServiceId..."
                    } else {
                        statusMessage = "启动失败：服务代理池已达上限！"
                        Toast.makeText(context, "启动失败：服务代理池已达上限！", Toast.LENGTH_SHORT)
                            .show()
                    }

                }) { Text("启动一个新的秒表服务") }
                Text(statusMessage, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("当前运行的服务实例列表", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(runningServices, key = { it.id }) { serviceInfo ->
                ServiceControlCard(
                    serviceInfo = serviceInfo,
                    onBind = {
                        serviceInfo.serviceConnection?.let {
                            context.bindPluginService(
                                cls = serviceInfo.serviceClass,
                                instanceId = serviceInfo.id,
                                connection = it,
                                flags = Context.BIND_AUTO_CREATE
                            )
                        }
                    },
                    onUnbind = {
                        if (serviceInfo.isBound) {
                            serviceInfo.serviceConnection?.let {
                                try {
                                    context.unbindService(it)
                                    serviceInfo.isBound = false
                                } catch (e: Exception) {
                                    Timber.w(e, "解绑失败")
                                }
                            }
                        }
                    },
                    onStop = {
                        context.stopPluginService(
                            cls = serviceInfo.serviceClass,
                            instanceId = serviceInfo.id
                        )
                    }
                )
            }
        }
    }
}


@Composable
fun ServiceControlCard(
    serviceInfo: ServiceInfo,
    onBind: () -> Unit,
    onUnbind: () -> Unit,
    onStop: () -> Unit,
) {
    val elapsedTime by serviceInfo.elapsedTime.collectAsState()
    val timeFormatter = remember { SimpleDateFormat("mm:ss.SSS", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("秒表服务实例 #${serviceInfo.id}", style = MaterialTheme.typography.titleMedium)
            Text(
                timeFormatter.format(Date(elapsedTime)),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (serviceInfo.isBound) onUnbind() else onBind()
                }) {
                    Text(if (serviceInfo.isBound) "解绑" else "绑定")
                }

                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("停止此服务")
                }
            }
        }
    }
}