package com.github.flexfloor

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.github.flexfloor.databinding.ActivityFloorDemoBinding
import com.github.flexfloor.floors.BannerFloor
import com.github.flexfloor.floors.ImageFloor
import com.github.flexfloor.floors.TextFloor
import com.github.flexfloor.network.MockFloorDataSource
import com.github.flexfloorlib.core.FloorManager
import com.github.flexfloorlib.core.FloorFactory
import com.github.flexfloorlib.core.FloorViewModel
import com.github.flexfloorlib.model.FloorType
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorConfig
import com.github.flexfloorlib.model.EdgeInsets

/**
 * 楼层化框架演示页面 - 使用 MVVM 架构
 */
class FloorDemoActivity : ComponentActivity() {

    private lateinit var binding: ActivityFloorDemoBinding
    private lateinit var floorManager: FloorManager
    private lateinit var viewModel: FloorViewModel
    private lateinit var mockDataSource: MockFloorDataSource

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

        initViewModel()
        registerFloorTypes()
        initFloorManager()
        setupUI()
        observeViewModel()
        loadDemoFloors()
    }

    /**
     * 初始化 ViewModel 和数据源
     */
    private fun initViewModel() {
        // 创建 ViewModel
        viewModel = ViewModelProvider(this)[FloorViewModel::class.java]
        
        // 创建模拟数据源
        mockDataSource = MockFloorDataSource(this)
        
        // 设置数据源到 ViewModel
        viewModel.setRemoteDataSource(mockDataSource)
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
            .enableAutoErrorHandling(true) // 启用自动错误处理
            .setOnFloorClickListener { floorData, position ->
                // 通过 ViewModel 处理楼层点击事件
                viewModel.onFloorClicked(floorData, position)
            }
            .setOnFloorExposureListener { floorId, exposureData ->
                // 通过 ViewModel 处理楼层曝光统计
                viewModel.onFloorExposed(floorId, exposureData)
            }
            // 不需要设置错误监听器，SDK会自动处理
    }

    /**
     * 配置UI组件并设置事件监听器
     * 设置下拉刷新布局、悬浮操作按钮和其他UI交互
     */
    private fun setupUI() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshFloors()
        }

        binding.fab.setOnClickListener {
            addTestFloor()
        }
    }

    /**
     * 观察 ViewModel 数据变化
     */
    private fun observeViewModel() {
        // 观察楼层数据列表
        viewModel.floorDataList.observe(this) { floorList ->
            if (floorList.isNotEmpty()) {
                floorManager.loadFloors(floorList)
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
            } else {
                binding.recyclerView.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        // 观察错误状态 - 简化处理
        viewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                // 只显示简单的提示，不需要复杂的错误处理
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }

        // 观察楼层点击事件
        viewModel.floorClickEvent.observe(this) { (floorData, position) ->
            Toast.makeText(this, "${floorData.floorType.typeName}楼层点击位置$position", Toast.LENGTH_SHORT).show()
        }

        // 观察楼层曝光事件
        viewModel.floorExposureEvent.observe(this) { (floorId, _) ->
            Toast.makeText(this, "${floorId}楼层曝光", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 加载演示楼层到RecyclerView
     * 通过 ViewModel 从数据源中加载楼层数据
     */
    private fun loadDemoFloors() {
        viewModel.loadFloorConfig("demo_page", useCache = false)
    }

    /**
     * 刷新楼层数据
     */
    private fun refreshFloors() {
        viewModel.refreshFloorConfig("demo_page")
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

        // 通过 ViewModel 添加楼层
        viewModel.addFloor(testFloor)
    }
} 