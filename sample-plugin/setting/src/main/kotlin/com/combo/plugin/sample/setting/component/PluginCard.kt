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

package com.combo.plugin.sample.setting.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.combo.core.manager.PluginManager
import com.combo.core.model.PluginInfo

/**
 * 单个插件的管理卡片
 * @param plugin 插件信息
 * @param isRunning 插件当前是否在运行
 * @param onEnableChange “自启动”开关状态改变回调
 * @param onLaunch 启动插件回调
 * @param onClose 关闭插件回调
 * @param onUninstall 卸载插件回调
 */
@Composable
fun PluginCard(
    plugin: PluginInfo,
    isRunning: Boolean,
    onEnableChange: (Boolean) -> Unit,
    onLaunch: () -> Unit,
    onClose: () -> Unit,
    onUninstall: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showCloseWarningDialog by remember { mutableStateOf(false) }

    val dependents = remember(plugin.pluginId) {
        PluginManager.getPluginDependentsChain(plugin.pluginId)
    }

    if (showCloseWarningDialog) {
        ClosePluginWarningDialog(
            dependents = dependents,
            onConfirm = {
                onClose()
                showCloseWarningDialog = false
            },
            onDismiss = {
                showCloseWarningDialog = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded }
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 左侧：插件信息和状态
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 插件ID和自启动开关
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = plugin.pluginId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            //省略中间部分
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis

                        )
                        Spacer(Modifier.width(8.dp))
                        ChipToggleButton(
                            isSelected = plugin.enabled,
                            onSelectedChange = onEnableChange
                        )
                    }
                    // 插件运行状态指示器
                    PluginStatusIndicator(isRunning = isRunning)
                }

                // 右侧：更多菜单
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (isRunning) {
                            DropdownMenuItem(
                                text = { Text("关闭", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    // 点击关闭时，先显示警告对话框
                                    showCloseWarningDialog = true
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "关闭",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("启动") },
                                onClick = {
                                    onLaunch()
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = "启动"
                                    )
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("卸载", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onUninstall()
                                menuExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "卸载",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            // --- 详情区 (Content)，带动画效果 ---
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = plugin.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "版本: ${plugin.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
/**
 * 关闭插件风险警告对话框
 * @param dependents 依赖于当前插件的其他插件列表
 * @param onConfirm 用户确认关闭
 * @param onDismiss 用户取消
 */
@Composable
private fun ClosePluginWarningDialog(
    dependents: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = "风险警告"
    val message = if (dependents.isEmpty()) {
        "您确定要立即关闭此插件吗？这会立即停止其所有活动。"
    } else {
        "关闭此插件可能会影响以下 ${dependents.size} 个插件的正常运行，甚至导致它们崩溃：\n\n${dependents.joinToString("\n")}\n\n您确定要继续吗？"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text("确定关闭", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 插件运行状态指示器 Composable
 */
@Composable
private fun PluginStatusIndicator(isRunning: Boolean) {
    val statusColor = if (isRunning) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusText = if (isRunning) "运行中" else "未运行"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor
        )
    }
}