package com.github.flexfloor

import android.os.Bundle
import android.view.View
import android.widget.Toast
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

    /**
     * 初始化活动并设置楼层演示
     * 设置视图绑定、注册楼层类型、初始化楼层管理器、配置UI组件和加载演示楼层
     *
     * @param savedInstanceState 保存的实例状态包
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFloorDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerFloorTypes()
        initFloorManager()
        setupUI()
        loadDemoFloors()
    }

    /**
     * 向FloorFactory注册所有可用的楼层类型
     * 将楼层类型映射到对应的楼层实现类
     */
    private fun registerFloorTypes() {
        FloorFactory.registerFloor(FloorType.TEXT) { TextFloor() }
        FloorFactory.registerFloor(FloorType.IMAGE) { ImageFloor() }
        FloorFactory.registerFloor(FloorType.BANNER) { BannerFloor() }
    }

    /**
     * 初始化和配置FloorManager
     * 设置RecyclerView集成、启用预加载和吸顶楼层功能、配置点击和曝光监听器
     */
    private fun initFloorManager() {
        floorManager = FloorManager.create(this)
            .setupWithRecyclerView(binding.recyclerView)
            .enablePreloading(true, 5)
            .enableStickyFloors(true)
            .setOnFloorClickListener { floorData, position ->
                // 处理楼层点击事件
                Toast.makeText(this, "${floorData.floorType.typeName}楼层点击位置$position", Toast.LENGTH_LONG)
            }
            .setOnFloorExposureListener { floorId, _ ->
                // 处理楼层曝光统计
                Toast.makeText(this, "${floorId}楼层曝光", Toast.LENGTH_LONG)
            }
    }

    /**
     * 配置UI组件并设置事件监听器
     * 设置下拉刷新布局、悬浮操作按钮和其他UI交互
     */
    private fun setupUI() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadDemoFloors()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        binding.fab.setOnClickListener {
            addTestFloor()
        }
    }

    /**
     * 加载演示楼层到RecyclerView
     * 在加载过程中显示进度指示器并管理视图可见性状态
     */
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

    /**
     * 创建具有各种配置的演示楼层列表
     * 包括轮播楼层、文本楼层和图片楼层，具有不同的样式选项
     *
     * @return List<FloorData> 包含演示楼层配置的列表
     */
    private fun createDemoFloors(): List<FloorData> {
        return listOf(
            // 轮播楼层
            FloorData(
                floorId = "demo_banner_floor",
                floorType = FloorType.BANNER,
                floorConfig = FloorConfig(
                    margin = EdgeInsets(16, 8, 16, 8),
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
                    margin = EdgeInsets(16, 8, 16, 8),
                    padding = EdgeInsets(16, 16, 16, 16),
                    cornerRadius = 16f,
                    backgroundColor = "#FFFFFF",
                    elevation = 2f,
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
                    margin = EdgeInsets(16, 8, 16, 8),
                    padding = EdgeInsets(12, 12, 12, 12),
                    cornerRadius = 16f,
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

    /**
     * 动态添加测试楼层到RecyclerView
     * 创建具有唯一ID的新文本楼层并将其添加到楼层管理器
     */
    private fun addTestFloor() {
        val testFloor = FloorData(
            floorId = "test_floor_${System.currentTimeMillis()}",
            floorType = FloorType.TEXT,
            floorConfig = FloorConfig(
                margin = EdgeInsets(16, 8, 16, 8),
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