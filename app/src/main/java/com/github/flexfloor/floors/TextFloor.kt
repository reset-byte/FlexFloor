package com.github.flexfloor.floors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.base.BaseFloor
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 文本楼层实现
 * 用于显示文本内容的楼层
 */
class TextFloor : BaseFloor() {
    
    override fun getFloorType(): String = FloorType.TEXT.typeName
    
    override fun onCreateView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(
            com.github.flexfloor.R.layout.floor_text,
            parent,
            false
        )
    }
    
    override fun onBindData(holder: RecyclerView.ViewHolder, floorData: FloorData, position: Int) {
        val textData: TextFloorData = parseTextData(floorData.data)
        
        val titleView: TextView = holder.itemView.findViewById(com.github.flexfloor.R.id.floor_title_text)
        val contentView: TextView = holder.itemView.findViewById(com.github.flexfloor.R.id.floor_content_text)
        
        titleView.text = textData.title ?: floorData.title ?: ""
        contentView.text = textData.content ?: ""
        
        // 设置文本样式
        textData.titleColor?.let { color ->
            try {
                titleView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }
        
        textData.contentColor?.let { color ->
            try {
                contentView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }
        
        textData.titleSize?.let { size ->
            titleView.textSize = size
        }
        
        textData.contentSize?.let { size ->
            contentView.textSize = size
        }
        
        // 设置可见性
        titleView.visibility = if (textData.title.isNullOrEmpty()) View.GONE else View.VISIBLE
        contentView.visibility = if (textData.content.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
    
    override fun isSupportLazyLoad(): Boolean = false
    
    override fun getPriority(): Int = 1
    
    /**
     * 解析文本数据
     */
    private fun parseTextData(data: Any?): TextFloorData {
        return try {
            when (data) {
                is TextFloorData -> data
                is String -> Gson().fromJson(data, TextFloorData::class.java)
                is Map<*, *> -> {
                    val gson = Gson()
                    val json = gson.toJson(data)
                    gson.fromJson(json, TextFloorData::class.java)
                }
                else -> TextFloorData()
            }
        } catch (e: Exception) {
            TextFloorData()
        }
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