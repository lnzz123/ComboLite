/*
 * Copyright © 2025. 贵州君城网络科技有限公司 版权所有
 * 保留所有权利。
 *
 * 本软件（包括但不限于代码、文档、资源文件等）受《中华人民共和国著作权法》及相关法律法规保护。
 * 未经本公司书面授权，任何单位或个人不得：
 * 1. 以任何形式复制、传播、修改、分发本软件的全部或部分内容；
 * 2. 将本软件用于商业目的或未经授权的第三方项目；
 * 3. 删除或篡改本软件中的版权声明、商标标识及技术标识。
 *
 * 违反上述条款者，本公司将依法追究其民事及刑事责任，并有权要求赔偿因此造成的全部经济损失。
 *
 * 授权许可请联系：贵州君城网络科技有限公司法律事务部
 * 邮箱：1755858138@qq.com
 * 电话：+86-175-85074415
 */

package com.jctech.plugin.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PluginInfo(
    val pluginId: String,
    val version: String,
    val path: String,
    val entryClass: String,
    var description: String,
    val status: PluginState,
    val installTime: Long,
    val staticReceivers: List<StaticReceiverInfo> = emptyList()
)

/**
 * 描述一个静态广播接收器的信息
 * @param className 接收器的完整类名
 * @param actions 它监听的所有广播动作 (action)
 */
@Serializable
data class StaticReceiverInfo(
    val className: String,
    val actions: List<String>
)

@Serializable
enum class PluginState {
    Enabled,
    Disabled
}
