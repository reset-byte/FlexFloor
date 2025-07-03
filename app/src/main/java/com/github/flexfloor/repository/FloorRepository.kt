package com.github.flexfloor.repository

import com.github.flexfloor.network.FloorApiService
import com.github.flexfloorlib.cache.FloorCacheManager
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorType
import com.github.flexfloorlib.model.FloorConfig
import com.github.flexfloorlib.model.EdgeInsets
import com.github.flexfloorlib.model.LoadPolicy
import com.github.flexfloorlib.model.CachePolicy
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
                floorConfig = FloorConfig(
                    margin = EdgeInsets(left = 16, top = 16, right = 16, bottom = 8),
                    padding = EdgeInsets(left = 16, top = 16, right = 16, bottom = 16),
                    cornerRadius = 16f,
                    backgroundColor = "#FFFFFF",
                    elevation = 2f,
                    clickable = true
                ),
                businessData = mapOf(
                    "title" to "FlexFloor 楼层架构演示",
                    "content" to "这是一个动态楼层架构的演示应用，支持多种楼层类型的动态配置和管理。",
                    "title_color" to "#2C3E50",
                    "content_color" to "#7F8C8D"
                ),
                priority = 1,
                isVisible = true,
                loadPolicy = LoadPolicy.EAGER,
                cachePolicy = CachePolicy.MEMORY
            ),
            FloorData(
                floorId = "feature_text_floor",
                floorType = FloorType.TEXT,
                floorConfig = FloorConfig(
                    margin = EdgeInsets(left = 16, top = 8, right = 16, bottom = 8),
                    padding = EdgeInsets(left = 16, top = 16, right = 16, bottom = 16),
                    cornerRadius = 16f,
                    backgroundColor = "#FFFFFF",
                    elevation = 1f,
                    clickable = true
                ),
                businessData = mapOf(
                    "title" to "核心功能",
                    "content" to "• 动态楼层配置\n• 多楼层类型支持\n• 智能缓存机制\n• 流畅的用户体验",
                    "title_color" to "#27AE60",
                    "content_color" to "#2C3E50"
                ),
                priority = 2,
                isVisible = true,
                loadPolicy = LoadPolicy.LAZY,
                cachePolicy = CachePolicy.MEMORY
            ),
            FloorData(
                floorId = "demo_image_floor",
                floorType = FloorType.IMAGE,
                floorConfig = FloorConfig(
                    margin = EdgeInsets(left = 16, top = 8, right = 16, bottom = 8),
                    padding = EdgeInsets(left = 0, top = 0, right = 0, bottom = 0),
                    cornerRadius = 16f,
                    backgroundColor = "#FFFFFF",
                    elevation = 3f,
                    clickable = true
                ),
                businessData = mapOf(
                    "title" to "示例图片",
                    "description" to "这是一个图片楼层的演示",
                    "image_url" to "https://via.placeholder.com/300x200",
                    "scale_type" to "center_crop"
                ),
                priority = 3,
                isVisible = true,
                loadPolicy = LoadPolicy.LAZY,
                cachePolicy = CachePolicy.BOTH
            ),
            FloorData(
                floorId = "demo_banner_floor",
                floorType = FloorType.BANNER,
                floorConfig = FloorConfig(
                    margin = EdgeInsets(left = 16, top = 8, right = 16, bottom = 16),
                    padding = EdgeInsets(left = 0, top = 0, right = 0, bottom = 0),
                    cornerRadius = 16f,
                    backgroundColor = "#FFFFFF",
                    elevation = 4f,
                    clickable = true
                ),
                businessData = mapOf(
                    "title" to "精彩轮播",
                    "title_color" to "#2C3E50",
                    "title_size" to 18f,
                    "auto_play" to true,
                    "play_interval" to 3000L,
                    "show_indicators" to true,
                    "infinite_loop" to true,
                    "corner_radius" to 12f,
                    "pages" to listOf(
                        mapOf(
                            "title" to "特色服务",
                            "description" to "为您提供最优质的特色服务体验",
                            "background_color" to "#E91E63",
                            "link_url" to "https://example.com/service"
                        ),
                        mapOf(
                            "title" to "创新产品",
                            "description" to "体验我们最新的创新产品功能",
                            "background_color" to "#00BCD4",
                            "link_url" to "https://example.com/product"
                        ),
                        mapOf(
                            "title" to "专业团队",
                            "description" to "专业的团队为您保驾护航",
                            "background_color" to "#4CAF50",
                            "link_url" to "https://example.com/team"
                        ),
                        mapOf(
                            "title" to "贴心支持",
                            "description" to "7x24小时全天候贴心技术支持",
                            "background_color" to "#FF5722",
                            "link_url" to "https://example.com/support"
                        ),
                        mapOf(
                            "title" to "优惠活动",
                            "description" to "限时优惠活动，机会难得！",
                            "background_color" to "#9C27B0",
                            "link_url" to "https://example.com/promotion"
                        )
                    )
                ),
                priority = 4,
                isVisible = true,
                loadPolicy = LoadPolicy.PRELOAD,
                cachePolicy = CachePolicy.BOTH
            )
        )
    }
} 