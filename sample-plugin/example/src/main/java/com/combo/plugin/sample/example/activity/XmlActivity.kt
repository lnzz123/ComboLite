package com.combo.plugin.sample.example.activity

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.combo.core.base.BasePluginActivity
import com.combo.plugin.sample.example.R

/**
 * XML UI 布局示例 Activity
 * 展示如何在插件中使用传统的 XML 布局和 findViewById
 */
class XmlActivity : BasePluginActivity() {
    private lateinit var button: Button
    private lateinit var textView: TextView
    private lateinit var imageView: ImageView
    private lateinit var switch: Switch
    private lateinit var seekBar: SeekBar
    private lateinit var progressBar: ProgressBar
    private lateinit var ratingBar: RatingBar
    private lateinit var checkBox: CheckBox
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioButton: RadioButton
    private lateinit var radioButton1: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用代理 Activity 设置布局
        proxyActivity?.setContentView(R.layout.activity_xml)

        // 初始化 UI 元素
        button = proxyActivity?.findViewById(R.id.button_first)!!
        textView = proxyActivity?.findViewById(R.id.textview_first)!!
        imageView = proxyActivity?.findViewById(R.id.imageView)!!
        switch = proxyActivity?.findViewById(R.id.switch_first)!!
        seekBar = proxyActivity?.findViewById(R.id.seekBar_first)!!
        progressBar = proxyActivity?.findViewById(R.id.progressBar_first)!!
        ratingBar = proxyActivity?.findViewById(R.id.ratingBar_first)!!
        checkBox = proxyActivity?.findViewById(R.id.checkBox_first)!!
        radioGroup = proxyActivity?.findViewById(R.id.radioGroup_first)!!
        radioButton = proxyActivity?.findViewById(R.id.radioButton_first)!!
        radioButton1 = proxyActivity?.findViewById(R.id.radioButton_second)!!


        // 为按钮添加点击事件
        button.setOnClickListener {
            // 显示 Toast 消息
            Toast.makeText(proxyActivity, "这是一个由插件Activity加载的 XML 布局页面", Toast.LENGTH_SHORT).show()
        }

        // 为复选框添加点击事件
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            // 显示 Toast 消息
            Toast.makeText(proxyActivity, "复选框状态：$isChecked", Toast.LENGTH_SHORT).show()
        }

        // 为单选框添加点击事件
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            // 显示 Toast 消息
            Toast.makeText(proxyActivity, "单选框状态：$checkedId", Toast.LENGTH_SHORT).show()
        }

        // 为进度条添加点击事件
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 显示 Toast 消息
                Toast.makeText(proxyActivity, "进度条状态：$progress", Toast.LENGTH_SHORT).show()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })


        // 为评分条添加点击事件
        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            // 显示 Toast 消息
            Toast.makeText(proxyActivity, "评分条状态：$rating", Toast.LENGTH_SHORT).show()
        }

        // 为开关添加点击事件
        switch.setOnCheckedChangeListener { _, isChecked ->
            // 显示 Toast 消息
            Toast.makeText(proxyActivity, "开关状态：$isChecked", Toast.LENGTH_SHORT).show()
        }

        // 为图片添加点击事件
        imageView.setOnClickListener {
            proxyActivity?.finish()
        }
    }
}