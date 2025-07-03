package com.github.flexfloorlib.cache

import android.annotation.SuppressLint
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
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: FloorCacheManager? = null
        
        fun getInstance(context: Context): FloorCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloorCacheManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // 内存缓存
    private val memoryCache = ConcurrentHashMap<String, Any>()
    private val memoryCacheTimestamps = ConcurrentHashMap<String, Long>()
    
    // 缓存配置
    private var maxMemoryCacheSize = 50 // 内存缓存最大条目数
    private var memoryTtl = 30 * 60 * 1000L // 内存缓存30分钟TTL
    private var diskTtl = 24 * 60 * 60 * 1000L // 磁盘缓存24小时TTL
    
    // 缓存目录
    private val diskCacheDir by lazy {
        File(context.cacheDir, "floor_cache").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * 缓存楼层数据
     *
     * @param key 缓存键
     * @param data 要缓存的数据
     * @param policy 缓存策略
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
                // 不执行任何操作
            }
        }
    }
    
    /**
     * 获取缓存的楼层数据
     *
     * @param key 缓存键
     * @param policy 缓存策略
     * @return 缓存的数据，如果不存在则返回null
     */
    suspend fun getCachedFloorData(key: String, policy: CachePolicy = CachePolicy.MEMORY): Any? {
        return when (policy) {
            CachePolicy.MEMORY -> getFromMemory(key)
            CachePolicy.DISK -> getFromDisk(key)
            CachePolicy.BOTH -> {
                getFromMemory(key) ?: getFromDisk(key)?.also { data ->
                    // 提升到内存缓存
                    cacheToMemory(key, data)
                }
            }
            CachePolicy.NONE -> null
        }
    }
    
    /**
     * 缓存楼层配置
     *
     * @param floorId 楼层ID
     * @param floorData 楼层数据
     */
    suspend fun cacheFloorConfig(floorId: String, floorData: FloorData) {
        val key = "config_$floorId"
        cacheFloorData(key, floorData, floorData.cachePolicy)
    }
    
    /**
     * 获取缓存的楼层配置
     *
     * @param floorId 楼层ID
     * @param policy 缓存策略
     * @return 缓存的楼层数据，如果不存在则返回null
     */
    suspend fun getCachedFloorConfig(floorId: String, policy: CachePolicy = CachePolicy.MEMORY): FloorData? {
        val key = "config_$floorId"
        return getCachedFloorData(key, policy) as? FloorData
    }
    
    /**
     * 缓存到内存
     *
     * @param key 缓存键
     * @param data 要缓存的数据
     */
    private fun cacheToMemory(key: String, data: Any) {
        // 检查缓存大小限制
        if (memoryCache.size >= maxMemoryCacheSize) {
            evictOldestMemoryCache()
        }
        
        memoryCache[key] = data
        memoryCacheTimestamps[key] = System.currentTimeMillis()
    }
    
    /**
     * 从内存缓存获取数据
     *
     * @param key 缓存键
     * @return 缓存的数据，如果不存在或过期则返回null
     */
    private fun getFromMemory(key: String): Any? {
        val timestamp = memoryCacheTimestamps[key] ?: return null
        val currentTime = System.currentTimeMillis()
        
        // 检查TTL
        if (currentTime - timestamp > memoryTtl) {
            memoryCache.remove(key)
            memoryCacheTimestamps.remove(key)
            return null
        }
        
        return memoryCache[key]
    }
    
    /**
     * 缓存到磁盘
     *
     * @param key 缓存键
     * @param data 要缓存的数据
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
     * 从磁盘缓存获取数据
     *
     * @param key 缓存键
     * @return 缓存的数据，如果不存在或过期则返回null
     */
    private suspend fun getFromDisk(key: String): Any? = withContext(Dispatchers.IO) {
        try {
            val file = File(diskCacheDir, "${key.hashCode()}.cache")
            if (!file.exists()) return@withContext null
            
            FileInputStream(file).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    val wrapper = ois.readObject() as CacheWrapper
                    val currentTime = System.currentTimeMillis()
                    
                    // 检查TTL
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
     * 移除最旧的内存缓存条目
     */
    private fun evictOldestMemoryCache() {
        val oldestEntry = memoryCacheTimestamps.minByOrNull { it.value }
        oldestEntry?.let { (key, _) ->
            memoryCache.remove(key)
            memoryCacheTimestamps.remove(key)
        }
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearAllCaches() {
        memoryCache.clear()
        memoryCacheTimestamps.clear()
        
        withContext(Dispatchers.IO) {
            diskCacheDir.listFiles()?.forEach { it.delete() }
        }
    }
    
    /**
     * 仅清除内存缓存
     */
    fun clearMemoryCache() {
        memoryCache.clear()
        memoryCacheTimestamps.clear()
    }
    
    /**
     * 仅清除磁盘缓存
     */
    suspend fun clearDiskCache() = withContext(Dispatchers.IO) {
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }
    
    /**
     * 配置缓存设置
     *
     * @param maxMemorySize 内存缓存最大条目数
     * @param memoryTtlMillis 内存缓存TTL（毫秒）
     * @param diskTtlMillis 磁盘缓存TTL（毫秒）
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
     * 磁盘存储的缓存包装器
     */
    private data class CacheWrapper(
        val data: Any,
        val timestamp: Long
    ) : java.io.Serializable
} 