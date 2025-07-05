package com.github.flexfloor.floors

import android.view.View
import android.widget.TextView
import com.github.flexfloorlib.core.BaseFloor
import com.github.flexfloorlib.model.FloorType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 重构后的文本楼层实现
 * 配合两阶段加载：骨架屏 -> 真实数据
 */
class TextFloor : BaseFloor<TextFloorData>() {
    
    override fun getFloorType(): String = FloorType.TEXT.typeName
    
    override fun getLayoutResId(): Int = com.github.flexfloor.R.layout.floor_text
    
    /**
     * 解析业务数据
     * 空数据时返回null以显示骨架屏
     */
    override fun parseBusinessData(configData: Map<String, Any>): TextFloorData? {
        // 如果没有业务数据，返回null显示骨架屏
        if (configData.isEmpty()) {
            return null
        }
        
        return try {
            val gson = Gson()
            val json = gson.toJson(configData)
            gson.fromJson(json, TextFloorData::class.java)
        } catch (e: Exception) {
            // 解析失败，尝试手动构建
            TextFloorData(
                title = configData["title"] as? String,
                content = configData["content"] as? String,
                titleColor = configData["title_color"] as? String,
                contentColor = configData["content_color"] as? String,
                titleSize = (configData["title_size"] as? Number)?.toFloat(),
                contentSize = (configData["content_size"] as? Number)?.toFloat(),
                maxLines = (configData["max_lines"] as? Number)?.toInt()
            )
        }
    }
    
    /**
     * 渲染视图
     */
    override fun renderView(view: View, data: TextFloorData, position: Int) {
        val titleView: TextView = view.findViewById(com.github.flexfloor.R.id.floor_title_text)
        val contentView: TextView = view.findViewById(com.github.flexfloor.R.id.floor_content_text)
        
        // 设置文本内容
        titleView.text = data.title ?: ""
        contentView.text = data.content ?: ""
        
        // 设置文本样式
        data.titleColor?.let { color ->
            try {
                titleView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }
        
        data.contentColor?.let { color ->
            try {
                contentView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }
        
        data.titleSize?.let { size ->
            titleView.textSize = size
        }
        
        data.contentSize?.let { size ->
            contentView.textSize = size
        }
        
        // 设置最大行数
        data.maxLines?.let { maxLines ->
            contentView.maxLines = maxLines
        }
        
        // 设置可见性
        titleView.visibility = if (data.title.isNullOrEmpty()) View.GONE else View.VISIBLE
        contentView.visibility = if (data.content.isNullOrEmpty()) View.GONE else View.VISIBLE
        
        // 重置透明度（清除loading状态的动画效果）
        titleView.alpha = 1.0f
        contentView.alpha = 1.0f
        
        // 重置文本颜色（清除错误状态）
        if (data.titleColor == null) {
            titleView.setTextColor(android.graphics.Color.BLACK)
        }
        if (data.contentColor == null) {
            contentView.setTextColor(android.graphics.Color.GRAY)
        }
        
        // 设置点击事件
        view.setOnClickListener { onFloorClick(it) }
    }
    
    /**
     * 异步加载数据
     */
    override suspend fun loadData(): TextFloorData? {
        // 这里返回null，不会被调用
        return null
    }
    
    /**
     * 自定义加载状态显示（骨架屏）
     */
    override fun showLoadingState(view: View) {
        val titleView: TextView = view.findViewById(com.github.flexfloor.R.id.floor_title_text)
        val contentView: TextView = view.findViewById(com.github.flexfloor.R.id.floor_content_text)
        
        titleView.text = "加载中..."
        contentView.text = "正在获取内容..."
        titleView.visibility = View.VISIBLE
        contentView.visibility = View.VISIBLE
        
        // 添加加载动画效果
        titleView.alpha = 0.5f
        contentView.alpha = 0.5f
        
        // 创建简单的闪烁动画
        val animation = android.animation.ObjectAnimator.ofFloat(titleView, "alpha", 0.5f, 1.0f)
        animation.duration = 500
        animation.repeatCount = android.animation.ObjectAnimator.INFINITE
        animation.repeatMode = android.animation.ObjectAnimator.REVERSE
        animation.start()
        
        val contentAnimation = android.animation.ObjectAnimator.ofFloat(contentView, "alpha", 0.5f, 1.0f)
        contentAnimation.duration = 500
        contentAnimation.repeatCount = android.animation.ObjectAnimator.INFINITE
        contentAnimation.repeatMode = android.animation.ObjectAnimator.REVERSE
        contentAnimation.start()
    }
    
    /**
     * 自定义错误状态显示
     */
    override fun showErrorState(view: View, error: String) {
        val titleView: TextView = view.findViewById(com.github.flexfloor.R.id.floor_title_text)
        val contentView: TextView = view.findViewById(com.github.flexfloor.R.id.floor_content_text)
        
        titleView.text = "加载失败"
        contentView.text = error
        titleView.visibility = View.VISIBLE
        contentView.visibility = View.VISIBLE
        
        // 重置透明度
        titleView.alpha = 1.0f
        contentView.alpha = 1.0f
        
        // 设置错误状态的颜色
        titleView.setTextColor(android.graphics.Color.RED)
        contentView.setTextColor(android.graphics.Color.RED)
    }
}

/**
 * 文本楼层数据模型
 */
data class TextFloorData(
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("content")
    val content: String? = null,
    
    @SerializedName("title_color")
    val titleColor: String? = null,
    
    @SerializedName("content_color")
    val contentColor: String? = null,
    
    @SerializedName("title_size")
    val titleSize: Float? = null,
    
    @SerializedName("content_size")
    val contentSize: Float? = null,
    
    @SerializedName("max_lines")
    val maxLines: Int? = null,
    
    @SerializedName("is_html")
    val isHtml: Boolean = false
) 