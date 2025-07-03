package com.github.flexfloorlib.core

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.github.flexfloorlib.model.FloorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 重构后的楼层基类
 * 职责分离：数据处理 + 视图渲染
 */
abstract class BaseFloor<T : Any> : LifecycleObserver {
    
    protected var floorData: FloorData? = null
    protected var businessData: T? = null
    private var currentView: View? = null
    private var currentPosition: Int = -1
    
    // 数据和视图状态
    private var dataLoadedFlag = false
    private var viewBoundFlag = false
    
    // 楼层专用协程作用域
    protected val floorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * 初始化楼层 - 模板方法
     */
    open fun initFloor(floorData: FloorData, businessData: T?) {
        this.floorData = floorData
        this.businessData = businessData
        
        // 如果业务数据为空，尝试从配置数据解析
        if (businessData == null) {
            this.businessData = parseBusinessData(floorData.businessData)
        }
        
        markDataLoaded()
    }
    
    /**
     * 绑定视图 - 模板方法
     */
    fun bindView(view: View, position: Int) {
        currentView = view
        currentPosition = position
        
        // 获取数据（同步 + 异步）
        val data = getFloorData()
        
        if (data != null) {
            // 有数据则立即渲染
            renderView(view, data, position)
            viewBoundFlag = true
        } else {
            // 无数据则显示加载状态，异步加载
            showLoadingState(view)
            loadDataAsync()
        }
    }
    
    /**
     * 获取楼层数据 - 统一数据获取逻辑
     */
    private fun getFloorData(): T? {
        // 优先使用已加载的业务数据
        businessData?.let { return it }
        
        // 从配置数据解析
        floorData?.businessData?.let { configData ->
            val parsedData = parseBusinessData(configData)
            businessData = parsedData
            return parsedData
        }
        
        return null
    }
    
    /**
     * 异步加载数据
     */
    private fun loadDataAsync() {
        if (dataLoadedFlag) return
        
        floorScope.launch {
            try {
                val loadedData = loadData()
                businessData = loadedData
                markDataLoaded()
                
                // 切换到主线程更新视图
                withContext(Dispatchers.Main) {
                    currentView?.let { view ->
                        if (loadedData != null) {
                            renderView(view, loadedData, currentPosition)
                            viewBoundFlag = true
                        } else {
                            showErrorState(view, "数据加载失败")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentView?.let { view ->
                        showErrorState(view, e.message ?: "加载异常")
                    }
                }
            }
        }
    }
    
    // === 抽象方法 - 子类必须实现 ===
    
    /**
     * 解析业务数据
     */
    protected abstract fun parseBusinessData(configData: Map<String, Any>): T?
    
    /**
     * 渲染视图
     */
    protected abstract fun renderView(view: View, data: T, position: Int)
    
    /**
     * 异步加载数据
     */
    abstract suspend fun loadData(): T?
    
    /**
     * 获取楼层类型
     */
    abstract fun getFloorType(): String
    
    /**
     * 获取布局资源ID
     */
    abstract fun getLayoutResId(): Int
    
    // === 可重写方法 ===
    
    /**
     * 显示加载状态
     */
    protected open fun showLoadingState(view: View) {
        // 默认实现：隐藏内容，显示加载指示器
    }
    
    /**
     * 显示错误状态
     */
    protected open fun showErrorState(view: View, error: String) {
        // 默认实现：显示错误信息
    }
    
    /**
     * 楼层点击处理
     */
    open fun onFloorClick(view: View) {
        floorData?.floorConfig?.jumpAction?.let { jumpAction ->
            FloorActionHandler.handleAction(view.context, jumpAction)
        }
    }
    
    // === 生命周期方法 ===
    
    open fun onFloorVisible() {
        // 楼层可见时调用
    }
    
    open fun onFloorInvisible() {
        // 楼层不可见时调用
    }
    
    open fun onAttach() {
        // 附着到窗口时调用
    }
    
    open fun onDetach() {
        // 从窗口分离时调用
        currentView = null
    }
    
    open fun onDestroy() {
        floorScope.cancel()
        currentView = null
    }
    
    // === 状态查询方法 ===
    
    fun isDataLoaded(): Boolean = dataLoadedFlag
    
    fun isViewBound(): Boolean = viewBoundFlag
    
    fun needsDataLoading(): Boolean = !dataLoadedFlag && businessData == null
    
    protected fun markDataLoaded() {
        dataLoadedFlag = true
    }
    
    /**
     * 获取曝光数据
     */
    open fun getExposureData(): Map<String, Any> {
        return mapOf(
            "floor_id" to (floorData?.floorId ?: ""),
            "floor_type" to getFloorType(),
            "priority" to (floorData?.priority ?: 0),
            "is_data_loaded" to dataLoadedFlag,
            "is_view_bound" to viewBoundFlag
        )
    }
} 