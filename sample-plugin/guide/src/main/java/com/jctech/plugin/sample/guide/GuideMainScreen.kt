/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利.
 */

package com.jctech.plugin.sample.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jctech.plugin.sample.common.component.NetworkImage

/**
 * 插件化框架使用指南主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun GuideMainScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "插件化框架指南",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 框架介绍卡片
            item {
                IntroductionCard()
            }

            // 核心特性列表
            item {
                CoreFeaturesCard()
            }

            // 快速开始指南
            item {
                QuickStartCard()
            }

            // 技术架构
            item {
                ArchitectureCard()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 框架介绍卡片 (颜色美化)
 */
@Composable
fun IntroductionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = "框架介绍",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "框架介绍",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // 使用主题色强调标题
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val tags = listOf(
                "https://img.shields.io/badge/Platform-Android-3DDC84.svg",
                "https://img.shields.io/badge/API-24%2B%20(Android%207.0)-blue.svg",
                "https://img.shields.io/badge/Kotlin-2.3.10-7F52FF.svg",
                "https://img.shields.io/badge/Compose-2025.08.00-FF6F00.svg",
                "https://img.shields.io/badge/AGP-9.2.0-007BFF.svg",
                "https://img.shields.io/badge/Gradle-9.5-6C757D.svg",
                "https://img.shields.io/badge/License-Commercial-red.svg",
                "https://img.shields.io/badge/GitHub-Repo-181717.svg"
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tags.forEach { tag ->
                    NetworkImage(
                        model = tag,
                        modifier = Modifier.height(20.dp),
                        contentDescription = null
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "一个为下一代 Android 应用而生的现代化插件化框架，专为 Jetpack Compose 深度优化，致力于提供极致的稳定性和无与伦比的开发体验。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface // 主要描述使用 onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "我们的核心设计哲学是 “0 Hook, 0 反射”。通过完全拥抱 Google 官方推荐的 API，我们从根本上解决了传统插件化框架长期存在的兼容性与稳定性痛点。这使得我们的框架：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // 次要描述使用 onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 使用 AnnotatedString 高亮关键词
            val bulletPoints = buildAnnotatedString {
                val bullet = "• "
                val points = mapOf(
                    "面向未来：" to "无惧 Android 系统版本升级，理论支持 Android 7-16+。\n",
                    "绝对稳定：" to "无任何非公开 API 调用，应用运行如原生般可靠。\n",
                    "性能卓越：" to "无反射调用带来的性能损耗，启动和运行速度更快。\n",
                    "维护轻松：" to "紧跟最新 Android Studio, Gradle 和 AGP，无历史包袱。"
                )
                points.forEach { (keyword, description) ->
                    withStyle(style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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


/**
 * 核心特性卡片 (内容优化)
 */
@Composable
fun CoreFeaturesCard() {
    val features = listOf(
        FeatureItem(Icons.Rounded.Done, "0 Hook & 0 反射", "完全基于官方 API，确保了极致的稳定性与未来的系统兼容性。"),
        FeatureItem(Icons.Rounded.Done, "去中心化设计", "插件亦是管理者，可主动管理自身或其他插件的下载、安装和更新。"),
        FeatureItem(Icons.Rounded.Done, "极简接入", "只需继承框架预定义的基类，即可零成本完成初始化与集成。"),
        FeatureItem(Icons.Rounded.Done, "全功能插件化", "完美支持四大组件、依赖注入(Koin)，支持宿主零逻辑。"),
        FeatureItem(Icons.Rounded.Done, "模块即插件", "创新的设计理念，支持将标准 AAR 无缝转换为插件 APK。"),
        FeatureItem(Icons.Rounded.Done, "灵活依赖", "支持插件之间相互依赖，轻松构建复杂的“超级应用”架构。")
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Build,
                    contentDescription = "核心特性",
                    tint = MaterialTheme.colorScheme.secondary, // 使用 secondary 增加色彩变化
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "核心特性",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary // 与图标颜色保持一致
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            features.forEach { feature ->
                FeatureRow(feature)
                if (feature != features.last()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * 特性行组件 (颜色美化)
 */
@Composable
fun FeatureRow(feature: FeatureItem) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            feature.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp).padding(top = 3.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                feature.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                feature.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}


/**
 * 快速开始卡片 (颜色美化)
 */
@Composable
fun QuickStartCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Send,
                    contentDescription = "快速开始",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "快速开始",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            val steps = listOf(
                "1. 添加框架依赖到 build.gradle.kts。",
                "2. Application 继承 `BaseHostApplication`。",
                "3. 宿主 Activity 继承 `BaseHostActivity`。",
                "4. 创建插件模块并配置 `AndroidManifest.xml` 元数据。",
                "5. 编译插件 APK 并通过管理器安装加载运行。"
            )
            steps.forEach { step ->
                // 高亮代码片段
                CodeSnippetText(text = step, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

/**
 * 技术架构卡片 (颜色美化)
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "技术架构",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val archText = buildAnnotatedString {
                val points = mapOf(
                    "核心管理器 (PluginManager):" to " 统一调度与管理插件生命周期。\n",
                    "类加载器 (PluginClassLoader):" to " 基于官方 API，安全隔离并加载插件代码。\n",
                    "资源加载器 (PluginResourcesLoader):" to " 高效加载插件资源，支持动态切换。\n",
                    "组件代理 (ComponentProxy):" to " 实现四大组件通信的透明代理层。\n",
                    "安装管理器 (InstallerManager):" to " 负责插件的解析、安装与版本管理。"
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

/**
 * 特性数据类 (保持不变)
 */
data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * 用于高亮代码片段的辅助 Composable
 */
@Composable
private fun CodeSnippetText(text: String, color: Color) {
    Text(
        buildAnnotatedString {
            val parts = text.split("`")
            withStyle(style = SpanStyle(color = color, fontFamily = FontFamily.Default)) {
                append(parts[0])
            }
            if (parts.size > 1) {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = color,
                        background = color.copy(alpha = 0.1f)
                    )
                ) {
                    append(" ${parts[1]} ")
                }
                append(parts[2])
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp),
        lineHeight = 20.sp
    )
}