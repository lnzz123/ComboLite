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

package com.combo.plugin.sample.guide.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 核心特性卡片
 */
@Composable
fun CoreFeaturesCard() {
    val pillars = mapOf(
        "现代化的设计" to listOf(
            FeatureItem(
                Icons.Rounded.Done,
                "原生为 Compose 而生",
                "为新一代UI工具包设计，插件可无缝使用 @Composable 函数构建界面。"
            ),
            FeatureItem(
                Icons.Rounded.Done,
                "拥抱主流技术栈",
                "完美集成 Kotlin Coroutines、Koin 等，让你能使用最前沿的技术。"
            ),
        ),
        "极致的稳定与兼容" to listOf(
            FeatureItem(
                Icons.Rounded.Done,
                "0 Hook, 原则上 0 反射",
                "完全基于官方API，仅在兼容旧版系统时采纳标准反射方案，确保极致稳定。"
            ),
            FeatureItem(
                Icons.Rounded.Done,
                "崩溃熔断与自愈",
                "内置崩溃处理器，能自动禁用问题插件并优雅降级，防止应用重复闪退。"
            ),
        ),
        "终极的灵活与解耦" to listOf(
            FeatureItem(
                Icons.Rounded.Done,
                "去中心化架构",
                "打破传统宿主-插件模式，任何插件都可管理其他插件，架构更灵活。"
            ),
            FeatureItem(
                Icons.Rounded.Done,
                "“空壳”宿主支持",
                "宿主可无任何业务逻辑，所有功能、UI 均由插件动态提供。"
            ),
        ),
        "卓越的开发者体验" to listOf(
            FeatureItem(
                Icons.Rounded.Done,
                "微乎其微的侵入性",
                "通过基类和数行配置即可完成集成，无需改动项目原有结构。"
            ),
            FeatureItem(
                Icons.Rounded.Done,
                "闪电般的类查找",
                "通过全局类索引机制，实现 O(1) 复杂度的跨插件类查找，性能卓越。"
            ),
        )
    )

    GuideSectionCard(
        title = "核心理念与优势",
        icon = Icons.Rounded.Build,
        iconTint = MaterialTheme.colorScheme.secondary
    ) {
        pillars.entries.forEachIndexed { index, entry ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(20.dp))
            }
            Text(
                text = entry.key,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            entry.value.forEach { feature ->
                FeatureRow(feature)
                if (feature != entry.value.last()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
