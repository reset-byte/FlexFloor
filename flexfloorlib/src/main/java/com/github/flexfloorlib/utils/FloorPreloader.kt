package com.github.flexfloorlib.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.LoadPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Preloader for floor data to improve performance
 */
class FloorPreloader(
    private val recyclerView: RecyclerView,
    private val preloadDistance: Int = 3,
    private val onPreloadFloor: (FloorData) -> Unit
) {
    
    private val preloaderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val preloadedFloors = ConcurrentHashMap<String, Boolean>()
    private val preloadQueue = mutableListOf<FloorData>()
    
    private var isAttached = false
    private var floors = listOf<FloorData>()
    
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            checkPreloadConditions()
        }
    }
    
    /**
     * Start preloading with floor data
     */
    fun startPreloading(floorDataList: List<FloorData>) {
        this.floors = floorDataList
        
        if (!isAttached) {
            recyclerView.addOnScrollListener(scrollListener)
            isAttached = true
        }
        
        // 初始预加载检查
        checkPreloadConditions()
        
        // 处理预加载队列
        processPreloadQueue()
    }
    
    /**
     * Schedule a floor for preloading
     */
    fun schedulePreload(floorData: FloorData) {
        if (!preloadedFloors.containsKey(floorData.floorId)) {
            preloadQueue.add(floorData)
        }
    }
    
    /**
     * Check if floors need to be preloaded based on scroll position
     */
    private fun checkPreloadConditions() {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val totalItemCount = layoutManager.itemCount
        
        // Calculate preload range
        val preloadStartPosition = lastVisiblePosition + 1
        val preloadEndPosition = minOf(
            preloadStartPosition + preloadDistance - 1,
            totalItemCount - 1
        )
        
        // Preload floors in range
        for (position in preloadStartPosition..preloadEndPosition) {
            if (position < floors.size) {
                val floorData = floors[position]
                if (shouldPreloadFloor(floorData)) {
                    preloadFloor(floorData)
                }
            }
        }
    }
    
    /**
     * Check if a floor should be preloaded
     */
    private fun shouldPreloadFloor(floorData: FloorData): Boolean {
        return when {
            preloadedFloors.containsKey(floorData.floorId) -> false
            floorData.loadPolicy == LoadPolicy.LAZY -> false
            else -> true
        }
    }
    
    /**
     * Preload a specific floor
     */
    private fun preloadFloor(floorData: FloorData) {
        preloadedFloors[floorData.floorId] = true
        
        preloaderScope.launch {
            try {
                // Add small delay to avoid blocking UI
                delay(50)
                onPreloadFloor(floorData)
            } catch (e: Exception) {
                e.printStackTrace()
                // Remove from preloaded set on failure
                preloadedFloors.remove(floorData.floorId)
            }
        }
    }
    
    /**
     * Process floors in preload queue
     */
    private fun processPreloadQueue() {
        preloaderScope.launch {
            while (preloadQueue.isNotEmpty()) {
                val floorData = preloadQueue.removeFirstOrNull() ?: break
                
                if (shouldPreloadFloor(floorData)) {
                    preloadFloor(floorData)
                    
                    // Add delay between preloads to prevent overwhelming
                    delay(100)
                }
            }
        }
    }
    
    /**
     * Clear preloaded floors cache
     */
    fun clearPreloadedCache() {
        preloadedFloors.clear()
    }
    
    /**
     * Check if a floor is preloaded
     */
    fun isFloorPreloaded(floorId: String): Boolean {
        return preloadedFloors.containsKey(floorId)
    }
    
    /**
     * Get preload statistics
     */
    fun getPreloadStats(): Map<String, Int> {
        return mapOf(
            "preloaded_count" to preloadedFloors.size,
            "queue_size" to preloadQueue.size,
            "total_floors" to floors.size
        )
    }
    
    /**
     * Destroy preloader and cleanup resources
     */
    fun destroy() {
        if (isAttached) {
            recyclerView.removeOnScrollListener(scrollListener)
            isAttached = false
        }
        
        preloadedFloors.clear()
        preloadQueue.clear()
        floors = emptyList()
    }
} 