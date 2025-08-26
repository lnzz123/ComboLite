/*
 * Copyright (c) 2025, 贵州君城网络科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.combo.plugin.sample.example.jni

/**
 * Native库接口类
 * 提供JNI方法调用接口，用于与C++代码交互
 */
class NativeLib {

    /**
     * 获取C++返回的字符串
     * @return C++中生成的Hello字符串
     */
    external fun stringFromJNI(): String

    /**
     * 计算两个整数的和
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 两个整数的和
     */
    external fun addNumbers(a: Int, b: Int): Int

    /**
     * 计算数字的平方根
     * @param number 要计算平方根的数字
     * @return 平方根结果
     */
    external fun calculateSquareRoot(number: Double): Double

    /**
     * 处理字符串数组
     * @param stringArray 字符串数组
     * @return 处理后的连接字符串
     */
    external fun processStringArray(stringArray: Array<String>): String

    /**
     * 获取系统信息
     * @return 包含编译器、架构等信息的字符串
     */
    external fun getSystemInfo(): String

    companion object {
        // 在类加载时自动加载native库
        init {
            System.loadLibrary("nativelib")
        }
    }
}