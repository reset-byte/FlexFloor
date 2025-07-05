package com.github.flexfloorlib.core

import android.content.Context
import com.github.flexfloorlib.cache.FloorCacheManager
import com.github.flexfloorlib.model.CachePolicy
import com.github.flexfloorlib.model.FloorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 楼层数据管理仓库
 * 协调网络、缓存数据源之间的交互
 */
class FloorRepository private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: FloorRepository? = null
        
        fun getInstance(context: Context): FloorRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloorRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val cacheManager = FloorCacheManager.getInstance(context)
    
    // 远程数据源接口（由应用程序实现）
    private var remoteDataSource: FloorRemoteDataSource? = null
    
    /**
     * 设置远程数据源
     */
    fun setRemoteDataSource(dataSource: FloorRemoteDataSource) {
        this.remoteDataSource = dataSource
    }
    
    /**
     * 从远程数据源加载楼层配置
     */
    suspend fun loadFloorConfig(pageId: String, useCache: Boolean = true): List<FloorData> {
        return withContext(Dispatchers.IO) {
            try {
                // 如果启用缓存，首先尝试缓存
                if (useCache) {
                    val cachedConfig = getCachedFloorConfig(pageId)
                    if (cachedConfig.isNotEmpty()) {
                        return@withContext cachedConfig
                    }
                }
                
                // 尝试远程数据源
                val remoteConfig = remoteDataSource?.loadFloorConfig(pageId)
                if (!remoteConfig.isNullOrEmpty()) {
                    // 缓存结果
                    cacheFloorConfig(pageId, remoteConfig)
                    return@withContext remoteConfig
                }
                
                // 如果远程数据源也没有数据，返回空列表
                emptyList()
                
            } catch (e: Exception) {
                // 出错时回退到缓存数据
                getCachedFloorConfig(pageId)
            }
        }
    }
    
    /**
     * 加载楼层业务数据
     */
    suspend fun loadFloorData(floorId: String, floorType: String, params: Map<String, Any> = emptyMap()): Any? {
        return withContext(Dispatchers.IO) {
            try {
                // 首先尝试缓存
                val cacheKey = "data_${floorId}_${floorType}"
                val cachedData = cacheManager.getCachedFloorData(cacheKey, CachePolicy.BOTH)
                if (cachedData != null) {
                    return@withContext cachedData
                }
                
                // 从远程加载
                val remoteData = remoteDataSource?.loadFloorData(floorId, floorType, params)
                if (remoteData != null) {
                    // 缓存结果
                    cacheManager.cacheFloorData(cacheKey, remoteData, CachePolicy.BOTH)
                    return@withContext remoteData
                }
                
                // 如果远程数据源也没有数据，返回null
                null
                
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * 缓存楼层配置
     */
    private suspend fun cacheFloorConfig(pageId: String, floorConfig: List<FloorData>) {
        val cacheKey = "config_$pageId"
        cacheManager.cacheFloorData(cacheKey, floorConfig, CachePolicy.BOTH)
        
        // 缓存单个楼层配置
        floorConfig.forEach { floorData ->
            cacheManager.cacheFloorConfig(floorData.floorId, floorData)
        }
    }
    
    /**
     * 获取缓存的楼层配置
     */
    private suspend fun getCachedFloorConfig(pageId: String): List<FloorData> {
        val cacheKey = "config_$pageId"
        @Suppress("UNCHECKED_CAST")
        return cacheManager.getCachedFloorData(cacheKey, CachePolicy.BOTH) as? List<FloorData> ?: emptyList()
    }
    
    /**
     * 更新楼层配置
     */
    suspend fun updateFloorConfig(pageId: String, floorConfig: List<FloorData>) {
        cacheFloorConfig(pageId, floorConfig)
        
        // 可选择同步到远程
        remoteDataSource?.updateFloorConfig(pageId, floorConfig)
    }
    
    /**
     * 清除特定页面的缓存
     */
    suspend fun clearPageCache(pageId: String) {
        val cacheKey = "config_$pageId"
        cacheManager.clearMemoryCache()
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache() {
        cacheManager.clearAllCaches()
    }
}

/**
 * 远程楼层数据源接口
 */
interface FloorRemoteDataSource {
    suspend fun loadFloorConfig(pageId: String): List<FloorData>?
    suspend fun loadFloorData(floorId: String, floorType: String, params: Map<String, Any>): Any?
    suspend fun updateFloorConfig(pageId: String, floorConfig: List<FloorData>): Boolean
} 