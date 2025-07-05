package com.github.flexfloor.network

import com.google.gson.annotations.SerializedName

/**
 * 楼层配置响应数据模型
 */
data class FloorConfigResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: List<FloorDataDto>?
)

/**
 * 楼层数据传输对象
 */
data class FloorDataDto(
    @SerializedName("floorId")
    val floorId: String,
    
    @SerializedName("floorType")
    val floorType: String,
    
    @SerializedName("floorConfig")
    val floorConfig: FloorConfigDto,
    
    @SerializedName("businessData")
    val businessData: Map<String, Any> = emptyMap(),
    
    @SerializedName("priority")
    val priority: Int = 0,
    
    @SerializedName("isVisible")
    val isVisible: Boolean = true,
    
    @SerializedName("isSticky")
    val isSticky: Boolean = false,
    
    @SerializedName("loadPolicy")
    val loadPolicy: String = "LAZY",
    
    @SerializedName("cachePolicy")
    val cachePolicy: String = "MEMORY",
    
    @SerializedName("exposureConfig")
    val exposureConfig: Any? = null
)

/**
 * 楼层配置传输对象
 */
data class FloorConfigDto(
    @SerializedName("margin")
    val margin: EdgeInsetsDto,
    
    @SerializedName("padding")
    val padding: EdgeInsetsDto,
    
    @SerializedName("cornerRadius")
    val cornerRadius: Float = 0f,
    
    @SerializedName("backgroundColor")
    val backgroundColor: String? = null,
    
    @SerializedName("backgroundImage")
    val backgroundImage: String? = null,
    
    @SerializedName("elevation")
    val elevation: Float = 0f,
    
    @SerializedName("clickable")
    val clickable: Boolean = false,
    
    @SerializedName("jumpAction")
    val jumpAction: Any? = null,
    
    @SerializedName("customStyle")
    val customStyle: Map<String, Any> = emptyMap()
)

/**
 * 边距传输对象
 */
data class EdgeInsetsDto(
    @SerializedName("left")
    val left: Int = 0,
    
    @SerializedName("top")
    val top: Int = 0,
    
    @SerializedName("right")
    val right: Int = 0,
    
    @SerializedName("bottom")
    val bottom: Int = 0
) 