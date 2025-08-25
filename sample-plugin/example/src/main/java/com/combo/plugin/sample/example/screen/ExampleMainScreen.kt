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

package com.combo.plugin.sample.example.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.combo.plugin.sample.common.navigation.AppScreen
import com.combo.plugin.sample.common.navigation.currentComposeNavigator
import com.combo.plugin.sample.example.component.ExampleItemGridCard


/**
 * 示例数据类，表示一个示例项目
 * @param title 示例标题
 * @param description 示例说明
 */
data class ExampleItem(
    val title: String,
    val description: String,
    val onClick: () -> Unit = {},
)

/**
 * 示例插件主界面 (纯文字网格布局版)
 * 展示各种 Android 组件和功能的示例列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ExampleMainScreen() {
    val navigator = currentComposeNavigator
    // 更新数据列表，移除图标
    val exampleItems = listOf(
        ExampleItem("Activity 示例", "生命周期、启动模式、Intent 传递等") {
            navigator.navigate(AppScreen.PluginActivity)
        },
        ExampleItem("Service 示例", "前后台服务、绑定服务等不同类型") {
            navigator.navigate(AppScreen.PluginService)
        },
        ExampleItem("广播接收器", "系统广播、自定义广播的发送与接收") {
            navigator.navigate(AppScreen.BroadcastReceiver)
        },
        ExampleItem("内容提供者", "数据共享、权限控制、CRUD 操作") {
            navigator.navigate(AppScreen.ContentProvider)
        },
        ExampleItem("SO 库加载", "JNI 调用、动态库加载、native 方法") {
            navigator.navigate(AppScreen.SoLibrary)
        },
        ExampleItem("皮肤资源替换", "动态换肤、资源替换、主题切换"),
        ExampleItem("权限申请", "运行时权限申请、权限检查、权限说明"),
        ExampleItem("悬浮窗示例", "展示如何在插件中创建和管理悬浮窗")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "插件示例集合",
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部说明卡片
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "探索 Android 开发的各个方面，从基础组件到高级功能，每个示例都包含详细的代码演示和说明。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 示例卡片网格
            items(exampleItems) { item ->
                ExampleItemGridCard(item = item)
            }
        }
    }
}

