package com.github.flexfloorlib.core

import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorType
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于楼层类型创建楼层实例的工厂
 * 线程安全的楼层注册和创建实现
 */
object FloorFactory {
    
    // 用于存储楼层创建器的线程安全映射
    private val floorCreators = ConcurrentHashMap<FloorType, () -> BaseFloor<*>>()
    private val customFloorCreators = ConcurrentHashMap<String, () -> BaseFloor<*>>()
    
    // 已创建楼层实例的缓存（可选，用于性能优化）
    private val floorInstanceCache = ConcurrentHashMap<String, BaseFloor<*>>()
    private var enableInstanceCache = false
    
    /**
     * 为特定楼层类型注册楼层创建器
     */
    fun <T : Any> registerFloor(floorType: FloorType, creator: () -> BaseFloor<T>) {
        floorCreators[floorType] = creator as () -> BaseFloor<*>
    }
    
    /**
     * 使用自定义类型名称注册自定义楼层创建器
     */
    fun <T : Any> registerCustomFloor(typeName: String, creator: () -> BaseFloor<T>) {
        customFloorCreators[typeName] = creator as () -> BaseFloor<*>
    }
    
    /**
     * 批量注册多个楼层类型
     */
    fun registerFloors(floorMap: Map<FloorType, () -> BaseFloor<*>>) {
        floorCreators.putAll(floorMap)
    }
    
    /**
     * 批量注册多个自定义楼层类型
     */
    fun registerCustomFloors(customFloorMap: Map<String, () -> BaseFloor<*>>) {
        customFloorCreators.putAll(customFloorMap)
    }
    
    /**
     * 基于楼层数据创建楼层实例
     */
    fun createFloor(floorData: FloorData): BaseFloor<*>? {
        // 如果启用缓存，首先检查缓存
        if (enableInstanceCache) {
            val cacheKey = generateCacheKey(floorData)
            floorInstanceCache[cacheKey]?.let { cachedFloor ->
                // 使用新数据重新初始化
                cachedFloor.initFloor(floorData, null)
                return cachedFloor
            }
        }
        
        val floor = when (floorData.floorType) {
            FloorType.CUSTOM -> {
                // 对于自定义楼层，通过业务数据中的自定义类型名称查找
                val customTypeName = floorData.businessData["customType"] as? String
                    ?: floorData.floorType.typeName
                createCustomFloor(customTypeName)
            }
            else -> {
                createStandardFloor(floorData.floorType)
            }
        }
        
        return floor?.apply {
            initFloor(floorData, null)
            
            // 如果启用缓存，则缓存实例
            if (enableInstanceCache) {
                val cacheKey = generateCacheKey(floorData)
                floorInstanceCache[cacheKey] = this
            }
        }
    }
    
    /**
     * 按类型创建标准楼层
     */
    private fun createStandardFloor(floorType: FloorType): BaseFloor<*>? {
        return try {
            floorCreators[floorType]?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 按类型名称创建自定义楼层
     */
    private fun createCustomFloor(typeName: String): BaseFloor<*>? {
        return try {
            customFloorCreators[typeName]?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 为楼层实例生成缓存键
     */
    private fun generateCacheKey(floorData: FloorData): String {
        return "${floorData.floorType.typeName}_${floorData.floorId}"
    }
    
    /**
     * 检查楼层类型是否已注册
     */
    fun isFloorTypeRegistered(floorType: FloorType): Boolean {
        return when (floorType) {
            FloorType.CUSTOM -> false // 自定义楼层需要特定的类型名称检查
            else -> floorCreators.containsKey(floorType)
        }
    }
    
    /**
     * 检查自定义楼层类型是否已注册
     */
    fun isCustomFloorTypeRegistered(typeName: String): Boolean {
        return customFloorCreators.containsKey(typeName)
    }
    
    /**
     * 检查任何楼层类型（标准或自定义）是否已注册
     */
    fun isAnyFloorTypeRegistered(floorData: FloorData): Boolean {
        return when (floorData.floorType) {
            FloorType.CUSTOM -> {
                val customTypeName = floorData.businessData["customType"] as? String
                    ?: floorData.floorType.typeName
                isCustomFloorTypeRegistered(customTypeName)
            }
            else -> isFloorTypeRegistered(floorData.floorType)
        }
    }
    
    /**
     * 获取所有已注册的楼层类型
     */
    fun getRegisteredFloorTypes(): Set<FloorType> {
        return floorCreators.keys.toSet()
    }
    
    /**
     * 获取所有已注册的自定义楼层类型
     */
    fun getRegisteredCustomFloorTypes(): Set<String> {
        return customFloorCreators.keys.toSet()
    }
    
    /**
     * 获取已注册楼层的总数
     */
    fun getRegisteredFloorCount(): Int {
        return floorCreators.size + customFloorCreators.size
    }
    
    /**
     * 取消注册楼层类型
     */
    fun unregisterFloor(floorType: FloorType) {
        floorCreators.remove(floorType)
        clearRelatedCache(floorType.typeName)
    }
    
    /**
     * 取消注册自定义楼层类型
     */
    fun unregisterCustomFloor(typeName: String) {
        customFloorCreators.remove(typeName)
        clearRelatedCache(typeName)
    }
    
    /**
     * 清除相关缓存条目
     */
    private fun clearRelatedCache(typeName: String) {
        if (enableInstanceCache) {
            val keysToRemove = floorInstanceCache.keys.filter { it.startsWith(typeName) }
            keysToRemove.forEach { key ->
                floorInstanceCache[key]?.onDestroy()
                floorInstanceCache.remove(key)
            }
        }
    }
    
    /**
     * 启用或禁用楼层实例缓存
     */
    fun setInstanceCacheEnabled(enabled: Boolean) {
        this.enableInstanceCache = enabled
        if (!enabled) {
            clearInstanceCache()
        }
    }
    
    /**
     * 清除楼层实例缓存
     */
    fun clearInstanceCache() {
        floorInstanceCache.values.forEach { it.onDestroy() }
        floorInstanceCache.clear()
    }
    
    /**
     * 获取缓存的楼层实例
     */
    fun getCachedFloorInstance(floorData: FloorData): BaseFloor<*>? {
        if (!enableInstanceCache) return null
        
        val cacheKey = generateCacheKey(floorData)
        return floorInstanceCache[cacheKey]
    }
    
    /**
     * 清除所有已注册的楼层（用于测试）
     */
    fun clearAllRegistrations() {
        floorCreators.clear()
        customFloorCreators.clear()
        clearInstanceCache()
    }
    
    /**
     * 获取工厂统计信息
     */
    fun getFactoryStats(): FloorFactoryStats {
        return FloorFactoryStats(
            standardFloorTypes = floorCreators.size,
            customFloorTypes = customFloorCreators.size,
            cachedInstances = if (enableInstanceCache) floorInstanceCache.size else 0,
            isCacheEnabled = enableInstanceCache
        )
    }
    
    /**
     * 验证楼层注册
     */
    fun validateFloorRegistration(floorData: FloorData): FloorValidationResult {
        val isRegistered = isAnyFloorTypeRegistered(floorData)
        
        return if (isRegistered) {
            // 尝试创建实例来验证
            val floor = createFloor(floorData)
            if (floor != null) {
                FloorValidationResult.Success
            } else {
                FloorValidationResult.CreationFailed("楼层创建失败，类型: ${floorData.floorType}")
            }
        } else {
            FloorValidationResult.NotRegistered("楼层类型未注册: ${floorData.floorType}")
        }
    }
    
    /**
     * 预验证楼层数据列表
     */
    fun validateFloorDataList(floorDataList: List<FloorData>): List<Pair<FloorData, FloorValidationResult>> {
        return floorDataList.map { floorData ->
            floorData to validateFloorRegistration(floorData)
        }
    }
    
    /**
     * 使用降级策略创建楼层
     */
    fun createFloorWithFallback(
        floorData: FloorData, 
        fallbackCreator: (() -> BaseFloor<*>)? = null
    ): BaseFloor<*>? {
        // 首先尝试正常创建
        val floor = createFloor(floorData)
        if (floor != null) return floor
        
        // 尝试降级创建器
        return try {
            fallbackCreator?.invoke()?.apply {
                initFloor(floorData, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * 工厂统计信息数据类
 */
data class FloorFactoryStats(
    val standardFloorTypes: Int,
    val customFloorTypes: Int,
    val cachedInstances: Int,
    val isCacheEnabled: Boolean
) {
    val totalFloorTypes: Int get() = standardFloorTypes + customFloorTypes
}

/**
 * 楼层验证结果密封类
 */
sealed class FloorValidationResult {
    object Success : FloorValidationResult()
    data class NotRegistered(val message: String) : FloorValidationResult()
    data class CreationFailed(val message: String) : FloorValidationResult()
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = !isSuccess
} 