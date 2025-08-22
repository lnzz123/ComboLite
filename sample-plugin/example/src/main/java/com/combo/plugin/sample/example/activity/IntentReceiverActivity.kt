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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.combo.core.base.BasePluginActivity

class IntentReceiverActivity : BasePluginActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 从 Intent 中提取数据
        val receivedString = proxyActivity?.intent?.getStringExtra("EXTRA_STRING") ?: "未收到字符串"
        val receivedInt = proxyActivity?.intent?.getIntExtra("EXTRA_INT", - 1) ?: - 1
        val receivedBoolean =
            proxyActivity?.intent?.getBooleanExtra("EXTRA_BOOLEAN", false) ?: false

        proxyActivity?.setContent {
            IntentReceiverScreen(
                strValue = receivedString,
                intValue = receivedInt,
                boolValue = receivedBoolean
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun IntentReceiverScreen(strValue: String, intValue: Int, boolValue: Boolean) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Intent 数据接收", fontWeight = FontWeight.Bold) })
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "已成功从 Intent 中接收到以下数据：",
                    style = MaterialTheme.typography.titleMedium
                )

                InfoRow(label = "字符串 (EXTRA_STRING):", value = strValue)
                InfoRow(label = "整数 (EXTRA_INT):", value = intValue.toString())
                InfoRow(label = "布尔值 (EXTRA_BOOLEAN):", value = boolValue.toString())
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
