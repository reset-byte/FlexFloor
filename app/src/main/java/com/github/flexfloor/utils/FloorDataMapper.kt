package com.github.flexfloor.utils

import com.github.flexfloor.network.EdgeInsetsDto
import com.github.flexfloor.network.FloorConfigDto
import com.github.flexfloor.network.FloorDataDto
import com.github.flexfloorlib.model.CachePolicy
import com.github.flexfloorlib.model.EdgeInsets
import com.github.flexfloorlib.model.FloorConfig
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorType
import com.github.flexfloorlib.model.LoadPolicy

/**
 * 楼层数据映射器
 * 负责将网络传输的DTO对象转换为业务实体对象
 */
object FloorDataMapper {
    
    /**
     * 将FloorDataDto转换为FloorData
     */
    fun fromDto(dto: FloorDataDto): FloorData {
        return FloorData(
            floorId = dto.floorId,
            floorType = parseFloorType(dto.floorType),
            floorConfig = fromConfigDto(dto.floorConfig),
            businessData = dto.businessData,
            priority = dto.priority,
            isVisible = dto.isVisible,
            isSticky = dto.isSticky,
            loadPolicy = parseLoadPolicy(dto.loadPolicy),
            cachePolicy = parseCachePolicy(dto.cachePolicy),
            exposureConfig = null // TODO: 根据需要解析exposureConfig
        )
    }
    
    /**
     * 将FloorConfigDto转换为FloorConfig
     */
    private fun fromConfigDto(dto: FloorConfigDto): FloorConfig {
        return FloorConfig(
            margin = fromEdgeInsetsDto(dto.margin),
            padding = fromEdgeInsetsDto(dto.padding),
            cornerRadius = dto.cornerRadius,
            backgroundColor = dto.backgroundColor,
            backgroundImage = dto.backgroundImage,
            elevation = dto.elevation,
            clickable = dto.clickable,
            jumpAction = null, // TODO: 根据需要解析jumpAction
            customStyle = dto.customStyle
        )
    }
    
    /**
     * 将EdgeInsetsDto转换为EdgeInsets
     */
    private fun fromEdgeInsetsDto(dto: EdgeInsetsDto): EdgeInsets {
        return EdgeInsets(
            left = dto.left,
            top = dto.top,
            right = dto.right,
            bottom = dto.bottom
        )
    }
    
    /**
     * 解析楼层类型
     */
    private fun parseFloorType(typeString: String): FloorType {
        return when (typeString.lowercase()) {
            "banner" -> FloorType.BANNER
            "grid" -> FloorType.GRID
            "list_horizontal" -> FloorType.LIST_HORIZONTAL
            "list_vertical" -> FloorType.LIST_VERTICAL
            "card" -> FloorType.CARD
            "text" -> FloorType.TEXT
            "image" -> FloorType.IMAGE
            "video" -> FloorType.VIDEO
            "webview" -> FloorType.WEBVIEW
            "custom" -> FloorType.CUSTOM
            else -> FloorType.CUSTOM
        }
    }
    
    /**
     * 解析加载策略
     */
    private fun parseLoadPolicy(policyString: String): LoadPolicy {
        return when (policyString.uppercase()) {
            "EAGER" -> LoadPolicy.EAGER
            "LAZY" -> LoadPolicy.LAZY
            else -> LoadPolicy.PRELOAD
        }
    }
    
    /**
     * 解析缓存策略
     */
    private fun parseCachePolicy(policyString: String): CachePolicy {
        return when (policyString.uppercase()) {
            "NONE" -> CachePolicy.NONE
            "MEMORY" -> CachePolicy.MEMORY
            "DISK" -> CachePolicy.DISK
            "BOTH" -> CachePolicy.BOTH
            else -> CachePolicy.MEMORY
        }
    }
    
    /**
     * 批量转换FloorDataDto列表
     */
    fun fromDtoList(dtoList: List<FloorDataDto>): List<FloorData> {
        return dtoList.map { fromDto(it) }
    }
} 