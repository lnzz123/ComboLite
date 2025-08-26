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

package com.combo.plugin.sample.example.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.combo.core.ext.startPluginActivity
import com.combo.plugin.sample.example.activity.ComposeActivity
import com.combo.plugin.sample.example.activity.IntentSenderActivity
import com.combo.plugin.sample.example.activity.LifecycleActivity
import com.combo.plugin.sample.example.activity.XmlActivity
import com.combo.plugin.sample.example.component.JumpButton


/**
 * Activity示例展示页面
 * 用于展示插件Activity的各种使用场景和功能示例
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ActivityScreen() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Activity示例",
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            JumpButton(text = "Compose 函数示例") {
                context.startPluginActivity(ComposeActivity::class.java)
            }
            JumpButton(text = "XML UI 布局示例") {
                context.startPluginActivity(XmlActivity::class.java)
            }
            JumpButton(text = "生命周期示例") {
                context.startPluginActivity(LifecycleActivity::class.java)
            }
            JumpButton(text = "Intent 传递示例") {
                context.startPluginActivity(IntentSenderActivity::class.java)
            }
        }
    }
}
