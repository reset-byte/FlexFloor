package com.github.flexfloor.network

import com.github.flexfloorlib.model.FloorData
import retrofit2.Response
import retrofit2.http.*

/**
 * 楼层网络API服务接口
 */
interface FloorApiService {
    
    /**
     * 获取楼层配置
     * @param pageId 页面ID
     * @return 楼层配置列表
     */
    @GET("api/floor/config")
    suspend fun getFloorConfig(@Query("page_id") pageId: String): Response<List<FloorData>>
    
    /**
     * 获取楼层业务数据
     * @param floorId 楼层ID
     * @param params 请求参数
     * @return 业务数据
     */
    @POST("api/floor/data")
    suspend fun getFloorData(
        @Query("floor_id") floorId: String,
        @Body params: Map<String, Any>
    ): Response<Any>
    
    /**
     * 批量获取楼层配置
     * @param pageIds 页面ID列表
     * @return 楼层配置映射表
     */
    @POST("api/floor/batch-config")
    suspend fun getBatchFloorConfig(@Body pageIds: List<String>): Response<Map<String, List<FloorData>>>
    
    /**
     * 上报楼层曝光
     * @param exposureData 曝光数据
     */
    @POST("api/floor/exposure")
    suspend fun reportFloorExposure(@Body exposureData: FloorExposureData): Response<Unit>
    
    /**
     * 上报楼层点击
     * @param clickData 点击数据
     */
    @POST("api/floor/click")
    suspend fun reportFloorClick(@Body clickData: FloorClickData): Response<Unit>
}

/**
 * 楼层曝光数据
 */
data class FloorExposureData(
    val floorId: String,
    val pageId: String,
    val position: Int,
    val exposureRatio: Float,
    val exposureTime: Long,
    val userId: String?,
    val sessionId: String
)

/**
 * 楼层点击数据
 */
data class FloorClickData(
    val floorId: String,
    val pageId: String,
    val position: Int,
    val actionType: String,
    val actionUrl: String?,
    val clickTime: Long,
    val userId: String?,
    val sessionId: String
) 