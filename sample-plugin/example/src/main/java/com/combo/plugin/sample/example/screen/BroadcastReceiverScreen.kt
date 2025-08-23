package com.combo.plugin.sample.example.screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.combo.core.ext.sendInternalBroadcast
import com.combo.plugin.sample.example.receiver.BroadcastLog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastReceiverScreen() {
    val context = LocalContext.current
    val logMessages = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        BroadcastLog.logFlow.collect { message ->
            logMessages.add(0, message)
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }
    DisposableEffect(context) {
        val dynamicReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context, // Context can be nullable in some cases, good practice to make it nullable here.
                intent: Intent
            ) {
                BroadcastLog.add("动态接收器", intent.action, intent.getStringExtra("data"))
            }
        }
        val intentFilter = IntentFilter(BroadcastLog.DYNAMIC_ACTION)
        ContextCompat.registerReceiver(context, dynamicReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        BroadcastLog.add("系统", "动态广播接收器", "已在本页面注册")
        onDispose {
            context.unregisterReceiver(dynamicReceiver)
            BroadcastLog.add("系统", "动态广播接收器", "已在本页面注销")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("广播接收器示例", fontWeight = FontWeight.Bold) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var tabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf("动态广播", "静态广播")

            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(text = title) }
                    )
                }
            }

            when (tabIndex) {
                0 -> DynamicBroadcastTab()
                1 -> StaticBroadcastTab()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("日志输出", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    BroadcastLog.clear()
                    logMessages.clear()
                    BroadcastLog.add("系统", "清空日志", "操作成功")
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "清空日志", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (logMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无日志", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        reverseLayout = true
                    ) {
                        items(logMessages) { msg ->
                            Text(text = msg, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicBroadcastTab() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("动态注册的广播接收器", style = MaterialTheme.typography.titleLarge)
        Text(
            "这种广播的生命周期与当前界面严格绑定。它通过代码动态注册，非常适合执行与UI相关的、即时的前台任务。当您离开此页面时，它会被自动注销。",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = {
                context.sendInternalBroadcast(BroadcastLog.DYNAMIC_ACTION) {
                    putExtra("data", "这是一条前台 UI 相关的广播")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("发送动态广播 (仅本页面能收到)")
        }
    }
}

/**
 * 静态广播 Tab，包含可展开/收起的测试说明
 */
@Composable
private fun StaticBroadcastTab() {
    val context = LocalContext.current
    var isInstructionsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("静态注册的广播接收器", style = MaterialTheme.typography.titleLarge)
        Text(
            "这是插件化框架的核心能力之一。接收器在插件的清单文件(AndroidManifest)中声明，由框架的代理机制唤醒，即使应用处于后台也能响应系统事件。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Button(
            onClick = {
                context.sendInternalBroadcast(BroadcastLog.STATIC_ACTION) {
                    putExtra("data", "发往后台的业务广播")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("发送自定义静态广播")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // 移除点击时的波纹效果
                ) { isInstructionsExpanded = !isInstructionsExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("如何测试系统广播", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            val rotationAngle by animateFloatAsState(targetValue = if (isInstructionsExpanded) 180f else 0f, label = "rotation")
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "展开/收起",
                modifier = Modifier.rotate(rotationAngle)
            )
        }

        AnimatedVisibility(visible = isInstructionsExpanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("静态接收器能响应系统事件。请尝试以下操作并观察日志输出：", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("1.在系统快捷设置中开关蓝牙", style = MaterialTheme.typography.bodyMedium)
                Text("2.插入或拔出有线耳机", style = MaterialTheme.typography.bodyMedium)
                Text("3.去系统设置更改设备语言", style = MaterialTheme.typography.bodyMedium)
                Text("4.重启设备测试 BOOT_COMPLETED(需自启动权限)", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}