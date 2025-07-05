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

/**
 * 楼层化框架演示页面 - 正确的加载流程
 * 
 * 加载流程：
 * 1. 调用一次loadFloorConfig，立即显示骨架屏
 * 2. MockFloorDataSource内部延迟5秒
 * 3. 5秒后通过回调更新真实数据
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
        
        // 设置数据更新回调
        setupDataUpdateCallback()
        
        // 开始加载（一次调用）
        startLoading()
    }
    
    /**
     * 初始化架构组件
     */
    private fun initializeArchitecture() {
        dataSource = MockFloorDataSource(this)
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
     * 设置数据更新回调
     */
    private fun setupDataUpdateCallback() {
        dataSource.setOnDataUpdateCallback { realData ->
            runOnUiThread {
                Toast.makeText(this, "真实数据加载完成，更新楼层内容", Toast.LENGTH_SHORT).show()
                // 通过ViewModel更新数据，确保正确的数据流
                updateFloorDataThroughViewModel(realData)
            }
        }
    }
    
    /**
     * 通过ViewModel更新楼层数据
     */
    private fun updateFloorDataThroughViewModel(floorData: List<FloorData>) {
        // 先清除楼层缓存，确保重新创建楼层实例
        clearFloorCache()
        
        // 直接通过FloorManager更新，但要确保清除了缓存
        floorManager.loadFloors(floorData)
    }
    
    /**
     * 清除楼层缓存
     */
    private fun clearFloorCache() {
        // 获取FloorAdapter并清除缓存
        binding.recyclerView.adapter?.let { adapter ->
            if (adapter is com.github.flexfloorlib.adapter.FloorAdapter) {
                adapter.clearCache()
            }
        }
    }
    
    /**
     * 开始加载（一次调用）
     */
    private fun startLoading() {
        Toast.makeText(this, "开始加载楼层数据，将显示5秒骨架屏", Toast.LENGTH_SHORT).show()
        viewModel.loadFloorConfig("demo_page", useCache = false)
    }
    
    /**
     * 刷新楼层数据
     */
    private fun refreshFloors() {
        // 重置加载状态
        dataSource.resetLoadingState()
        
        // 重新设置回调
        setupDataUpdateCallback()
        
        // 重新开始加载
        startLoading()
    }
} 