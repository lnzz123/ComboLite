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

package com.combo.plugin.sample.example.activity

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import com.combo.core.base.BasePluginActivity
import com.combo.plugin.sample.example.databinding.ActivityXmlBinding

class XmlActivity : BasePluginActivity() {

    private lateinit var binding: ActivityXmlBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityXmlBinding.inflate(proxyActivity!!.layoutInflater)
        proxyActivity?.setContentView(binding.root)

        setupTitleBar()
        setupEventListeners()
    }

    private fun setupTitleBar() {
        binding.ivBack.setOnClickListener {
            proxyActivity?.finish()
        }
    }

    private fun setupEventListeners() {
        binding.buttonShowToast.setOnClickListener {
            val inputText = binding.editText.text.toString()
            val message =
                if (inputText.isNotBlank()) "输入内容为: $inputText" else "您没有输入任何内容"
            showToast(message)
            updateFeedbackText("点击了按钮")
        }

        binding.controlSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.editText.isEnabled = isChecked
            val status = if (isChecked) "启用" else "禁用"
            updateFeedbackText("开关: 输入框已 $status")
        }

        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            updateFeedbackText("复选框: 状态变为 $isChecked")
        }

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedText = if (checkedId == binding.radioButton1.id) "选项A" else "选项B"
            updateFeedbackText("单选组: 选择了 $selectedText")
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.progressBar.progress = progress
                if (fromUser) {
                    updateFeedbackText("SeekBar 拖动中: $progress%", false)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val finalProgress = seekBar?.progress ?: 0
                showToast("最终进度: $finalProgress%")
                updateFeedbackText("SeekBar 最终进度: $finalProgress%")
            }
        })

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                updateFeedbackText("评分条: 选择了 $rating 星")
            }
        }
    }

    /**
     * 更新顶部的反馈文本
     * @param message 要显示的消息
     * @param alsoToast 是否同时显示一个 Toast (默认为 false)
     */
    private fun updateFeedbackText(message: String, alsoToast: Boolean = false) {
        binding.tvFeedback.text = message
        if (alsoToast) {
            showToast(message)
        }
    }

    /**
     * 统一的 Toast 显示方法
     */
    private fun showToast(message: String) {
        Toast.makeText(proxyActivity, message, Toast.LENGTH_SHORT).show()
    }
}