package com.combo.plugin.sample.guide.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 快速开始卡片
 */
@Composable
fun QuickStartCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Send,
                    contentDescription = "快速开始",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "快速开始",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            val steps =
                listOf(
                    "1. 添加框架依赖到 build.gradle.kts。",
                    "2. Application 继承 `BaseHostApplication`。",
                    "3. 宿主 Activity 继承 `BaseHostActivity`。",
                    "4. 创建插件模块并配置 `AndroidManifest.xml` 元数据。",
                    "5. 编译插件 APK 并通过管理器安装加载运行。",
                )
            steps.forEach { step ->
                // 高亮代码片段
                CodeSnippetText(text = step, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}