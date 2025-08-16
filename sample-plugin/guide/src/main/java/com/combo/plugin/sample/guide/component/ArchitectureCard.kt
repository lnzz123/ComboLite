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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 技术架构卡片
 */
@Composable
fun ArchitectureCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "技术架构",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "技术架构",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val archText =
                buildAnnotatedString {
                    val points =
                        mapOf(
                            "核心管理器 (PluginManager):" to " 统一调度与管理插件生命周期。\n",
                            "类加载器 (PluginClassLoader):" to " 基于官方 API，安全隔离并加载插件代码。\n",
                            "资源加载器 (PluginResourcesLoader):" to " 高效加载插件资源，支持动态切换。\n",
                            "组件代理 (ComponentProxy):" to " 实现四大组件通信的透明代理层。\n",
                            "安装管理器 (InstallerManager):" to " 负责插件的解析、安装与版本管理。",
                        )
                    points.forEach { (keyword, description) ->
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                            append("• ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(keyword)
                            }
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(description)
                        }
                    }
                }
            Text(archText, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
    }
}