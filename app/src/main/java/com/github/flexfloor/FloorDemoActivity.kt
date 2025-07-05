package com.github.flexfloor

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.github.flexfloor.databinding.ActivityFloorDemoBinding
import com.github.flexfloor.floors.BannerFloor
import com.github.flexfloor.floors.ImageFloor
import com.github.flexfloor.floors.TextFloor
import com.github.flexfloor.network.MockFloorDataSource
import com.github.flexfloorlib.core.FloorManager
import com.github.flexfloorlib.core.FloorFactory
import com.github.flexfloorlib.core.FloorViewModel
import com.github.flexfloorlib.core.FloorArchitecture
import com.github.flexfloorlib.model.FloorType
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorConfig
import com.github.flexfloorlib.model.EdgeInsets

/**
 * 楼层化框架演示页面 - 使用改进的架构设计
 * 
 * 架构特点：
 * 1. 数据源在应用层管理
 * 2. 通过依赖注入配置Repository
 * 3. FloorManager专注于楼层展示
 * 4. 清晰的职责分离
 */
class FloorDemoActivity : ComponentActivity() {

    private lateinit var binding: ActivityFloorDemoBinding
    private lateinit var floorManager: FloorManager
    private lateinit var viewModel: FloorViewModel
    private lateinit var dataSource: MockFloorDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityFloorDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化架构组件
        initializeArchitecture()
        
        // 注册楼层类型
        registerFloorTypes()
        
        // 初始化FloorManager
        initFloorManager()
        
        // 设置UI
        setupUI()
        
        // 观察数据变化
        observeViewModel()
        
        // 加载演示数据
        loadDemoFloors()
    }
    
    /**
     * 初始化架构组件
     * 使用依赖注入的方式配置数据源和Repository
     */
    private fun initializeArchitecture() {
        // 1. 创建你的数据源实现（这里用户可以实现自己的网络请求）
        dataSource = MockFloorDataSource(this)
        
        // 2. 使用架构初始化器创建ViewModel（推荐方式）
        viewModel = FloorArchitecture.createViewModel(application, dataSource)
    }
    
    /**
     * 注册楼层类型
     */
    private fun registerFloorTypes() {
        FloorFactory.registerFloor(FloorType.TEXT) { TextFloor() }
        FloorFactory.registerFloor(FloorType.IMAGE) { ImageFloor() }
        FloorFactory.registerFloor(FloorType.BANNER) { BannerFloor() }
    }
    
    /**
     * 初始化FloorManager
     * 专注于楼层展示配置，不涉及数据源
     */
    private fun initFloorManager() {
        floorManager = FloorManager.create(this)
            .setupWithRecyclerView(binding.recyclerView)
            .enablePreloading(true, 5)
            .enableStickyFloors(true)
            .enableAutoErrorHandling(true)
            .setOnFloorClickListener { floorData, position ->
                viewModel.onFloorClicked(floorData, position)
            }
            .setOnFloorExposureListener { floorId, exposureData ->
                viewModel.onFloorExposed(floorId, exposureData)
            }
            .configureErrorHandling {
                // 配置错误处理策略
                onNetworkError(
                    com.github.flexfloorlib.core.ErrorHandlingStrategy.RETRY,
                    com.github.flexfloorlib.core.ErrorRecoveryAction.Retry(maxRetries = 3)
                )
                onDataParseError(
                    com.github.flexfloorlib.core.ErrorHandlingStrategy.FALLBACK,
                    com.github.flexfloorlib.core.ErrorRecoveryAction.Fallback {
                        Toast.makeText(this@FloorDemoActivity, "数据解析失败，使用缓存数据", Toast.LENGTH_SHORT).show()
                    }
                )
            }
    }
    
    /**
     * 设置UI组件
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
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
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
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
        
        viewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.floorClickEvent.observe(this) { (floorData, position) ->
            Toast.makeText(
                this,
                "点击了${floorData.floorType.typeName}楼层，位置：$position",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        viewModel.floorExposureEvent.observe(this) { (floorId, _) ->
            Toast.makeText(this, "楼层曝光：$floorId", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 加载演示楼层数据
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
     * 动态添加测试楼层
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
        
        viewModel.addFloor(testFloor)
    }
} 