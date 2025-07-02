package com.github.flexfloor.repository

import com.github.flexfloor.network.FloorApiService
import com.github.flexfloorlib.cache.FloorCacheManager
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 楼层数据仓库
 * 负责管理楼层数据的获取、缓存和更新
 */
class FloorRepository(
    private val apiService: FloorApiService,
    private val cacheManager: FloorCacheManager
) {
    
    /**
     * 加载楼层配置
     * @param pageId 页面ID
     * @return 楼层数据流
     */
    suspend fun loadFloorConfig(pageId: String): Flow<List<FloorData>> = flow {
        try {
            // 首先发送示例数据
            val sampleFloors = createSampleFloors()
            emit(sampleFloors)
            
            // 可以在这里调用API获取真实数据
            // val response = apiService.getFloorConfig(pageId)
            // if (response.isSuccessful) {
            //     response.body()?.let { floors ->
            //         emit(floors)
            //     }
            // }
        } catch (e: Exception) {
            // 发送空列表以避免崩溃
            emit(emptyList())
        }
    }
    
    /**
     * 刷新楼层数据
     * @param pageId 页面ID
     * @return 楼层数据流
     */
    suspend fun refreshFloors(pageId: String): Flow<List<FloorData>> = flow {
        try {
            // 刷新时返回示例数据
            val sampleFloors = createSampleFloors()
            emit(sampleFloors)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    /**
     * 创建示例楼层数据
     */
    private fun createSampleFloors(): List<FloorData> {
        return listOf(
            FloorData(
                floorId = "welcome_text_floor",
                floorType = FloorType.TEXT,
                title = "欢迎使用FlexFloor",
                data = mapOf(
                    "title" to "FlexFloor 楼层架构演示",
                    "content" to "这是一个动态楼层架构的演示应用，支持多种楼层类型的动态配置和管理。",
                    "title_color" to "#2C3E50",
                    "content_color" to "#7F8C8D"
                ),
                style = null,
                action = null,
                priority = 1,
                isSticky = false,
                isVisible = true,
                isLazyLoad = false
            ),
            FloorData(
                floorId = "feature_text_floor",
                floorType = FloorType.TEXT,
                title = "主要特性",
                data = mapOf(
                    "title" to "核心功能",
                    "content" to "• 动态楼层配置\n• 多楼层类型支持\n• 智能缓存机制\n• 流畅的用户体验",
                    "title_color" to "#27AE60",
                    "content_color" to "#2C3E50"
                ),
                style = null,
                action = null,
                priority = 2,
                isSticky = false,
                isVisible = true,
                isLazyLoad = false
            )
        )
    }
} 