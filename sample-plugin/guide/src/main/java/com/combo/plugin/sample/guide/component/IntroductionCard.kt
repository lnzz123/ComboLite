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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.combo.plugin.sample.common.component.NetworkImage

/**
 * 框架介绍卡片
 */
@Composable
fun IntroductionCard() {
    GuideSectionCard(
        title = "框架介绍",
        icon = Icons.Rounded.Info,
        iconTint = MaterialTheme.colorScheme.primary,
    ) {
        val tags = listOf(
            "https://img.shields.io/badge/Platform-Android-3DDC84.svg",
            "https://img.shields.io/badge/API-24%2B%20(Android%207.0)-blue.svg",
            "https://img.shields.io/badge/Kotlin-2.2.0-7F52FF.svg",
            "https://img.shields.io/badge/Compose-1.9.0-FF6F00.svg",
            "https://img.shields.io/badge/AGP-8.12.0-007BFF.svg",
            "https://img.shields.io/badge/Gradle-8.13-6C757D.svg",
            "https://img.shields.io/badge/License-Apache%202.0-blue.svg",
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
            text = "随着 Android 生态的不断演进，众多诞生于 View 时代的经典插件化框架在如今的开发场景中已显得力不从心。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "ComboLite 的诞生，正是为了终结这一困境。它从源头上彻底抛弃了充满风险的非公开 API 反射调用，以完全公开的 API 为基石，实现了 0 Hook、0 反射 的纯净架构，原生为 Jetpack Compose 设计，并开创性地引入了去中心化的管理哲学。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}