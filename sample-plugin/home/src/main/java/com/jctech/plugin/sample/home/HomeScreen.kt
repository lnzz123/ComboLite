package com.jctech.plugin.sample.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jctech.plugin.sample.common.navigation.AppScreen
import com.jctech.plugin.sample.common.navigation.currentComposeNavigator

/**
 * 主页屏幕
 * 
 * 提供插件测试功能的主界面，包含导航、插件管理等功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val navigator = currentComposeNavigator
    var showDialog by remember { mutableStateOf(false) }
    var dialogType by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Sample Plugin 主页",
                        fontWeight = FontWeight.Bold
                    ) 
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 欢迎卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚀",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "欢迎使用 Sample Plugin",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "插件功能测试中心",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // 页面导航区域
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📱 页面导航测试",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { navigator.navigate(AppScreen.ScreenOne) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Screen One")
                        }
                        
                        Button(
                            onClick = { navigator.navigate(AppScreen.ScreenTwo) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Screen Two")
                        }
                    }
                }
            }
            
            // 插件管理区域
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔧 插件管理功能",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                dialogType = "update"
                                showDialog = true 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔄 检查插件更新")
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                dialogType = "install"
                                showDialog = true 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📦 安装新插件")
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                dialogType = "uninstall"
                                showDialog = true 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("🗑️ 卸载插件")
                        }
                    }
                }
            }
            
            // 插件间跳转测试
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔗 插件间跳转测试",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                dialogType = "jump_plugin_a"
                                showDialog = true 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("➡️ 跳转到插件 A")
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                dialogType = "jump_plugin_b"
                                showDialog = true 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("➡️ 跳转到插件 B")
                        }
                    }
                }
            }
            
            // 系统信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ 系统信息",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• 插件版本: 1.0.0\n• 框架版本: 2.0.0\n• 构建时间: 2025-01-27",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // 对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    when (dialogType) {
                        "update" -> "检查更新"
                        "install" -> "安装插件"
                        "uninstall" -> "卸载插件"
                        "jump_plugin_a" -> "跳转插件 A"
                        "jump_plugin_b" -> "跳转插件 B"
                        else -> "提示"
                    }
                )
            },
            text = {
                Text(
                    when (dialogType) {
                        "update" -> "正在检查插件更新...\n\n当前版本: 1.0.0\n最新版本: 1.0.0\n\n您的插件已是最新版本！"
                        "install" -> "插件安装功能演示\n\n这里将显示可安装的插件列表，用户可以选择需要的插件进行安装。"
                        "uninstall" -> "确定要卸载当前插件吗？\n\n⚠️ 卸载后将无法使用插件功能，需要重新安装。"
                        "jump_plugin_a" -> "即将跳转到插件 A\n\n这是插件间跳转功能的演示，实际使用时会跳转到指定的插件页面。"
                        "jump_plugin_b" -> "即将跳转到插件 B\n\n这是插件间跳转功能的演示，实际使用时会跳转到指定的插件页面。"
                        else -> "操作完成"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text(
                        if (dialogType == "uninstall") "确认卸载" else "确定",
                        color = if (dialogType == "uninstall") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = if (dialogType == "uninstall") {
                {
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            } else null
        )
    }
}
