/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利.
 */

package com.combo.plugin.sample.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.combo.plugin.sample.guide.component.ArchitectureCard
import com.combo.plugin.sample.guide.component.CoreFeaturesCard
import com.combo.plugin.sample.guide.component.IntroductionCard
import com.combo.plugin.sample.guide.component.QuickStartCard

/**
 * 插件化框架使用指南主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun GuideMainScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ComboLite插件化框架指南",
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 框架介绍卡片
            item {
                IntroductionCard()
            }

            // 核心特性列表
            item {
                CoreFeaturesCard()
            }

            // 快速开始指南
            item {
                QuickStartCard()
            }

            // 技术架构
            item {
                ArchitectureCard()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}