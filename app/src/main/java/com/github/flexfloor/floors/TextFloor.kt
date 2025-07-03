package com.github.flexfloor.floors

import android.view.View
import android.widget.TextView
import com.github.flexfloorlib.core.BaseFloor
import com.github.flexfloorlib.model.FloorType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 重构后的文本楼层实现
 * 使用新的架构：职责分离，统一数据处理
 */
class TextFloor : BaseFloor<TextFloorData>() {
    
    override fun getFloorType(): String = FloorType.TEXT.typeName
    
    override fun getLayoutResId(): Int = com.github.flexfloor.R.layout.floor_text
    
    /**
     * 解析业务数据 - 统一数据解析入口
     */
    override fun parseBusinessData(configData: Map<String, Any>): TextFloorData? {
        return try {
            val gson = Gson()
            val json = gson.toJson(configData)
            gson.fromJson(json, TextFloorData::class.java)
        } catch (e: Exception) {
            // 解析失败时使用默认数据
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
     * 渲染视图 - 纯视图渲染逻辑
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
        
        // 设置点击事件
        view.setOnClickListener { onFloorClick(it) }
    }
    
    /**
     * 异步加载数据 - 可选的远程数据加载
     */
    override suspend fun loadData(): TextFloorData? {
        // 这里可以实现远程数据加载逻辑
        // 例如：从API获取更详细的文本内容
        return null // 当前示例中不需要异步加载
    }
    
    /**
     * 自定义加载状态显示
     */
    override fun showLoadingState(view: View) {
        val titleView: TextView = view.findViewById(com.github.flexfloor.R.id.floor_title_text)
        val contentView: TextView = view.findViewById(com.github.flexfloor.R.id.floor_content_text)
        
        titleView.text = "加载中..."
        contentView.text = "正在获取内容..."
        titleView.visibility = View.VISIBLE
        contentView.visibility = View.VISIBLE
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