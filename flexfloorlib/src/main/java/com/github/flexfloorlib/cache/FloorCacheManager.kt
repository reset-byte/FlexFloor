package com.github.flexfloorlib.cache

import android.content.Context
import com.github.flexfloorlib.model.CachePolicy
import com.github.flexfloorlib.model.FloorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * 楼层数据和配置的缓存管理器
 */
class FloorCacheManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: FloorCacheManager? = null
        
        fun getInstance(context: Context): FloorCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloorCacheManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Memory cache
    private val memoryCache = ConcurrentHashMap<String, Any>()
    private val memoryCacheTimestamps = ConcurrentHashMap<String, Long>()
    
    // Cache configuration
    private var maxMemoryCacheSize = 50 // Maximum number of items in memory cache
    private var memoryTtl = 30 * 60 * 1000L // 30 minutes TTL for memory cache
    private var diskTtl = 24 * 60 * 60 * 1000L // 24 hours TTL for disk cache
    
    // Cache directories
    private val diskCacheDir by lazy {
        File(context.cacheDir, "floor_cache").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Cache floor data
     */
    suspend fun cacheFloorData(key: String, data: Any, policy: CachePolicy = CachePolicy.MEMORY) {
        when (policy) {
            CachePolicy.MEMORY -> cacheToMemory(key, data)
            CachePolicy.DISK -> cacheToDisk(key, data)
            CachePolicy.BOTH -> {
                cacheToMemory(key, data)
                cacheToDisk(key, data)
            }
            CachePolicy.NONE -> {
                // Do nothing
            }
        }
    }
    
    /**
     * Get cached floor data
     */
    suspend fun getCachedFloorData(key: String, policy: CachePolicy = CachePolicy.MEMORY): Any? {
        return when (policy) {
            CachePolicy.MEMORY -> getFromMemory(key)
            CachePolicy.DISK -> getFromDisk(key)
            CachePolicy.BOTH -> {
                getFromMemory(key) ?: getFromDisk(key)?.also { data ->
                    // Promote to memory cache
                    cacheToMemory(key, data)
                }
            }
            CachePolicy.NONE -> null
        }
    }
    
    /**
     * Cache floor configuration
     */
    suspend fun cacheFloorConfig(floorId: String, floorData: FloorData) {
        val key = "config_$floorId"
        cacheFloorData(key, floorData, floorData.cachePolicy)
    }
    
    /**
     * Get cached floor configuration
     */
    suspend fun getCachedFloorConfig(floorId: String, policy: CachePolicy = CachePolicy.MEMORY): FloorData? {
        val key = "config_$floorId"
        return getCachedFloorData(key, policy) as? FloorData
    }
    
    /**
     * Cache to memory
     */
    private fun cacheToMemory(key: String, data: Any) {
        // Check cache size limit
        if (memoryCache.size >= maxMemoryCacheSize) {
            evictOldestMemoryCache()
        }
        
        memoryCache[key] = data
        memoryCacheTimestamps[key] = System.currentTimeMillis()
    }
    
    /**
     * Get from memory cache
     */
    private fun getFromMemory(key: String): Any? {
        val timestamp = memoryCacheTimestamps[key] ?: return null
        val currentTime = System.currentTimeMillis()
        
        // Check TTL
        if (currentTime - timestamp > memoryTtl) {
            memoryCache.remove(key)
            memoryCacheTimestamps.remove(key)
            return null
        }
        
        return memoryCache[key]
    }
    
    /**
     * Cache to disk
     */
    private suspend fun cacheToDisk(key: String, data: Any) = withContext(Dispatchers.IO) {
        try {
            val file = File(diskCacheDir, "${key.hashCode()}.cache")
            val wrapper = CacheWrapper(data, System.currentTimeMillis())
            
            FileOutputStream(file).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(wrapper)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get from disk cache
     */
    private suspend fun getFromDisk(key: String): Any? = withContext(Dispatchers.IO) {
        try {
            val file = File(diskCacheDir, "${key.hashCode()}.cache")
            if (!file.exists()) return@withContext null
            
            FileInputStream(file).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    val wrapper = ois.readObject() as CacheWrapper
                    val currentTime = System.currentTimeMillis()
                    
                    // Check TTL
                    if (currentTime - wrapper.timestamp > diskTtl) {
                        file.delete()
                        return@withContext null
                    }
                    
                    return@withContext wrapper.data
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    /**
     * Evict oldest memory cache entry
     */
    private fun evictOldestMemoryCache() {
        val oldestEntry = memoryCacheTimestamps.minByOrNull { it.value }
        oldestEntry?.let { (key, _) ->
            memoryCache.remove(key)
            memoryCacheTimestamps.remove(key)
        }
    }
    
    /**
     * Clear all caches
     */
    suspend fun clearAllCaches() {
        memoryCache.clear()
        memoryCacheTimestamps.clear()
        
        withContext(Dispatchers.IO) {
            diskCacheDir.listFiles()?.forEach { it.delete() }
        }
    }
    
    /**
     * Clear memory cache only
     */
    fun clearMemoryCache() {
        memoryCache.clear()
        memoryCacheTimestamps.clear()
    }
    
    /**
     * Clear disk cache only
     */
    suspend fun clearDiskCache() = withContext(Dispatchers.IO) {
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }
    
    /**
     * Configure cache settings
     */
    fun configureCacheSettings(
        maxMemorySize: Int = 50,
        memoryTtlMillis: Long = 30 * 60 * 1000L,
        diskTtlMillis: Long = 24 * 60 * 60 * 1000L
    ) {
        this.maxMemoryCacheSize = maxMemorySize
        this.memoryTtl = memoryTtlMillis
        this.diskTtl = diskTtlMillis
    }
    
    /**
     * Cache wrapper for disk storage
     */
    private data class CacheWrapper(
        val data: Any,
        val timestamp: Long
    ) : java.io.Serializable
} 