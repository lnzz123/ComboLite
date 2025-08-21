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

package com.combo.plugin.sample.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.combo.core.base.BasePluginActivity
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LifecycleActivity : BasePluginActivity() {

    private val lifecycleEvents = mutableStateListOf<String>()
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private fun addEvent(event: String) {
        val timestamp = timeFormatter.format(Date())
        val logMessage = "[$timestamp] $event"
        lifecycleEvents.add(0, logMessage)
        Timber.d(logMessage)
    }

    // 覆盖所有生命周期方法
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addEvent("onCreate")
        proxyActivity?.setContent {
            LifecycleScreen(lifecycleEvents)
        }
    }

    override fun onStart() {
        super.onStart()
        addEvent("onStart")
    }

    override fun onResume() {
        super.onResume()
        addEvent("onResume")
    }

    override fun onPause() {
        super.onPause()
        addEvent("onPause")
    }

    override fun onStop() {
        super.onStop()
        addEvent("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        addEvent("onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        addEvent("onRestart")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LifecycleScreen(events: List<String>) {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("生命周期监听", fontWeight = FontWeight.Bold) })
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    "观察下方的日志输出。可以尝试按 Home 键、返回键或旋转屏幕来触发不同的生命周期事件。",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 当列表更新时，自动滚动到顶部
                LaunchedEffect(events) {
                    coroutineScope.launch {
                        if (events.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.LightGray.copy(alpha = 0.2f)),
                    reverseLayout = false
                ) {
                    items(events) { event ->
                        Text(
                            text = event,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

