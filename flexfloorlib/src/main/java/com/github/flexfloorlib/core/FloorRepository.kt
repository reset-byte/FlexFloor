package com.github.flexfloorlib.core

import android.content.Context
import com.github.flexfloorlib.cache.FloorCacheManager
import com.github.flexfloorlib.model.CachePolicy
import com.github.flexfloorlib.model.FloorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for floor data management
 * Coordinates between network, cache, and local data sources
 */
class FloorRepository private constructor(private val context: Context) {
    
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
    
    // Data source interfaces (to be implemented by app)
    private var remoteDataSource: FloorRemoteDataSource? = null
    private var localDataSource: FloorLocalDataSource? = null
    
    /**
     * Set remote data source
     */
    fun setRemoteDataSource(dataSource: FloorRemoteDataSource) {
        this.remoteDataSource = dataSource
    }
    
    /**
     * Set local data source
     */
    fun setLocalDataSource(dataSource: FloorLocalDataSource) {
        this.localDataSource = dataSource
    }
    
    /**
     * Load floor configuration from various sources
     */
    suspend fun loadFloorConfig(pageId: String, useCache: Boolean = true): List<FloorData> {
        return withContext(Dispatchers.IO) {
            try {
                // Try cache first if enabled
                if (useCache) {
                    val cachedConfig = getCachedFloorConfig(pageId)
                    if (cachedConfig.isNotEmpty()) {
                        return@withContext cachedConfig
                    }
                }
                
                // Try remote source
                val remoteConfig = remoteDataSource?.loadFloorConfig(pageId)
                if (!remoteConfig.isNullOrEmpty()) {
                    // Cache the result
                    cacheFloorConfig(pageId, remoteConfig)
                    return@withContext remoteConfig
                }
                
                // Fallback to local source
                localDataSource?.loadFloorConfig(pageId) ?: emptyList()
                
            } catch (e: Exception) {
                // Fallback to cached data on error
                getCachedFloorConfig(pageId)
            }
        }
    }
    
    /**
     * Load floor business data
     */
    suspend fun loadFloorData(floorId: String, floorType: String, params: Map<String, Any> = emptyMap()): Any? {
        return withContext(Dispatchers.IO) {
            try {
                // Try cache first
                val cacheKey = "data_${floorId}_${floorType}"
                val cachedData = cacheManager.getCachedFloorData(cacheKey, CachePolicy.BOTH)
                if (cachedData != null) {
                    return@withContext cachedData
                }
                
                // Load from remote
                val remoteData = remoteDataSource?.loadFloorData(floorId, floorType, params)
                if (remoteData != null) {
                    // Cache the result
                    cacheManager.cacheFloorData(cacheKey, remoteData, CachePolicy.BOTH)
                    return@withContext remoteData
                }
                
                // Fallback to local
                localDataSource?.loadFloorData(floorId, floorType, params)
                
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Cache floor configuration
     */
    private suspend fun cacheFloorConfig(pageId: String, floorConfig: List<FloorData>) {
        val cacheKey = "config_$pageId"
        cacheManager.cacheFloorData(cacheKey, floorConfig, CachePolicy.BOTH)
        
        // Cache individual floor configs
        floorConfig.forEach { floorData ->
            cacheManager.cacheFloorConfig(floorData.floorId, floorData)
        }
    }
    
    /**
     * Get cached floor configuration
     */
    private suspend fun getCachedFloorConfig(pageId: String): List<FloorData> {
        val cacheKey = "config_$pageId"
        @Suppress("UNCHECKED_CAST")
        return cacheManager.getCachedFloorData(cacheKey, CachePolicy.BOTH) as? List<FloorData> ?: emptyList()
    }
    
    /**
     * Update floor configuration
     */
    suspend fun updateFloorConfig(pageId: String, floorConfig: List<FloorData>) {
        cacheFloorConfig(pageId, floorConfig)
        
        // Optionally sync to remote
        remoteDataSource?.updateFloorConfig(pageId, floorConfig)
    }
    
    /**
     * Clear cache for specific page
     */
    suspend fun clearPageCache(pageId: String) {
        val cacheKey = "config_$pageId"
        cacheManager.clearMemoryCache()
    }
    
    /**
     * Clear all cache
     */
    suspend fun clearAllCache() {
        cacheManager.clearAllCaches()
    }
}

/**
 * Interface for remote floor data source
 */
interface FloorRemoteDataSource {
    suspend fun loadFloorConfig(pageId: String): List<FloorData>?
    suspend fun loadFloorData(floorId: String, floorType: String, params: Map<String, Any>): Any?
    suspend fun updateFloorConfig(pageId: String, floorConfig: List<FloorData>): Boolean
}

/**
 * Interface for local floor data source
 */
interface FloorLocalDataSource {
    suspend fun loadFloorConfig(pageId: String): List<FloorData>?
    suspend fun loadFloorData(floorId: String, floorType: String, params: Map<String, Any>): Any?
    suspend fun saveFloorConfig(pageId: String, floorConfig: List<FloorData>): Boolean
} 