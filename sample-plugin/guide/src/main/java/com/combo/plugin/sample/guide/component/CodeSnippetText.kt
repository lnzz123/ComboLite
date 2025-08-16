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
fun CodeSnippetText(
    text: String,
    color: Color,
) {
    Text(
        buildAnnotatedString {
            val parts = text.split("`")
            withStyle(style = SpanStyle(color = color, fontFamily = FontFamily.Default)) {
                append(parts[0])
            }
            if (parts.size > 1) {
                withStyle(
                    style =
                        SpanStyle(
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                            background = color.copy(alpha = 0.1f),
                        ),
                ) {
                    append(" ${parts[1]} ")
                }
                append(parts[2])
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp),
        lineHeight = 20.sp,
    )
}