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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 用于高亮代码片段的辅助 Composable
 */
@Composable
fun CodeSnippetText(text: String, color: Color) {
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
                        background = color.copy(alpha = 0.1f),
                    ),
                ) {
                    append(" ${parts[1]} ")
                }
                if (parts.size > 2) {
                    append(parts[2])
                }
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp),
        lineHeight = 20.sp,
    )
}