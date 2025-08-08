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

package com.jctech.plugin.core.ext

import timber.log.Timber

fun <T> ClassLoader.getInterface(interfaceClass: Class<T>, className: String): T? {
    return try {
        // 加载指定的实现类
        val clazz = loadClass(className)

        // 使用默认构造函数创建实例
        val instance = clazz.getDeclaredConstructor().newInstance()

        // 验证实例是否实现了指定接口
        if (interfaceClass.isInstance(instance)) {
            @Suppress("UNCHECKED_CAST")
            instance as T
        } else {
            Timber.e("类 $className 未实现接口 ${interfaceClass.name}")
            null
        }
    } catch (e: Exception) {
        Timber.e(e, "加载接口实现类失败: $className")
        null
    }
}
