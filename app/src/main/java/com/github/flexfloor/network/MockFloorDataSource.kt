package com.github.flexfloor.network

import android.content.Context
import com.github.flexfloor.utils.FloorDataMapper
import com.github.flexfloorlib.core.FloorRemoteDataSource
import com.github.flexfloorlib.model.FloorData
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream

/**
 * 模拟楼层数据源
 * 单次调用，立即返回骨架屏，5秒后通过回调更新真实数据
 */
class MockFloorDataSource(private val context: Context) : FloorRemoteDataSource {

    private val gson = Gson()
    private val networkDelayMs = 5000L // 模拟网络延迟5000ms
    private val dataSourceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedFullData: List<FloorData>? = null
    
    // 数据更新回调
    private var onDataUpdateCallback: ((List<FloorData>) -> Unit)? = null

    /**
     * 加载楼层配置 - 立即返回骨架屏，后台加载真实数据
     */
    override suspend fun loadFloorConfig(pageId: String): List<FloorData>? {
        return try {
            // 立即返回骨架屏配置
            val skeletonConfig = loadSkeletonConfig()
            
            // 启动后台任务加载真实数据
            startBackgroundDataLoading()
            
            skeletonConfig
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 加载骨架屏配置（立即返回）
     */
    private fun loadSkeletonConfig(): List<FloorData> {
        // 从assets中读取JSON数据
        val jsonString = readJsonFromAssets("floor_demo_data.json")
        val floorConfigResponse = gson.fromJson(jsonString, FloorConfigResponse::class.java)
        
        if (floorConfigResponse.success && floorConfigResponse.data != null) {
            val fullData = FloorDataMapper.fromDtoList(floorConfigResponse.data)
            cachedFullData = fullData // 缓存完整数据
            
            // 返回骨架屏配置：清空所有楼层的businessData
            return fullData.map { floorData ->
                floorData.copy(businessData = emptyMap())
            }
        }
        
        return emptyList()
    }

    /**
     * 启动后台数据加载任务
     */
    private fun startBackgroundDataLoading() {
        dataSourceScope.launch {
            try {
                // 模拟网络延迟
                delay(networkDelayMs)
                
                // 5秒后返回完整数据
                val realData = cachedFullData ?: emptyList()
                
                // 通知数据更新
                onDataUpdateCallback?.invoke(realData)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 设置数据更新回调
     */
    fun setOnDataUpdateCallback(callback: (List<FloorData>) -> Unit) {
        onDataUpdateCallback = callback
    }

    /**
     * 重置加载状态（用于刷新）
     */
    fun resetLoadingState() {
        cachedFullData = null
        onDataUpdateCallback = null
    }

    /**
     * 模拟加载楼层业务数据
     */
    override suspend fun loadFloorData(
        floorId: String,
        floorType: String,
        params: Map<String, Any>
    ): Any? {
        return try {
            // 这个方法不再使用，因为数据已经在loadFloorConfig中返回了
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 模拟更新楼层配置
     */
    override suspend fun updateFloorConfig(
        pageId: String,
        floorConfig: List<FloorData>
    ): Boolean {
        return try {
            delay(networkDelayMs)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从assets文件夹中读取JSON文件
     */
    private fun readJsonFromAssets(fileName: String): String {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

} 