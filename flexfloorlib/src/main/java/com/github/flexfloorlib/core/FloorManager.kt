package com.github.flexfloorlib.core

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * 楼层化布局系统的核心管理器
 * 处理楼层生命周期、预加载、缓存和协调
 */
class FloorManager private constructor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner? = null
) {
    
    companion object {
        /**
         * 创建FloorManager实例（需要手动管理生命周期）
         */
        fun create(context: Context): FloorManager {
            return FloorManager(context, null)
        }
        
        /**
         * 创建FloorManager实例（自动管理生命周期）
         */
        fun create(context: Context, lifecycleOwner: LifecycleOwner): FloorManager {
            return FloorManager(context, lifecycleOwner)
        }
    }
    
    private val floorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val cacheManager = FloorCacheManager.getInstance(context)
    private val errorHandler = FloorErrorHandler()
    private var preloader: FloorPreloader? = null
    private var stickyHelper: StickyFloorHelper? = null
    private var isDestroyed = false
    
    init {
        // 如果提供了lifecycleOwner，自动管理生命周期
        lifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    destroy()
                }
            }
        })
        
        // 设置默认的错误处理策略
        setupDefaultErrorHandling()
    }
    
    // 核心组件
    private var recyclerView: RecyclerView? = null
    private var floorAdapter: FloorAdapter? = null
    
    // 配置参数
    private var enablePreloading = true
    private var enableStickyFloors = false
    private var preloadDistance = 3 // 提前预加载的楼层数量
    private var enableAutoErrorHandling = false // 自动错误处理开关
    
    // 回调函数
    private var onFloorClickListener: ((FloorData, Int) -> Unit)? = null
    private var onFloorExposureListener: ((String, Map<String, Any>) -> Unit)? = null
    private var onFloorLoadListener: ((FloorData) -> Unit)? = null
    private var onFloorErrorListener: ((FloorError) -> Unit)? = null
    private var onFloorRecoveryListener: ((FloorError, Boolean) -> Unit)? = null
    
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
        
        // 默认启用自动错误处理
        if (!enableAutoErrorHandling) {
            enableAutoErrorHandling(true)
        }
        
        return this
    }
    
    /**
     * 加载楼层数据
     */
    fun loadFloors(floorDataList: List<FloorData>) {
        floorScope.launch {
            executeWithErrorHandling(
                operation = {
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
                },
                onError = { error ->
                    when (error) {
                        is FloorError.DataParseError -> {
                            // 数据解析错误，尝试使用缓存数据
                            loadFloorDataFromCache(floorDataList)
                        }
                        is FloorError.NetworkError -> {
                            // 网络错误，尝试重试
                            retryLoadFloors(floorDataList)
                        }
                        else -> {
                            // 其他错误，显示错误状态
                            showErrorState(error)
                        }
                    }
                }
            )
        }
    }
    
    /**
     * 添加单个楼层
     */
    fun addFloor(floorData: FloorData, position: Int = -1) {
        floorScope.launch {
            executeWithErrorHandling(
                operation = {
                    // 缓存配置
                    cacheManager.cacheFloorConfig(floorData.floorId, floorData)
                    
                    // 添加到适配器
                    if (position >= 0) {
                        floorAdapter?.addFloorData(floorData, position)
                    } else {
                        floorAdapter?.addFloorData(floorData)
                    }
                },
                onError = { error ->
                    handleFloorOperationError(error, floorData)
                }
            )
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
            executeWithErrorHandling(
                operation = {
                    // 缓存更新的配置
                    cacheManager.cacheFloorConfig(floorData.floorId, floorData)
                    
                    // 更新适配器
                    floorAdapter?.updateFloorData(position, floorData)
                },
                onError = { error ->
                    handleFloorOperationError(error, floorData)
                }
            )
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
        return withContext(Dispatchers.Default) {
            floorDataList.map { floorData ->
                when (floorData.loadPolicy) {
                    LoadPolicy.EAGER -> {
                        // 立即加载楼层数据
                        loadFloorDataImmediately(floorData)
                        floorData
                    }
                    LoadPolicy.LAZY -> {
                        // 懒加载策略，仅返回配置
                        floorData
                    }
                    LoadPolicy.PRELOAD -> {
                        // 预加载策略，异步加载数据
                        preloadFloorData(floorData)
                        floorData
                    }
                }
            }
        }
    }
    
    /**
     * 立即加载楼层数据
     */
    private suspend fun loadFloorDataImmediately(floorData: FloorData) {
        executeWithErrorHandling(
            operation = {
                val floor = FloorFactory.createFloor(floorData)
                if (floor != null) {
                    val data = floor.loadData()
                    // 缓存加载的数据
                    if (data != null) {
                        cacheManager.cacheFloorData(
                            "${floorData.floorId}_data",
                            data,
                            floorData.cachePolicy
                        )
                    }
                } else {
                    // 楼层创建失败，通过错误处理机制处理
                    val error = FloorError.FloorCreationError.FloorTypeNotRegistered(
                        floorData.floorType.typeName,
                        floorData = floorData
                    )
                    errorHandler.handleError(error)
                }
            },
            onError = { error ->
                handleFloorDataLoadError(error, floorData)
            }
        )
    }
    
    /**
     * 预加载楼层数据
     */
    private fun preloadFloorData(floorData: FloorData) {
        floorScope.launch {
            executeWithErrorHandling(
                operation = {
                    loadFloorDataImmediately(floorData)
                },
                onError = { error ->
                    // 预加载失败不影响主流程，只记录错误
                    logError("预加载楼层数据失败", error)
                }
            )
        }
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
     * 启用自动错误处理
     */
    fun enableAutoErrorHandling(enable: Boolean): FloorManager {
        this.enableAutoErrorHandling = enable
        if (enable) {
            // 启用自动错误处理时，设置默认的UI处理器
            setAutoErrorHandler()
        } else {
            // 禁用自动错误处理时，清除自动错误监听器
            if (onFloorErrorListener != null) {
                onFloorErrorListener = null
            }
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
    
    fun setOnFloorErrorListener(listener: (FloorError) -> Unit): FloorManager {
        onFloorErrorListener = listener
        // 同时设置到错误处理器
        errorHandler.addErrorListener(listener)
        return this
    }
    
    fun setOnFloorRecoveryListener(listener: (FloorError, Boolean) -> Unit): FloorManager {
        onFloorRecoveryListener = listener
        return this
    }
    
    /**
     * 清理资源
     */
    fun destroy() {
        preloader?.destroy()
        stickyHelper?.detachFromRecyclerView()
        floorAdapter?.clearCache()
        errorHandler.reset()
        
        preloader = null
        stickyHelper = null
        floorAdapter = null
        recyclerView = null
        
        isDestroyed = true
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
    
    /**
     * 获取错误处理器
     */
    fun getErrorHandler(): FloorErrorHandler = errorHandler
    
    /**
     * 获取错误统计
     */
    fun getErrorStats(): Map<String, Int> = errorHandler.getErrorStats()
    
    /**
     * 配置错误处理策略
     */
    fun configureErrorHandling(config: FloorErrorHandlingConfig.() -> Unit): FloorManager {
        val configBuilder = FloorErrorHandlingConfig()
        config(configBuilder)
        configBuilder.applyTo(errorHandler)
        return this
    }
    
    // === 错误处理辅助方法 ===
    
    /**
     * 执行操作并处理错误
     */
    private suspend fun executeWithErrorHandling(
        operation: suspend () -> Unit,
        onError: suspend (FloorError) -> Unit
    ) {
        try {
            if (isDestroyed) {
                val error = FloorError.LifecycleError.ManagerDestroyed()
                onError(error)
                return
            }
            operation()
        } catch (e: Exception) {
            val floorError = FloorErrorUtils.convertThrowableToFloorError(e)
            val handled = errorHandler.handleError(floorError)
            
            if (!handled) {
                onError(floorError)
            } else {
                // 如果错误处理器处理了错误，通知恢复监听器
                onFloorRecoveryListener?.invoke(floorError, true)
            }
        }
    }
    
    /**
     * 处理楼层操作错误
     */
    private suspend fun handleFloorOperationError(error: FloorError, floorData: FloorData) {
        when (error) {
            is FloorError.CacheError -> {
                // 缓存错误，尝试直接更新适配器
                logError("缓存操作失败，跳过缓存", error)
            }
            is FloorError.LifecycleError -> {
                // 生命周期错误，停止操作
                logError("生命周期错误，停止楼层操作", error)
                return
            }
            else -> {
                // 其他错误，通知上层
                onFloorErrorListener?.invoke(error)
            }
        }
    }
    
    /**
     * 处理楼层数据加载错误
     */
    private suspend fun handleFloorDataLoadError(error: FloorError, floorData: FloorData) {
        when (error) {
            is FloorError.NetworkError -> {
                // 网络错误，尝试从缓存加载
                tryLoadFromCache(floorData)
            }
            is FloorError.FloorCreationError -> {
                // 楼层创建错误，使用默认楼层
                createFallbackFloor(floorData)
            }
            is FloorError.DataParseError -> {
                // 数据解析错误，显示错误状态
                showFloorErrorState(floorData, error)
            }
            else -> {
                onFloorErrorListener?.invoke(error)
            }
        }
    }
    
    /**
     * 重试加载楼层
     */
    private suspend fun retryLoadFloors(floorDataList: List<FloorData>, retryCount: Int = 0) {
        if (retryCount >= 3) {
            val error = FloorError.NetworkError.RequestFailed()
            onFloorErrorListener?.invoke(error)
            return
        }
        
        // 延迟重试
        delay(1000L * (retryCount + 1))
        
        try {
            loadFloors(floorDataList)
        } catch (e: Exception) {
            retryLoadFloors(floorDataList, retryCount + 1)
        }
    }
    
    /**
     * 从缓存加载楼层数据
     */
    private suspend fun loadFloorDataFromCache(floorDataList: List<FloorData>) {
        try {
            val cachedFloors = mutableListOf<FloorData>()
            
            for (floorData in floorDataList) {
                val cached = cacheManager.getCachedFloorConfig(floorData.floorId, floorData.cachePolicy)
                if (cached != null) {
                    cachedFloors.add(cached)
                }
            }
            
            if (cachedFloors.isNotEmpty()) {
                floorAdapter?.updateFloorData(cachedFloors)
            } else {
                val error = FloorError.CacheError.CacheReadError()
                onFloorErrorListener?.invoke(error)
            }
        } catch (e: Exception) {
            val error = FloorErrorUtils.convertThrowableToFloorError(e)
            onFloorErrorListener?.invoke(error)
        }
    }
    
    /**
     * 从缓存尝试加载单个楼层
     */
    private suspend fun tryLoadFromCache(floorData: FloorData) {
        try {
            val cached = cacheManager.getCachedFloorConfig(floorData.floorId, floorData.cachePolicy)
            if (cached != null) {
                // 使用缓存数据
                logError("网络加载失败，使用缓存数据", FloorError.NetworkError.RequestFailed(floorData = floorData))
            } else {
                // 缓存也没有，显示错误状态
                showFloorErrorState(floorData, FloorError.CacheError.CacheReadError(floorData = floorData))
            }
        } catch (e: Exception) {
            val error = FloorErrorUtils.convertThrowableToFloorError(e, floorData)
            showFloorErrorState(floorData, error)
        }
    }
    
    /**
     * 创建降级楼层
     */
    private suspend fun createFallbackFloor(floorData: FloorData) {
        try {
            // 尝试创建默认楼层，但不提供fallback创建器
            val fallbackFloor = FloorFactory.createFloorWithFallback(floorData, null)
            
            if (fallbackFloor != null) {
                logError("使用降级楼层", FloorError.FloorCreationError.FloorTypeNotRegistered(
                    floorData.floorType.typeName, floorData = floorData
                ))
            } else {
                showFloorErrorState(floorData, FloorError.FloorCreationError.FloorInstantiationFailed(
                    floorData.floorType.typeName, floorData = floorData
                ))
            }
        } catch (e: Exception) {
            val error = FloorErrorUtils.convertThrowableToFloorError(e, floorData)
            showFloorErrorState(floorData, error)
        }
    }
    
    /**
     * 显示错误状态
     */
    private fun showErrorState(error: FloorError) {
        // 可以在这里显示全局错误状态
        onFloorErrorListener?.invoke(error)
    }
    
    /**
     * 显示楼层错误状态
     */
    private fun showFloorErrorState(floorData: FloorData, error: FloorError) {
        // 可以在这里为特定楼层显示错误状态
        onFloorErrorListener?.invoke(error)
    }
    
    /**
     * 记录错误日志
     */
    private fun logError(message: String, error: FloorError) {
        // 可以在这里记录错误日志到文件或上报到服务器
        println("FloorManager Error: $message - ${error.errorMessage}")
        onFloorErrorListener?.invoke(error)
    }
    
    /**
     * 设置默认的错误处理策略
     */
    private fun setupDefaultErrorHandling() {
        configureErrorHandling {
            // 网络错误：自动重试
            onNetworkError(
                ErrorHandlingStrategy.RETRY,
                ErrorRecoveryAction.Retry(maxRetries = 3, delayMs = 2000)
            )
            
            // 数据解析错误：使用降级方案
            onDataParseError(
                ErrorHandlingStrategy.FALLBACK,
                ErrorRecoveryAction.Fallback {
                    // 静默处理，不显示错误
                }
            )
            
            // 缓存错误：忽略继续执行
            onCacheError(
                ErrorHandlingStrategy.IGNORE,
                ErrorRecoveryAction.None
            )
            
            // 楼层创建错误：使用降级方案
            onFloorCreationError(
                ErrorHandlingStrategy.FALLBACK,
                ErrorRecoveryAction.Fallback {
                    // 创建空楼层或跳过
                }
            )
            
            // 资源错误：使用降级方案
            onResourceError(
                ErrorHandlingStrategy.FALLBACK,
                ErrorRecoveryAction.Fallback {
                    // 使用默认资源
                }
            )
            
            // 生命周期错误：快速失败
            onLifecycleError(
                ErrorHandlingStrategy.FAIL_FAST,
                ErrorRecoveryAction.None
            )
        }
    }
    
    /**
     * 设置自动错误处理器
     */
    private fun setAutoErrorHandler() {
        // 如果已经设置了自定义错误监听器，不要覆盖
        if (onFloorErrorListener == null) {
            setOnFloorErrorListener { floorError ->
                if (enableAutoErrorHandling) {
                    // 自动处理错误，显示用户友好的提示
                    FloorErrorUtils.showDefaultErrorUI(context, floorError)
                }
            }
        }
    }
}

/**
 * 错误处理配置构建器
 */
class FloorErrorHandlingConfig {
    private val strategies = mutableMapOf<String, ErrorHandlingStrategy>()
    private val recoveryActions = mutableMapOf<String, ErrorRecoveryAction>()
    
    /**
     * 设置网络错误处理策略
     */
    fun onNetworkError(strategy: ErrorHandlingStrategy, action: ErrorRecoveryAction = ErrorRecoveryAction.None) {
        strategies["NETWORK"] = strategy
        recoveryActions["NETWORK"] = action
    }
    
    /**
     * 设置数据解析错误处理策略
     */
    fun onDataParseError(strategy: ErrorHandlingStrategy, action: ErrorRecoveryAction = ErrorRecoveryAction.None) {
        strategies["DATA"] = strategy
        recoveryActions["DATA"] = action
    }
    
    /**
     * 设置缓存错误处理策略
     */
    fun onCacheError(strategy: ErrorHandlingStrategy, action: ErrorRecoveryAction = ErrorRecoveryAction.None) {
        strategies["CACHE"] = strategy
        recoveryActions["CACHE"] = action
    }
    
    /**
     * 设置楼层创建错误处理策略
     */
    fun onFloorCreationError(strategy: ErrorHandlingStrategy, action: ErrorRecoveryAction = ErrorRecoveryAction.None) {
        strategies["FLOOR"] = strategy
        recoveryActions["FLOOR"] = action
    }
    
    /**
     * 设置资源错误处理策略
     */
    fun onResourceError(strategy: ErrorHandlingStrategy, action: ErrorRecoveryAction = ErrorRecoveryAction.None) {
        strategies["RESOURCE"] = strategy
        recoveryActions["RESOURCE"] = action
    }
    
    /**
     * 设置生命周期错误处理策略
     */
    fun onLifecycleError(strategy: ErrorHandlingStrategy, action: ErrorRecoveryAction = ErrorRecoveryAction.None) {
        strategies["LIFECYCLE"] = strategy
        recoveryActions["LIFECYCLE"] = action
    }
    
    /**
     * 设置特定错误码的处理策略
     */
    fun onErrorCode(errorCode: String, strategy: ErrorHandlingStrategy, action: ErrorRecoveryAction = ErrorRecoveryAction.None) {
        strategies[errorCode] = strategy
        recoveryActions[errorCode] = action
    }
    
    /**
     * 应用配置到错误处理器
     */
    internal fun applyTo(errorHandler: FloorErrorHandler) {
        strategies.forEach { (code, strategy) ->
            errorHandler.setErrorStrategy(code, strategy)
        }
        recoveryActions.forEach { (code, action) ->
            errorHandler.setRecoveryAction(code, action)
        }
    }
} 