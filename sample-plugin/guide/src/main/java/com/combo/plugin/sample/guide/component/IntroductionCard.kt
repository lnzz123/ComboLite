package com.combo.plugin.sample.guide.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.combo.plugin.sample.common.component.NetworkImage

/**
 * 框架介绍卡片
 */
@Composable
fun IntroductionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = "框架介绍",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "框架介绍",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, // 使用主题色强调标题
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val tags =
                listOf(
                    "https://img.shields.io/badge/Platform-Android-3DDC84.svg",
                    "https://img.shields.io/badge/API-24%2B%20(Android%207.0)-blue.svg",
                    "https://img.shields.io/badge/Kotlin-2.2.0-7F52FF.svg",
                    "https://img.shields.io/badge/Compose-1.9.0-FF6F00.svg",
                    "https://img.shields.io/badge/AGP-8.12.0-007BFF.svg",
                    "https://img.shields.io/badge/Gradle-8.13-6C757D.svg",
                    "https://img.shields.io/badge/License-Commercial-red.svg",
                    "https://img.shields.io/badge/GitHub-lnzz123-181717.svg",
                )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                tags.forEach { tag ->
                    NetworkImage(
                        model = tag,
                        modifier = Modifier.height(20.dp),
                        contentDescription = null,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "一个为现代 Android 应用而生的现代化插件化框架，专为 Jetpack Compose 深度优化，致力于提供极致的稳定性和无与伦比的开发体验。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "我们的核心设计哲学是 “0 Hook, 0 反射”。通过完全拥抱 Google 官方推荐的 API，我们从根本上解决了传统插件化框架长期存在的兼容性与稳定性痛点。这使得我们的框架：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, // 次要描述使用 onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 使用 AnnotatedString 高亮关键词
            val bulletPoints =
                buildAnnotatedString {
                    val bullet = "• "
                    val points =
                        mapOf(
                            "面向未来：" to "无惧 Android 系统版本升级，理论支持 Android 7-16+。\n",
                            "绝对稳定：" to "无任何非公开 API 调用，应用运行如原生般可靠。\n",
                            "性能卓越：" to "无反射调用带来的性能损耗，启动和运行速度更快。\n",
                            "维护轻松：" to "紧跟最新 Android Studio, Gradle 和 AGP，无历史包袱。",
                        )
                    points.forEach { (keyword, description) ->
                        withStyle(
                            style =
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                ),
                        ) {
                            append(bullet)
                            append(keyword)
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(description)
                        }
                    }
                }
            Text(bulletPoints, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
    }
}