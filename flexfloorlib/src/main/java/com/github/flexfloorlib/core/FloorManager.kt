package com.github.flexfloorlib.core

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.adapter.FloorAdapter
import com.github.flexfloorlib.cache.FloorCacheManager
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.LoadPolicy
import com.github.flexfloorlib.utils.FloorPreloader
import com.github.flexfloorlib.utils.StickyFloorHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 楼层化布局系统的核心管理器
 * 处理楼层生命周期、预加载、缓存和协调
 */
class FloorManager private constructor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    
    companion object {
        fun create(context: Context, lifecycleOwner: LifecycleOwner): FloorManager {
            return FloorManager(context, lifecycleOwner)
        }
    }
    
    private val floorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val cacheManager = FloorCacheManager.getInstance(context)
    private var preloader: FloorPreloader? = null
    private var stickyHelper: StickyFloorHelper? = null
    
    // 核心组件
    private var recyclerView: RecyclerView? = null
    private var floorAdapter: FloorAdapter? = null
    
    // 配置参数
    private var enablePreloading = true
    private var enableStickyFloors = false
    private var preloadDistance = 3 // 提前预加载的楼层数量
    
    // 回调函数
    private var onFloorClickListener: ((FloorData, Int) -> Unit)? = null
    private var onFloorExposureListener: ((String, Map<String, Any>) -> Unit)? = null
    private var onFloorLoadListener: ((FloorData) -> Unit)? = null
    private var onFloorErrorListener: ((String, Throwable) -> Unit)? = null
    
    /**
     * 使用RecyclerView初始化楼层管理器
     */
    fun setupWithRecyclerView(recyclerView: RecyclerView): FloorManager {
        this.recyclerView = recyclerView
        
        // 设置适配器
        floorAdapter = FloorAdapter().apply {
            setOnFloorClickListener { floorData, position ->
                onFloorClickListener?.invoke(floorData, position)
            }
            setOnFloorExposureListener { floorId, exposureData ->
                onFloorExposureListener?.invoke(floorId, exposureData)
            }
            setOnFloorLoadListener { floorData ->
                onFloorLoadListener?.invoke(floorData)
            }
        }
        
        recyclerView.adapter = floorAdapter
        
        // 如果未设置布局管理器则设置默认的
        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(context)
        }
        
        // 设置预加载器
        if (enablePreloading) {
            setupPreloader()
        }
        
        // 设置吸顶楼层
        if (enableStickyFloors) {
            setupStickyFloors()
        }
        
        return this
    }
    
    /**
     * 加载楼层数据
     */
    fun loadFloors(floorDataList: List<FloorData>) {
        floorScope.launch {
            try {
                // 按加载策略处理楼层
                val processedFloors = processFloorsByLoadPolicy(floorDataList)
                
                // 更新适配器
                floorAdapter?.updateFloorData(processedFloors)
                
                // 缓存楼层配置
                cacheFloorConfigurations(processedFloors)
                
                // 如果启用预加载则开始预加载
                if (enablePreloading) {
                    preloader?.startPreloading(processedFloors)
                }
                
            } catch (e: Exception) {
                onFloorErrorListener?.invoke("加载楼层失败", e)
            }
        }
    }
    
    /**
     * 添加单个楼层
     */
    fun addFloor(floorData: FloorData, position: Int = -1) {
        floorScope.launch {
            try {
                // 缓存配置
                cacheManager.cacheFloorConfig(floorData.floorId, floorData)
                
                // 添加到适配器
                if (position >= 0) {
                    floorAdapter?.addFloorData(floorData, position)
                } else {
                    floorAdapter?.addFloorData(floorData)
                }
                
            } catch (e: Exception) {
                onFloorErrorListener?.invoke("添加楼层失败", e)
            }
        }
    }
    
    /**
     * 移除楼层
     */
    fun removeFloor(position: Int) {
        floorAdapter?.removeFloorData(position)
    }
    
    /**
     * 更新楼层
     */
    fun updateFloor(position: Int, floorData: FloorData) {
        floorScope.launch {
            try {
                // 缓存更新的配置
                cacheManager.cacheFloorConfig(floorData.floorId, floorData)
                
                // 更新适配器
                floorAdapter?.updateFloorData(position, floorData)
                
            } catch (e: Exception) {
                onFloorErrorListener?.invoke("更新楼层失败", e)
            }
        }
    }
    
    /**
     * 刷新所有楼层
     */
    fun refreshFloors() {
        val currentFloors = floorAdapter?.getFloorDataList() ?: return
        loadFloors(currentFloors)
    }
    
    /**
     * 按加载策略处理楼层
     */
    private suspend fun processFloorsByLoadPolicy(floorDataList: List<FloorData>): List<FloorData> {
        return floorDataList.map { floorData ->
            when (floorData.loadPolicy) {
                LoadPolicy.EAGER -> {
                    // 立即加载
                    loadFloorDataImmediately(floorData)
                }
                LoadPolicy.PRELOAD -> {
                    // 安排预加载
                    scheduleFloorPreload(floorData)
                }
                LoadPolicy.LAZY -> {
                    // 保持原样，将在可见时加载
                    floorData
                }
            }
        }
    }
    
    /**
     * 立即加载楼层数据
     */
    private suspend fun loadFloorDataImmediately(floorData: FloorData): FloorData {
        return try {
            val floor = FloorFactory.createFloor(floorData)
            val data = floor?.loadData()
            @Suppress("UNCHECKED_CAST")
            (floor as BaseFloor<Any?>?)?.initFloor(floorData, data)
            floorData
        } catch (e: Exception) {
            onFloorErrorListener?.invoke("加载楼层数据失败: ${floorData.floorId}", e)
            floorData
        }
    }
    
    /**
     * 安排楼层预加载
     */
    private fun scheduleFloorPreload(floorData: FloorData): FloorData {
        preloader?.schedulePreload(floorData)
        return floorData
    }
    
    /**
     * 缓存楼层配置
     */
    private suspend fun cacheFloorConfigurations(floorDataList: List<FloorData>) {
        floorDataList.forEach { floorData ->
            cacheManager.cacheFloorConfig(floorData.floorId, floorData)
        }
    }
    
    /**
     * 设置预加载器
     */
    private fun setupPreloader() {
        recyclerView?.let { rv ->
            preloader = FloorPreloader(
                recyclerView = rv,
                preloadDistance = preloadDistance,
                onPreloadFloor = { floorData ->
                    floorScope.launch {
                        loadFloorDataImmediately(floorData)
                    }
                }
            )
        }
    }
    
    /**
     * 设置吸顶楼层
     */
    private fun setupStickyFloors() {
        recyclerView?.let { rv ->
            stickyHelper = StickyFloorHelper(rv).apply {
                attachToRecyclerView()
            }
        }
    }
    
    /**
     * 配置方法
     */
    fun enablePreloading(enable: Boolean, distance: Int = 3): FloorManager {
        this.enablePreloading = enable
        this.preloadDistance = distance
        
        if (enable && recyclerView != null) {
            setupPreloader()
        } else {
            preloader?.destroy()
            preloader = null
        }
        
        return this
    }
    
    fun enableStickyFloors(enable: Boolean): FloorManager {
        this.enableStickyFloors = enable
        
        if (enable && recyclerView != null) {
            setupStickyFloors()
        } else {
            stickyHelper?.detachFromRecyclerView()
            stickyHelper = null
        }
        
        return this
    }
    
    /**
     * 回调设置方法
     */
    fun setOnFloorClickListener(listener: (FloorData, Int) -> Unit): FloorManager {
        onFloorClickListener = listener
        return this
    }
    
    fun setOnFloorExposureListener(listener: (String, Map<String, Any>) -> Unit): FloorManager {
        onFloorExposureListener = listener
        return this
    }
    
    fun setOnFloorLoadListener(listener: (FloorData) -> Unit): FloorManager {
        onFloorLoadListener = listener
        return this
    }
    
    fun setOnFloorErrorListener(listener: (String, Throwable) -> Unit): FloorManager {
        onFloorErrorListener = listener
        return this
    }
    
    /**
     * 清理资源
     */
    fun destroy() {
        preloader?.destroy()
        stickyHelper?.detachFromRecyclerView()
        floorAdapter?.clearCache()
        
        preloader = null
        stickyHelper = null
        floorAdapter = null
        recyclerView = null
    }
    
    /**
     * 获取缓存管理器实例
     */
    fun getCacheManager(): FloorCacheManager = cacheManager
    
    /**
     * 获取当前楼层
     */
    fun getCurrentFloors(): List<FloorData> {
        return floorAdapter?.getFloorDataList() ?: emptyList()
    }
} 