package com.github.flexfloorlib.model

import com.google.gson.annotations.SerializedName

/**
 * 楼层基础数据模型
 * @param floorId 楼层唯一标识
 * @param floorType 楼层类型
 * @param title 楼层标题
 * @param data 楼层业务数据
 * @param style 楼层样式配置
 * @param action 楼层交互配置
 * @param priority 楼层优先级，用于排序
 * @param isSticky 是否为吸顶楼层
 * @param isVisible 是否可见
 * @param isLazyLoad 是否延迟加载
 */
data class FloorData(
    @SerializedName("floor_id")
    val floorId: String,
    
    @SerializedName("floor_type")
    val floorType: FloorType,
    
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("data")
    val data: Any?,
    
    @SerializedName("style")
    val style: FloorStyle?,
    
    @SerializedName("action")
    val action: FloorAction?,
    
    @SerializedName("priority")
    val priority: Int = 0,
    
    @SerializedName("is_sticky")
    val isSticky: Boolean = false,
    
    @SerializedName("is_visible")
    val isVisible: Boolean = true,
    
    @SerializedName("is_lazy_load")
    val isLazyLoad: Boolean = false
)

/**
 * 楼层类型枚举
 */
enum class FloorType(val typeName: String) {
    @SerializedName("banner")
    BANNER("banner"),
    
    @SerializedName("grid")
    GRID("grid"),
    
    @SerializedName("list")
    LIST("list"),
    
    @SerializedName("card")
    CARD("card"),
    
    @SerializedName("text")
    TEXT("text"),
    
    @SerializedName("image")
    IMAGE("image"),
    
    @SerializedName("video")
    VIDEO("video"),
    
    @SerializedName("custom")
    CUSTOM("custom")
}

/**
 * 楼层样式配置
 * @param backgroundColor 背景色
 * @param cornerRadius 圆角大小
 * @param marginTop 上边距
 * @param marginBottom 下边距
 * @param marginLeft 左边距
 * @param marginRight 右边距
 * @param paddingTop 内上边距
 * @param paddingBottom 内下边距
 * @param paddingLeft 内左边距
 * @param paddingRight 内右边距
 * @param elevation 阴影高度
 */
data class FloorStyle(
    @SerializedName("background_color")
    val backgroundColor: String? = null,
    
    @SerializedName("corner_radius")
    val cornerRadius: Float = 0f,
    
    @SerializedName("margin_top")
    val marginTop: Int = 0,
    
    @SerializedName("margin_bottom")
    val marginBottom: Int = 0,
    
    @SerializedName("margin_left")
    val marginLeft: Int = 0,
    
    @SerializedName("margin_right")
    val marginRight: Int = 0,
    
    @SerializedName("padding_top")
    val paddingTop: Int = 0,
    
    @SerializedName("padding_bottom")
    val paddingBottom: Int = 0,
    
    @SerializedName("padding_left")
    val paddingLeft: Int = 0,
    
    @SerializedName("padding_right")
    val paddingRight: Int = 0,
    
    @SerializedName("elevation")
    val elevation: Float = 0f
)

/**
 * 楼层交互配置
 * @param actionType 交互类型
 * @param actionUrl 跳转链接
 * @param actionParams 交互参数
 */
data class FloorAction(
    @SerializedName("action_type")
    val actionType: ActionType,
    
    @SerializedName("action_url")
    val actionUrl: String?,
    
    @SerializedName("action_params")
    val actionParams: Map<String, Any>?
)

/**
 * 交互类型枚举
 */
enum class ActionType {
    @SerializedName("none")
    NONE,
    
    @SerializedName("url")
    URL,
    
    @SerializedName("native")
    NATIVE,
    
    @SerializedName("dialog")
    DIALOG,
    
    @SerializedName("share")
    SHARE
} 