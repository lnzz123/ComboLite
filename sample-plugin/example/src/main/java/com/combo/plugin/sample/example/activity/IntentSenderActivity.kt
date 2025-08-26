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

package com.combo.plugin.sample.example.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.combo.core.base.BasePluginActivity
import com.combo.core.ext.startPluginActivity

class IntentSenderActivity : BasePluginActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        proxyActivity?.setContent {
            IntentSenderScreen(
                onSendData = { str, int, bool ->
                    // 启动 Intent 传递示例 Activity，并附带数据
                    proxyActivity?.startPluginActivity(IntentReceiverActivity::class.java) {
                        putExtra("EXTRA_STRING", str)
                        putExtra("EXTRA_INT", int)
                        putExtra("EXTRA_BOOLEAN", bool)
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun IntentSenderScreen(
        onSendData: (String, Int, Boolean) -> Unit
    ) {
        val context = LocalContext.current
        var textState by remember { mutableStateOf("来自插件的消息") }
        var numberState by remember { mutableStateOf("2025") }
        var switchState by remember { mutableStateOf(true) }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Intent 数据发送", fontWeight = FontWeight.Bold) })
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "请在下方输入或修改要传递的数据，然后点击发送按钮。",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                // 文本输入框
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text("要发送的字符串") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 数字输入框
                OutlinedTextField(
                    value = numberState,
                    onValueChange = { numberState = it },
                    label = { Text("要发送的整数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("要发送的布尔值")
                    Switch(
                        checked = switchState,
                        onCheckedChange = { switchState = it }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 发送按钮
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        val number = numberState.toIntOrNull()
                        if (number == null) {
                            Toast.makeText(context, "请输入一个有效的整数！", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            onSendData(textState, number, switchState)
                        }
                    }
                ) {
                    Text("发送数据到下一页")
                }
            }
        }
    }
}

