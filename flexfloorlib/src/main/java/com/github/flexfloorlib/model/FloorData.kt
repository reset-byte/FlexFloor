package com.github.flexfloorlib.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * 楼层数据模型，表示页面中的单个楼层
 */
@Parcelize
data class FloorData(
    val floorId: String,
    val floorType: FloorType,
    val floorConfig: FloorConfig,
    val businessData: @RawValue Map<String, Any> = emptyMap(),
    val priority: Int = 0,
    val isVisible: Boolean = true,
    val isSticky: Boolean = false,
    val loadPolicy: LoadPolicy = LoadPolicy.LAZY,
    val cachePolicy: CachePolicy = CachePolicy.MEMORY,
    val exposureConfig: ExposureConfig? = null
) : Parcelable

/**
 * 楼层类型枚举
 */
enum class FloorType(val typeName: String) {
    BANNER("banner"),
    GRID("grid"),
    LIST_HORIZONTAL("list_horizontal"),
    LIST_VERTICAL("list_vertical"),
    CARD("card"),
    TEXT("text"),
    IMAGE("image"),
    VIDEO("video"),
    WEBVIEW("webview"),
    CUSTOM("custom")
}

/**
 * 楼层配置，用于样式和行为设置
 */
@Parcelize
data class FloorConfig(
    val margin: EdgeInsets = EdgeInsets(),
    val padding: EdgeInsets = EdgeInsets(),
    val cornerRadius: Float = 0f,
    val backgroundColor: String? = null,
    val backgroundImage: String? = null,
    val elevation: Float = 0f,
    val clickable: Boolean = false,
    val jumpAction: JumpAction? = null,
    val customStyle: @RawValue Map<String, Any> = emptyMap()
) : Parcelable

/**
 * 边距配置，用于外边距和内边距
 */
@Parcelize
data class EdgeInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) : Parcelable

/**
 * 跳转动作配置
 */
@Parcelize
data class JumpAction(
    val actionType: ActionType,
    val url: String? = null,
    val params: Map<String, String> = emptyMap()
) : Parcelable

enum class ActionType {
    NONE, WEB, NATIVE, SHARE, PHONE
}

/**
 * 楼层数据加载策略
 */
enum class LoadPolicy {
    EAGER,      // 立即加载
    LAZY,       // 懒加载，可见时加载
    PRELOAD     // 预加载，即将可见时加载
}

/**
 * 楼层数据缓存策略
 */
enum class CachePolicy {
    NONE,       // 不缓存
    MEMORY,     // 仅内存缓存
    DISK,       // 仅磁盘缓存
    BOTH        // 内存和磁盘双重缓存
}

/**
 * 曝光追踪配置
 */
@Parcelize
data class ExposureConfig(
    val trackOnShow: Boolean = true,
    val trackOnClick: Boolean = true,
    val minVisibleRatio: Float = 0.5f,
    val minVisibleDuration: Long = 500L,
    val eventParams: Map<String, String> = emptyMap()
) : Parcelable 