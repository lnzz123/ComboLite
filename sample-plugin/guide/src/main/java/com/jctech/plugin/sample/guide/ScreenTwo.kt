package com.jctech.plugin.sample.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jctech.plugin.sample.common.navigation.AppScreen
import com.jctech.plugin.sample.common.navigation.currentComposeNavigator

/**
 * Screen Two 页面
 *
 * 这是第二个测试页面，展示列表和交互功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTwo() {
    val navigator = currentComposeNavigator
    var counter by remember { mutableIntStateOf(0) }

    val testItems = remember {
        listOf(
            "测试项目 1",
            "测试项目 2",
            "测试项目 3",
            "测试项目 4",
            "测试项目 5"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Two") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Text("←")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { counter ++ }
            ) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "欢迎来到 Screen Two",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "这是第二个测试屏幕",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { navigator.navigate(AppScreen.ScreenOne) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("前往 Screen One")
                }

                OutlinedButton(
                    onClick = { navigator.navigate(AppScreen.Home) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("返回主页")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "计数器测试",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当前计数: $counter",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { counter ++ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+1")
                        }
                        OutlinedButton(
                            onClick = { counter = 0 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重置")
                        }
                    }
                }
            }

            Text(
                text = "测试列表",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(testItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { /* 点击事件 */ }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "→",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}