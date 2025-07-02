package com.github.flexfloor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.github.flexfloor.databinding.ActivityMainBinding

/**
 * 主活动 - FlexFloor 样例入口页面
 * 展示项目介绍和功能导航
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 使用ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化UI
        initializeUI()
    }
    
    /**
     * 初始化UI组件
     */
    private fun initializeUI() {
        // 设置Toolbar
        binding.toolbar.title = "FlexFloor 楼层化框架"
        
        // 设置版本信息
        binding.versionText.text = "Version 1.0.0"
        
        // 设置楼层演示按钮点击事件
        binding.floorDemoButton.setOnClickListener {
            startFloorDemo()
        }
        
        // 设置关于按钮点击事件
        binding.aboutButton.setOnClickListener {
            showAboutDialog()
        }
    }
    
    /**
     * 启动楼层演示页面
     */
    private fun startFloorDemo() {
        val intent = Intent(this, FloorDemoActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
        dialogBuilder.setTitle("关于 FlexFloor")
        dialogBuilder.setMessage("""
            FlexFloor 是一个强大的楼层化页面架构框架
            
            主要特性：
            • 模块化楼层设计
            • 支持多种楼层类型
            • 动态配置和加载
            • 楼层复用和缓存
            • MVI 架构模式
            • 完整的生命周期管理
            
            开发者：FlexFloor Team
            许可证：Apache 2.0
        """.trimIndent())
        dialogBuilder.setPositiveButton("确定") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
    }
}