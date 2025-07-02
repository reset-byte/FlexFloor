package com.github.flexfloor

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.flexfloor.databinding.ActivityFloorDemoBinding
import com.github.flexfloor.floors.BannerFloor
import com.github.flexfloor.floors.ImageFloor
import com.github.flexfloor.floors.TextFloor
import com.github.flexfloor.network.FloorApiService
import com.github.flexfloor.repository.FloorRepository
import com.github.flexfloor.viewmodel.FloorViewModel
import com.github.flexfloor.viewmodel.FloorViewModelFactory
import com.github.flexfloorlib.adapter.FloorAdapter
import com.github.flexfloorlib.base.IFloorClickListener
import com.github.flexfloorlib.base.IFloorExposureListener
import com.github.flexfloorlib.manager.FloorManager
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorStyle
import com.github.flexfloorlib.model.FloorType
import com.github.flexfloorlib.widget.StickyFloorLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 楼层化框架演示页面
 * 展示如何使用楼层架构构建动态页面
 */
class FloorDemoActivity : ComponentActivity() {
    
    private lateinit var binding: ActivityFloorDemoBinding
    private lateinit var floorViewModel: FloorViewModel
    private lateinit var floorAdapter: FloorAdapter
    private lateinit var floorManager: FloorManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 使用ViewBinding
        binding = ActivityFloorDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化组件
        initializeFloorManager()
        initializeViewModel()
        initializeRecyclerView()
        initializeUI()
        observeViewModelState()
        
        // 加载楼层配置
        loadFloorConfiguration()
    }
    
    /**
     * 初始化楼层管理器
     */
    private fun initializeFloorManager() {
        floorManager = FloorManager.getInstance()
        
        // 注册楼层类型
        floorManager.registerFloor(FloorType.TEXT) { TextFloor() }
        floorManager.registerFloor(FloorType.IMAGE) { ImageFloor() }
        floorManager.registerFloor(FloorType.BANNER) { BannerFloor() }
        
        // 可以注册更多楼层类型
        // floorManager.registerFloor(FloorType.VIDEO) { VideoFloor() }
        // floorManager.registerFloor(FloorType.GRID) { GridFloor() }
    }
    
    /**
     * 初始化ViewModel
     */
    private fun initializeViewModel() {
        // 创建一个默认的API服务实现，实际项目中应该使用Retrofit
        val apiService = object : FloorApiService {
            override suspend fun getFloorConfig(pageId: String): retrofit2.Response<List<com.github.flexfloorlib.model.FloorData>> {
                // 返回示例数据，实际项目中应该调用真实API
                return retrofit2.Response.success(emptyList())
            }
            
            override suspend fun getFloorData(floorId: String, params: Map<String, Any>): retrofit2.Response<Any> {
                // 返回示例数据
                return retrofit2.Response.success(mapOf<String, Any>())
            }
            
            override suspend fun getBatchFloorConfig(pageIds: List<String>): retrofit2.Response<Map<String, List<com.github.flexfloorlib.model.FloorData>>> {
                // 返回示例数据
                return retrofit2.Response.success(emptyMap())
            }
            
            override suspend fun reportFloorExposure(exposureData: com.github.flexfloor.network.FloorExposureData): retrofit2.Response<Unit> {
                // 返回成功响应
                return retrofit2.Response.success(Unit)
            }
            
            override suspend fun reportFloorClick(clickData: com.github.flexfloor.network.FloorClickData): retrofit2.Response<Unit> {
                // 返回成功响应
                return retrofit2.Response.success(Unit)
            }
        }
        
        // 创建缓存管理器
        val cacheManager = com.github.flexfloorlib.cache.FloorCacheManager.getInstance(this)
        
        val repository = FloorRepository(apiService, cacheManager)
        val factory = FloorViewModelFactory(repository, apiService)
        
        floorViewModel = ViewModelProvider(this, factory)[FloorViewModel::class.java]
    }
    
    /**
     * 初始化RecyclerView
     */
    private fun initializeRecyclerView() {
        floorAdapter = FloorAdapter()
        
        // 支持吸顶楼层
        val layoutManager = StickyFloorLayoutManager(binding.recyclerView)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = floorAdapter
        
        // 设置楼层点击监听器
        floorAdapter.setFloorClickListener(object : IFloorClickListener {
            override fun onFloorClick(floorData: FloorData, view: View, position: Int) {
                handleFloorClick(floorData, view, position)
            }
        })
        
        // 设置楼层曝光监听器
        floorAdapter.setFloorExposureListener(object : IFloorExposureListener {
            override fun onFloorExposure(floorData: FloorData, position: Int, exposureRatio: Float) {
                handleFloorExposure(floorData, position, exposureRatio)
            }
        })
    }
    
    /**
     * 初始化UI组件
     */
    private fun initializeUI() {
        // 设置Toolbar
        binding.toolbar.setNavigationOnClickListener {
            // 返回上一页
            finish()
        }
        
        // 设置下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshFloorData()
        }
        
        // 设置错误提示关闭按钮
        binding.dismissErrorButton.setOnClickListener {
            hideErrorMessage()
        }
        
        // 设置浮动操作按钮
        binding.fab.setOnClickListener {
            addTestFloor()
        }
    }
    
    /**
     * 观察ViewModel状态变化
     */
    private fun observeViewModelState() {
        lifecycleScope.launch {
            // 观察UI状态
            floorViewModel.uiState.collectLatest { uiState ->
                updateUIState(uiState)
            }
        }
        
        lifecycleScope.launch {
            // 观察楼层数据
            floorViewModel.floorData.collectLatest { floors ->
                updateFloorData(floors)
            }
        }
        
        lifecycleScope.launch {
            // 观察错误消息
            floorViewModel.errorMessage.collectLatest { error ->
                error?.let { showErrorMessage(it) }
            }
        }
    }
    
    /**
     * 加载楼层配置
     */
    private fun loadFloorConfiguration() {
        floorViewModel.loadFloorConfig("demo_page")
    }
    
    /**
     * 刷新楼层数据
     */
    private fun refreshFloorData() {
        floorViewModel.refreshFloors("demo_page")
    }
    
    /**
     * 更新UI状态
     */
    private fun updateUIState(uiState: com.github.flexfloor.viewmodel.FloorUiState) {
        // 更新加载状态
        binding.progressIndicator.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
        
        // 更新空状态
        binding.emptyStateLayout.visibility = if (uiState.isEmpty && !uiState.isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // 更新RecyclerView可见性
        binding.recyclerView.visibility = if (uiState.isEmpty || uiState.isLoading) {
            View.GONE
        } else {
            View.VISIBLE
        }
        
        // 处理导航事件
        uiState.navigationEvent?.let { event ->
            handleNavigationEvent(event)
            floorViewModel.clearNavigationEvent()
        }
    }
    
    /**
     * 更新楼层数据
     */
    private fun updateFloorData(floors: List<FloorData>) {
        floorAdapter.updateFloors(floors)
    }
    
    /**
     * 显示错误消息
     */
    private fun showErrorMessage(message: String) {
        binding.errorMessageText.text = message
        binding.errorCard.visibility = View.VISIBLE
    }
    
    /**
     * 隐藏错误消息
     */
    private fun hideErrorMessage() {
        binding.errorCard.visibility = View.GONE
        floorViewModel.clearError()
    }
    
    /**
     * 处理楼层点击事件
     */
    private fun handleFloorClick(floorData: FloorData, view: View, position: Int) {
        // 委托给ViewModel处理
        floorViewModel.handleFloorClick(floorData, position)
        
        // 显示点击反馈
        Toast.makeText(
            this,
            "点击了楼层: ${floorData.title ?: floorData.floorId}",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * 处理楼层曝光事件
     */
    private fun handleFloorExposure(floorData: FloorData, position: Int, exposureRatio: Float) {
        // 委托给ViewModel处理
        floorViewModel.handleFloorExposure(floorData, position, exposureRatio)
    }
    
    /**
     * 处理导航事件
     */
    private fun handleNavigationEvent(event: com.github.flexfloor.viewmodel.NavigationEvent) {
        when (event.actionType) {
            "url" -> {
                // 处理URL跳转
                Toast.makeText(this, "跳转到: ${event.actionUrl}", Toast.LENGTH_SHORT).show()
            }
            "activity" -> {
                // 处理Activity跳转
                Toast.makeText(this, "跳转到Activity: ${event.actionUrl}", Toast.LENGTH_SHORT).show()
            }
            "fragment" -> {
                // 处理Fragment跳转
                Toast.makeText(this, "跳转到Fragment: ${event.actionUrl}", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "未知的跳转类型: ${event.actionType}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 添加测试楼层
     */
    private fun addTestFloor() {
        val floorTypes = listOf(FloorType.TEXT, FloorType.IMAGE, FloorType.BANNER)
        val randomType = floorTypes.random()
        
        val testFloor = createTestFloorData(randomType)
        floorViewModel.addFloor(testFloor)
        
        val typeName = when (randomType) {
            FloorType.TEXT -> "文本楼层"
            FloorType.IMAGE -> "图片楼层"
            FloorType.BANNER -> "横幅楼层"
            else -> "未知楼层"
        }
        
        Toast.makeText(this, "已添加${typeName}测试楼层", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 创建测试楼层数据
     */
    private fun createTestFloorData(floorType: FloorType = FloorType.TEXT): FloorData {
        val currentTime = System.currentTimeMillis()
        
        return when (floorType) {
            FloorType.TEXT -> createTestTextFloor(currentTime)
            FloorType.IMAGE -> createTestImageFloor(currentTime)
            FloorType.BANNER -> createTestBannerFloor(currentTime)
            else -> createTestTextFloor(currentTime)
        }
    }
    
    /**
     * 创建测试文本楼层
     */
    private fun createTestTextFloor(timestamp: Long): FloorData {
        return FloorData(
            floorId = "test_text_floor_${timestamp}",
            floorType = FloorType.TEXT,
            title = "测试文本楼层",
            data = mapOf(
                "title" to "这是一个测试文本楼层",
                "content" to "通过点击浮动按钮动态添加的文本内容",
                "title_color" to "#333333",
                "content_color" to "#666666"
            ),
            style = createTestFloorStyle("#F8F9FA"),
            action = null,
            priority = 999,
            isSticky = false,
            isVisible = true,
            isLazyLoad = false
        )
    }
    
    /**
     * 创建测试图片楼层
     */
    private fun createTestImageFloor(timestamp: Long): FloorData {
        return FloorData(
            floorId = "test_image_floor_${timestamp}",
            floorType = FloorType.IMAGE,
            title = "测试图片楼层",
            data = mapOf(
                "title" to "这是一个测试图片楼层",
                "description" to "展示图片内容的楼层示例",
                "image_url" to "https://example.com/test-image.jpg",
                "image_width" to 300,
                "image_height" to 200,
                "scale_type" to "center_crop",
                "title_color" to "#2C3E50",
                "description_color" to "#7F8C8D"
            ),
            style = createTestFloorStyle("#E8F4F8"),
            action = null,
            priority = 998,
            isSticky = false,
            isVisible = true,
            isLazyLoad = true
        )
    }
    
    /**
     * 创建测试横幅楼层
     */
    private fun createTestBannerFloor(timestamp: Long): FloorData {
        return FloorData(
            floorId = "test_banner_floor_${timestamp}",
            floorType = FloorType.BANNER,
            title = "测试横幅楼层",
            data = mapOf(
                "title" to "这是一个测试横幅楼层",
                "image_url" to "https://example.com/test-banner.jpg",
                "height" to 180,
                "scale_type" to "center_crop",
                "background_color" to "#E3F2FD",
                "corner_radius" to 12f,
                "title_color" to "#1976D2",
                "title_size" to 18f,
                "auto_play" to true,
                "play_interval" to 5000L,
                "show_indicators" to true
            ),
            style = createTestFloorStyle("#E3F2FD"),
            action = null,
            priority = 997,
            isSticky = false,
            isVisible = true,
            isLazyLoad = true
        )
    }
    
    /**
     * 创建测试楼层样式
     */
    private fun createTestFloorStyle(backgroundColor: String): FloorStyle {
        return FloorStyle(
            backgroundColor = backgroundColor,
            cornerRadius = 8f,
            marginTop = 12,
            marginBottom = 12,
            marginLeft = 16,
            marginRight = 16,
            paddingTop = 16,
            paddingBottom = 16,
            paddingLeft = 16,
            paddingRight = 16,
            elevation = 2f
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        floorAdapter.clearFloorCache()
    }
} 