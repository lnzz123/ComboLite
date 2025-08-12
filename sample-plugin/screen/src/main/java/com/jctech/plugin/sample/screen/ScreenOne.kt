package com.jctech.plugin.sample.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jctech.plugin.core.ext.startPluginActivity
import com.jctech.plugin.core.ext.startPluginService
import com.jctech.plugin.core.ext.stopPluginService
import com.jctech.plugin.sample.common.navigation.AppScreen
import com.jctech.plugin.sample.common.navigation.currentComposeNavigator

/**
 * Screen One 页面
 * 
 * 这是一个测试页面，展示基本的UI组件和导航功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenOne() {
    val navigator = currentComposeNavigator
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen One") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Text("←")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "欢迎来到 Screen One",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "这是第一个测试屏幕",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { navigator.navigate(AppScreen.ScreenTwo) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("前往 Screen Two")
            }
            
            OutlinedButton(
                onClick = {
                    context.startPluginActivity(TestPluginActivity::class.java)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("跳转Activity")
            }

            OutlinedButton(
                onClick = {
                    context.startPluginService(TestPluginService::class.java)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("启动插件服务")
            }

            OutlinedButton(
                onClick = {
                    context.stopPluginService(TestPluginService::class.java)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("关闭插件服务")
            }


            Spacer(modifier = Modifier.weight(1f))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "功能说明",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 测试页面导航\n• 展示UI组件\n• 验证插件功能",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}