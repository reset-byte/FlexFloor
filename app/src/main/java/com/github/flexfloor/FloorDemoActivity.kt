package com.github.flexfloor

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.github.flexfloor.databinding.ActivityFloorDemoBinding
import com.github.flexfloor.floors.BannerFloor
import com.github.flexfloor.floors.ImageFloor
import com.github.flexfloor.floors.TextFloor
import com.github.flexfloorlib.core.FloorManager
import com.github.flexfloorlib.core.FloorFactory
import com.github.flexfloorlib.model.FloorType
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorConfig
import com.github.flexfloorlib.model.EdgeInsets

/**
 * 楼层化框架演示页面
 */
class FloorDemoActivity : ComponentActivity() {
    
    private lateinit var binding: ActivityFloorDemoBinding
    private lateinit var floorManager: FloorManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityFloorDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        registerFloorTypes()
        initFloorManager()
        setupUI()
        loadDemoFloors()
    }
    
    private fun registerFloorTypes() {
        FloorFactory.registerFloor(FloorType.TEXT) { TextFloor() }
        FloorFactory.registerFloor(FloorType.IMAGE) { ImageFloor() }
        FloorFactory.registerFloor(FloorType.BANNER) { BannerFloor() }
    }
    
    private fun initFloorManager() {
        floorManager = FloorManager.create(this, this)
            .setupWithRecyclerView(binding.recyclerView)
            .enablePreloading(true, 3)
            .enableStickyFloors(true)
            .setOnFloorClickListener { floorData, position ->
                // 处理楼层点击事件
            }
            .setOnFloorExposureListener { floorId, _ ->
                // 处理楼层曝光统计
            }
    }
    
    private fun setupUI() {
        // 注意：当前布局没有toolbar，如果需要可以后续添加
        // binding.toolbar.setNavigationOnClickListener {
        //     finish()
        // }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadDemoFloors()
            binding.swipeRefreshLayout.isRefreshing = false
        }
        
        binding.fab.setOnClickListener {
            addTestFloor()
        }
    }
    
    private fun loadDemoFloors() {
        lifecycleScope.launch {
            binding.progressIndicator.visibility = View.VISIBLE
            
            val floorList = createDemoFloors()
            floorManager.loadFloors(floorList)
            
            binding.progressIndicator.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }
    
    private fun createDemoFloors(): List<FloorData> {
        return listOf(
            // 轮播楼层
            FloorData(
                floorId = "demo_banner_floor",
                floorType = FloorType.BANNER,
                floorConfig = FloorConfig(
                    margin = EdgeInsets(16, 16, 16, 16),
                    padding = EdgeInsets(16, 16, 16, 16),
                    cornerRadius = 16f,
                    backgroundColor = "#FFFFFF",
                    elevation = 2f,
                    clickable = true
                ),
                businessData = mapOf(
                    "title" to "精彩推荐",
                    "title_color" to "#333333",
                    "title_size" to 18f,
                    "auto_play" to true,
                    "play_interval" to 3000L,
                    "show_indicators" to true,
                    "infinite_loop" to true,
                    "corner_radius" to 12f,
                    "pages" to listOf(
                        mapOf(
                            "title" to "精彩内容 1",
                            "description" to "这是第一个轮播页面，展示精彩内容",
                            "background_color" to "#2196F3"
                        ),
                        mapOf(
                            "title" to "优质服务 2",
                            "description" to "这是第二个轮播页面，提供优质服务",
                            "background_color" to "#4CAF50"
                        ),
                        mapOf(
                            "title" to "创新体验 3",
                            "description" to "这是第三个轮播页面，带来创新体验",
                            "background_color" to "#FF9800"
                        ),
                        mapOf(
                            "title" to "贴心关怀 4",
                            "description" to "这是第四个轮播页面，给您贴心关怀",
                            "background_color" to "#9C27B0"
                        )
                    )
                )
            ),
            
            // 文本楼层
            FloorData(
                floorId = "demo_text_floor",
                floorType = FloorType.TEXT,
                floorConfig = FloorConfig(
                    margin = EdgeInsets(16, 16, 16, 16),
                    padding = EdgeInsets(16, 16, 16, 16),
                    cornerRadius = 8f,
                    backgroundColor = "#FFFFFF",
                    elevation = 1f,
                    clickable = true
                ),
                businessData = mapOf(
                    "title" to "文本楼层示例",
                    "content" to "这是一个文本楼层的示例，展示如何在楼层中显示文字内容。支持多种样式配置，包括字体大小、颜色、背景等。",
                    "title_color" to "#2E7D32",
                    "content_color" to "#424242",
                    "title_size" to 16f,
                    "content_size" to 14f
                )
            ),
            
            // 图片楼层
            FloorData(
                floorId = "demo_image_floor",
                floorType = FloorType.IMAGE,
                floorConfig = FloorConfig(
                    margin = EdgeInsets(16, 16, 16, 16),
                    padding = EdgeInsets(12, 12, 12, 12),
                    cornerRadius = 8f,
                    backgroundColor = "#FFFFFF",
                    elevation = 2f,
                    clickable = true
                ),
                businessData = mapOf(
                    "title" to "图片楼层示例",
                    "description" to "这是一个图片楼层的示例，可以展示各种图片内容。",
                    "image_url" to "https://popaife.s3-accelerate.amazonaws.com/other/talkingLime-2025-07-01-01.webp",
                    "scale_type" to "CENTER_CROP",
                    "title_color" to "#1976D2",
                    "description_color" to "#666666"
                )
            )
        )
    }
    
    private fun addTestFloor() {
        val testFloor = FloorData(
            floorId = "test_floor_${System.currentTimeMillis()}",
            floorType = FloorType.TEXT,
            floorConfig = FloorConfig(
                margin = EdgeInsets(16, 16, 16, 16),
                padding = EdgeInsets(16, 16, 16, 16),
                cornerRadius = 8f,
                backgroundColor = "#FFFFFF",
                elevation = 2f,
                clickable = true
            ),
            businessData = mapOf(
                "title" to "动态添加的楼层",
                "content" to "这是通过FloatingActionButton动态添加的测试楼层",
                "title_color" to "#2E7D32",
                "content_color" to "#424242"
            )
        )
        
        lifecycleScope.launch {
            floorManager.addFloor(testFloor)
        }
    }
} 