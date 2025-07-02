package com.github.flexfloorlib.cache

import android.content.Context
import android.util.LruCache
import com.github.flexfloorlib.model.FloorData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * 楼层缓存管理器
 * 提供内存缓存和磁盘缓存功能，支持预加载和异步操作
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
        
        private const val CACHE_DIR_NAME = "floor_cache"
        private const val MEMORY_CACHE_SIZE = 50 // 内存缓存大小
        private const val DEFAULT_EXPIRE_TIME = 24 * 60 * 60 * 1000L // 默认过期时间：24小时
    }
    
    private val gson: Gson = Gson()
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 内存缓存
    private val memoryCache: LruCache<String, CacheItem> = LruCache(MEMORY_CACHE_SIZE)
    
    // 预加载任务映射表
    private val preloadTaskMap: ConcurrentHashMap<String, Job> = ConcurrentHashMap()
    
    // 缓存目录
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * 缓存楼层数据
     * @param key 缓存键
     * @param floorList 楼层数据列表
     * @param expireTime 过期时间（毫秒），0表示不过期
     */
    fun cacheFloorData(key: String, floorList: List<FloorData>, expireTime: Long = DEFAULT_EXPIRE_TIME) {
        val cacheItem = CacheItem(floorList, System.currentTimeMillis(), expireTime)
        
        // 写入内存缓存
        memoryCache.put(key, cacheItem)
        
        // 异步写入磁盘缓存
        coroutineScope.launch {
            writeToDisk(key, cacheItem)
        }
    }
    
    /**
     * 获取缓存的楼层数据
     * @param key 缓存键
     * @return 楼层数据列表，如果缓存不存在或已过期则返回null
     */
    suspend fun getCachedFloorData(key: String): List<FloorData>? {
        return withContext(Dispatchers.IO) {
            // 先从内存缓存获取
            var cacheItem: CacheItem? = memoryCache.get(key)
            
            // 如果内存缓存没有，从磁盘缓存读取
            if (cacheItem == null) {
                cacheItem = readFromDisk(key)
                cacheItem?.let { memoryCache.put(key, it) }
            }
            
            // 检查是否过期
            if (cacheItem?.isExpired() == false) {
                cacheItem.floorList
            } else {
                // 缓存过期，清除缓存
                if (cacheItem != null) {
                    clearCache(key)
                }
                null
            }
        }
    }
    
    /**
     * 同步获取缓存的楼层数据（仅从内存缓存获取）
     * @param key 缓存键
     * @return 楼层数据列表，如果缓存不存在或已过期则返回null
     */
    fun getCachedFloorDataSync(key: String): List<FloorData>? {
        val cacheItem: CacheItem? = memoryCache.get(key)
        return if (cacheItem?.isExpired() == false) {
            cacheItem.floorList
        } else {
            null
        }
    }
    
    /**
     * 预加载楼层数据
     * @param key 缓存键
     * @param dataLoader 数据加载器
     * @param expireTime 过期时间
     */
    fun preloadFloorData(
        key: String, 
        dataLoader: suspend () -> List<FloorData>?, 
        expireTime: Long = DEFAULT_EXPIRE_TIME
    ) {
        // 取消之前的预加载任务
        preloadTaskMap[key]?.cancel()
        
        val job = coroutineScope.launch {
            try {
                val floorList: List<FloorData>? = dataLoader()
                if (floorList != null) {
                    cacheFloorData(key, floorList, expireTime)
                }
            } catch (e: Exception) {
                // 预加载失败，记录日志但不抛出异常
                e.printStackTrace()
            }
        }
        
        preloadTaskMap[key] = job
    }
    
    /**
     * 清除指定键的缓存
     * @param key 缓存键
     */
    fun clearCache(key: String) {
        // 清除内存缓存
        memoryCache.remove(key)
        
        // 清除磁盘缓存
        coroutineScope.launch {
            val file = File(cacheDir, "$key.cache")
            if (file.exists()) {
                file.delete()
            }
        }
        
        // 取消预加载任务
        preloadTaskMap[key]?.cancel()
        preloadTaskMap.remove(key)
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        // 清除内存缓存
        memoryCache.evictAll()
        
        // 清除磁盘缓存
        coroutineScope.launch {
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".cache")) {
                    file.delete()
                }
            }
        }
        
        // 取消所有预加载任务
        preloadTaskMap.values.forEach { job ->
            job.cancel()
        }
        preloadTaskMap.clear()
    }
    
    /**
     * 获取缓存大小（内存 + 磁盘）
     * @return 缓存大小信息
     */
    suspend fun getCacheSize(): CacheSize {
        return withContext(Dispatchers.IO) {
            val memorySize: Int = memoryCache.size()
            val diskSize: Long = cacheDir.listFiles()?.sumOf { file ->
                if (file.name.endsWith(".cache")) file.length() else 0L
            } ?: 0L
            
            CacheSize(memorySize, diskSize)
        }
    }
    
    /**
     * 检查缓存是否存在且未过期
     * @param key 缓存键
     * @return 是否存在有效缓存
     */
    fun hasValidCache(key: String): Boolean {
        val cacheItem: CacheItem? = memoryCache.get(key)
        return cacheItem?.isExpired() == false
    }
    
    /**
     * 批量预加载楼层数据
     * @param preloadMap 预加载映射表，key为缓存键，value为数据加载器
     */
    fun batchPreload(preloadMap: Map<String, suspend () -> List<FloorData>?>) {
        preloadMap.forEach { (key, dataLoader) ->
            preloadFloorData(key, dataLoader)
        }
    }
    
    /**
     * 销毁缓存管理器
     */
    fun destroy() {
        coroutineScope.cancel()
        memoryCache.evictAll()
        preloadTaskMap.clear()
    }
    
    /**
     * 写入磁盘缓存
     */
    private fun writeToDisk(key: String, cacheItem: CacheItem) {
        try {
            val file = File(cacheDir, "$key.cache")
            FileWriter(file).use { writer ->
                gson.toJson(cacheItem, writer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    /**
     * 从磁盘缓存读取
     */
    private fun readFromDisk(key: String): CacheItem? {
        return try {
            val file = File(cacheDir, "$key.cache")
            if (file.exists()) {
                FileReader(file).use { reader ->
                    gson.fromJson(reader, CacheItem::class.java)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * 缓存项数据类
 */
private data class CacheItem(
    val floorList: List<FloorData>,
    val cacheTime: Long,
    val expireTime: Long
) {
    /**
     * 检查是否过期
     */
    fun isExpired(): Boolean {
        return if (expireTime == 0L) {
            false // 永不过期
        } else {
            System.currentTimeMillis() - cacheTime > expireTime
        }
    }
}

/**
 * 缓存大小信息
 */
data class CacheSize(
    val memoryCount: Int,
    val diskSize: Long
) {
    fun getDiskSizeInMB(): Double {
        return diskSize / (1024.0 * 1024.0)
    }
} 