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
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 核心特性卡片
 */
@Composable
fun CoreFeaturesCard() {
    val features =
        listOf(
            FeatureItem(
                Icons.Rounded.Done,
                "0 Hook & 0 反射",
                "完全基于官方 API，确保了极致的稳定性与未来的系统兼容性。",
            ),
            FeatureItem(
                Icons.Rounded.Done,
                "去中心化设计",
                "插件亦是管理者，可主动管理自身或其他插件的下载、安装和更新。",
            ),
            FeatureItem(
                Icons.Rounded.Done,
                "极简接入",
                "只需继承框架预定义的基类，即可零成本完成初始化与集成。",
            ),
            FeatureItem(
                Icons.Rounded.Done,
                "全功能插件化",
                "完美支持四大组件、依赖注入(Koin)，支持宿主零逻辑。",
            ),
            FeatureItem(
                Icons.Rounded.Done,
                "模块即插件",
                "创新的设计理念，支持将标准 AAR 无缝转换为插件 APK。",
            ),
            FeatureItem(
                Icons.Rounded.Done,
                "灵活依赖",
                "支持插件之间相互依赖，轻松构建复杂的“超级应用”架构。",
            ),
        )

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Build,
                    contentDescription = "核心特性",
                    tint = MaterialTheme.colorScheme.secondary, // 使用 secondary 增加色彩变化
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "核心特性",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary, // 与图标颜色保持一致
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
 * 特性行组件
 */
@Composable
fun FeatureRow(feature: FeatureItem) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            feature.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier =
                Modifier
                    .size(20.dp)
                    .padding(top = 3.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                feature.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                feature.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )
        }
    }
}





/**
 * 特性数据类 (保持不变)
 */
data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
)


