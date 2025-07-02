package com.github.flexfloorlib.manager

import android.util.LruCache
import com.github.flexfloorlib.base.IFloor
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorType
import java.util.concurrent.ConcurrentHashMap

/**
 * 楼层管理器
 * 负责楼层的注册、创建、缓存和生命周期管理
 */
class FloorManager {
    
    companion object {
        @Volatile
        private var INSTANCE: FloorManager? = null
        
        fun getInstance(): FloorManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloorManager().also { INSTANCE = it }
            }
        }
    }
    
    // 楼层工厂映射表
    private val floorFactoryMap: ConcurrentHashMap<String, () -> IFloor> = ConcurrentHashMap()
    
    // 楼层实例缓存，使用LRU缓存避免内存泄漏
    private val floorInstanceCache: LruCache<String, IFloor> = LruCache(50)
    
    // 楼层视图池映射表
    private val floorViewPoolMap: ConcurrentHashMap<String, FloorViewPool> = ConcurrentHashMap()
    
    /**
     * 注册楼层工厂
     * @param floorType 楼层类型
     * @param factory 楼层工厂方法
     */
    fun registerFloor(floorType: String, factory: () -> IFloor) {
        floorFactoryMap[floorType] = factory
        // 初始化视图池
        floorViewPoolMap[floorType] = FloorViewPool()
    }
    
    /**
     * 注册楼层工厂（使用枚举类型）
     */
    fun registerFloor(floorType: FloorType, factory: () -> IFloor) {
        registerFloor(floorType.typeName, factory)
    }
    
    /**
     * 批量注册楼层工厂
     * @param factoryMap 楼层工厂映射表
     */
    fun registerFloors(factoryMap: Map<String, () -> IFloor>) {
        factoryMap.forEach { (floorType, factory) ->
            registerFloor(floorType, factory)
        }
    }
    
    /**
     * 创建楼层实例
     * @param floorData 楼层数据
     * @return 楼层实例，如果不存在对应的工厂则返回null
     */
    fun createFloor(floorData: FloorData): IFloor? {
        val floorType: String = floorData.floorType.typeName
        return createFloor(floorType)
    }
    
    /**
     * 创建楼层实例
     * @param floorType 楼层类型
     * @return 楼层实例，如果不存在对应的工厂则返回null
     */
    fun createFloor(floorType: String): IFloor? {
        // 先从缓存中获取
        val cachedFloor: IFloor? = floorInstanceCache.get(floorType)
        if (cachedFloor != null) {
            return cachedFloor
        }
        
        // 从工厂创建新实例
        val factory: (() -> IFloor)? = floorFactoryMap[floorType]
        return factory?.invoke()?.also { floor ->
            // 缓存新创建的实例
            floorInstanceCache.put(floorType, floor)
            // 调用楼层的onCreate方法
            floor.onCreate()
        }
    }
    
    /**
     * 获取楼层视图池
     * @param floorType 楼层类型
     * @return 视图池实例
     */
    fun getFloorViewPool(floorType: String): FloorViewPool? {
        return floorViewPoolMap[floorType]
    }
    
    /**
     * 检查楼层类型是否已注册
     * @param floorType 楼层类型
     * @return 是否已注册
     */
    fun isFloorRegistered(floorType: String): Boolean {
        return floorFactoryMap.containsKey(floorType)
    }
    
    /**
     * 获取所有已注册的楼层类型
     * @return 楼层类型列表
     */
    fun getRegisteredFloorTypes(): Set<String> {
        return floorFactoryMap.keys.toSet()
    }
    
    /**
     * 清除楼层缓存
     * @param floorType 楼层类型，为null时清除所有缓存
     */
    fun clearFloorCache(floorType: String? = null) {
        if (floorType != null) {
            val floor: IFloor? = floorInstanceCache.get(floorType)
            floor?.onDestroy()
            floorInstanceCache.remove(floorType)
        } else {
            // 清除所有缓存
            val snapshot = floorInstanceCache.snapshot()
            for ((key, floor) in snapshot) {
                floor?.onDestroy()
            }
            floorInstanceCache.evictAll()
        }
    }
    
    /**
     * 销毁楼层管理器
     */
    fun destroy() {
        clearFloorCache()
        floorFactoryMap.clear()
        floorViewPoolMap.clear()
    }
    
    /**
     * 预加载楼层
     * @param floorTypes 需要预加载的楼层类型列表
     */
    fun preloadFloors(floorTypes: List<String>) {
        floorTypes.forEach { floorType ->
            if (isFloorRegistered(floorType) && floorInstanceCache.get(floorType) == null) {
                createFloor(floorType)
            }
        }
    }
}

/**
 * 楼层视图池
 * 用于复用楼层视图，提升性能
 */
class FloorViewPool {
    private val viewPool: ArrayDeque<Any> = ArrayDeque()
    private val maxPoolSize: Int = 10
    
    /**
     * 获取视图对象
     * @return 视图对象，如果池为空则返回null
     */
    fun getView(): Any? {
        return if (viewPool.isNotEmpty()) {
            viewPool.removeFirst()
        } else {
            null
        }
    }
    
    /**
     * 回收视图对象
     * @param view 要回收的视图对象
     */
    fun recycleView(view: Any) {
        if (viewPool.size < maxPoolSize) {
            viewPool.addLast(view)
        }
    }
    
    /**
     * 清空视图池
     */
    fun clear() {
        viewPool.clear()
    }
    
    /**
     * 获取池中视图数量
     */
    fun size(): Int {
        return viewPool.size
    }
} 