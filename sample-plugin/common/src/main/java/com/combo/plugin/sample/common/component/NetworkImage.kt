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

package com.combo.plugin.sample.common.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade


/**
 * 支持加载网络 SVG 和其他格式图片的基础组件 (优化版)
 *
 * @param model 图片数据，可以是 R.drawable.xxx，或者 网络url地址
 * @param modifier 修饰符
 * @param contentDescription 图片描述
 * @param contentScale 图片缩放模式
 */
@Composable
fun NetworkImage(
    // 名称可以更明确一些
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Fit,
) {
    // 使用更简洁的 AsyncImage 组件，它内部已经处理了占位图、错误图等状态
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalContext.current)
                .data(model)
                .crossfade(true)
                .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
