/*
 * Copyright (c) 2025. 深圳市德为信息技术有限公司, 深圳市诠云科技有限公司 保留所有权利。
 *
 * 根据《中华人民共和国著作权法》、《计算机软件保护条例》及相关法律法规，
 * 本软件著作权归属于深圳市德为信息技术有限公司与深圳市诠云科技有限公司共同所有，
 * 任何单位或个人未经书面授权不得复制、修改、分发或用于商业用途。
 * （本声明适用于本项目所有源代码、资源配置文件及文档资料）
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
 * 支持加载本地和网络资源的基础图片组件
 *
 * @param model 图片数据，可以是 R.drawable.xxx， 或者 网络url地址
 * @param modifier 修饰符
 * @param contentDescription 图片描述
 * @param contentScale 图片缩放模式
 * @param placeholder 加载时的占位图，例如：painterResource(id = R.drawable.loading)
 * @param error 加载失败时的占位图，例如：painterResource(id = R.drawable.error)
 * @param alpha 图片透明度
 * @param colorFilter 图片颜色过滤器
 */

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
