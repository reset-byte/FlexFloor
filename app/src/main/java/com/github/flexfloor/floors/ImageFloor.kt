package com.github.flexfloor.floors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.base.BaseFloor
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 图片楼层实现
 * 用于显示图片内容的楼层
 */
class ImageFloor : BaseFloor() {
    
    override fun getFloorType(): String = FloorType.IMAGE.typeName
    
    override fun onCreateView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(
            com.github.flexfloor.R.layout.floor_image,
            parent,
            false
        )
    }
    
    override fun onBindData(holder: RecyclerView.ViewHolder, floorData: FloorData, position: Int) {
        val imageData: ImageFloorData = parseImageData(floorData.data)
        
        val imageView: ImageView = holder.itemView.findViewById(com.github.flexfloor.R.id.floor_image_view)
        val titleView: TextView = holder.itemView.findViewById(com.github.flexfloor.R.id.floor_title_text)
        val descView: TextView = holder.itemView.findViewById(com.github.flexfloor.R.id.floor_description_text)
        
        // 设置标题和描述
        titleView.text = imageData.title ?: floorData.title ?: ""
        descView.text = imageData.description ?: ""
        
        // 设置图片
        loadImage(imageView, imageData.imageUrl)
        
        // 设置图片尺寸
        imageData.imageWidth?.let { width ->
            imageData.imageHeight?.let { height ->
                val layoutParams: ViewGroup.LayoutParams = imageView.layoutParams
                layoutParams.width = convertDpToPx(width, holder.itemView.context)
                layoutParams.height = convertDpToPx(height, holder.itemView.context)
                imageView.layoutParams = layoutParams
            }
        }
        
        // 设置图片缩放类型
        imageView.scaleType = when (imageData.scaleType) {
            "center_crop" -> ImageView.ScaleType.CENTER_CROP
            "center_inside" -> ImageView.ScaleType.CENTER_INSIDE
            "fit_center" -> ImageView.ScaleType.FIT_CENTER
            "fit_xy" -> ImageView.ScaleType.FIT_XY
            else -> ImageView.ScaleType.CENTER_CROP
        }
        
        // 设置文本样式
        imageData.titleColor?.let { color ->
            try {
                titleView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }
        
        imageData.descriptionColor?.let { color ->
            try {
                descView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }
        
        // 设置可见性
        titleView.visibility = if (imageData.title.isNullOrEmpty()) View.GONE else View.VISIBLE
        descView.visibility = if (imageData.description.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
    
    override fun isSupportLazyLoad(): Boolean = true
    
    override fun getPriority(): Int = 2
    

    
    /**
     * 加载图片
     */
    private fun loadImage(imageView: ImageView, imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }
        
        // 简单实现，实际项目中建议使用Glide、Picasso等图片加载库
        try {
            // 这里可以集成Glide等图片加载库
            // Glide.with(imageView.context)
            //     .load(imageUrl)
            //     .placeholder(android.R.drawable.ic_menu_gallery)
            //     .error(android.R.drawable.ic_menu_report_image)
            //     .into(imageView)
            
            // 暂时使用默认图片
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        } catch (e: Exception) {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }
    
    /**
     * dp转px
     */
    private fun convertDpToPx(dp: Int, context: android.content.Context): Int {
        val density: Float = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
    
    /**
     * 解析图片数据
     */
    private fun parseImageData(data: Any?): ImageFloorData {
        return try {
            when (data) {
                is ImageFloorData -> data
                is String -> Gson().fromJson(data, ImageFloorData::class.java)
                is Map<*, *> -> {
                    val gson = Gson()
                    val json = gson.toJson(data)
                    gson.fromJson(json, ImageFloorData::class.java)
                }
                else -> ImageFloorData()
            }
        } catch (e: Exception) {
            ImageFloorData()
        }
    }
}

/**
 * 图片楼层数据模型
 */
data class ImageFloorData(
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("image_url")
    val imageUrl: String? = null,
    
    @SerializedName("image_width")
    val imageWidth: Int? = null,
    
    @SerializedName("image_height")
    val imageHeight: Int? = null,
    
    @SerializedName("scale_type")
    val scaleType: String = "center_crop",
    
    @SerializedName("title_color")
    val titleColor: String? = null,
    
    @SerializedName("description_color")
    val descriptionColor: String? = null,
    
    @SerializedName("placeholder_resource") 
    val placeholderResource: String? = null,
    
    @SerializedName("error_resource")
    val errorResource: String? = null
) 