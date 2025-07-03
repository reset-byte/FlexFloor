package com.github.flexfloor.floors

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.github.flexfloor.R
import com.github.flexfloorlib.core.BaseFloor
import com.github.flexfloorlib.model.FloorType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 重构后的图片楼层实现
 * 使用新的架构：职责分离，统一数据处理
 */
class ImageFloor : BaseFloor<ImageFloorData>() {

    override fun getFloorType(): String = FloorType.IMAGE.typeName

    override fun getLayoutResId(): Int = R.layout.floor_image

    /**
     * 解析业务数据 - 统一数据解析入口
     */
    override fun parseBusinessData(configData: Map<String, Any>): ImageFloorData? {
        return try {
            val gson = Gson()
            val json = gson.toJson(configData)
            gson.fromJson(json, ImageFloorData::class.java)
        } catch (e: Exception) {
            // 解析失败时使用默认数据
            ImageFloorData(
                title = configData["title"] as? String,
                description = configData["description"] as? String,
                imageUrl = configData["image_url"] as? String,
                imageWidth = (configData["image_width"] as? Number)?.toInt(),
                imageHeight = (configData["image_height"] as? Number)?.toInt(),
                scaleType = configData["scale_type"] as? String ?: "center_crop",
                titleColor = configData["title_color"] as? String,
                descriptionColor = configData["description_color"] as? String,
                backgroundColor = configData["background_color"] as? String
            )
        }
    }

    /**
     * 渲染视图 - 纯视图渲染逻辑
     */
    override fun renderView(view: View, data: ImageFloorData, position: Int) {
        val imageView: ImageView = view.findViewById(R.id.floor_image_view)
        val titleView: TextView = view.findViewById(R.id.floor_title_text)
        val descView: TextView = view.findViewById(R.id.floor_description_text)

        // 设置标题和描述
        titleView.text = data.title ?: ""
        descView.text = data.description ?: ""

        // 设置图片
        loadImage(imageView, data.imageUrl)

        // 设置图片尺寸
        data.imageWidth?.let { width ->
            data.imageHeight?.let { height ->
                val layoutParams: ViewGroup.LayoutParams = imageView.layoutParams
                layoutParams.width = convertDpToPx(width, view.context)
                layoutParams.height = convertDpToPx(height, view.context)
                imageView.layoutParams = layoutParams
            }
        }

        // 设置图片缩放类型
        imageView.scaleType = when (data.scaleType) {
            "center_crop" -> ImageView.ScaleType.CENTER_CROP
            "center_inside" -> ImageView.ScaleType.CENTER_INSIDE
            "fit_center" -> ImageView.ScaleType.FIT_CENTER
            "fit_xy" -> ImageView.ScaleType.FIT_XY
            else -> ImageView.ScaleType.CENTER_CROP
        }

        // 设置文本样式
        data.titleColor?.let { color ->
            try {
                titleView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }

        data.descriptionColor?.let { color ->
            try {
                descView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }

        // 设置可见性
        titleView.visibility = if (data.title.isNullOrEmpty()) View.GONE else View.VISIBLE
        descView.visibility = if (data.description.isNullOrEmpty()) View.GONE else View.VISIBLE

        // 设置点击事件
        view.setOnClickListener { onFloorClick(it) }
    }

    /**
     * 异步加载数据 - 可选的远程数据加载
     */
    override suspend fun loadData(): ImageFloorData? {
        // 这里可以实现远程数据加载逻辑
        // 例如：从API获取更详细的图片信息
        return null // 当前示例中不需要异步加载
    }

    /**
     * 自定义加载状态显示
     */
    override fun showLoadingState(view: View) {
        val titleView: TextView = view.findViewById(R.id.floor_title_text)
        val descView: TextView = view.findViewById(R.id.floor_description_text)
        val imageView: ImageView = view.findViewById(R.id.floor_image_view)
        
        titleView.text = "加载中..."
        descView.text = "正在获取图片..."
        titleView.visibility = View.VISIBLE
        descView.visibility = View.VISIBLE
        imageView.setImageResource(R.drawable.ic_image) // 显示占位图
    }

    /**
     * 自定义错误状态显示
     */
    override fun showErrorState(view: View, error: String) {
        val titleView: TextView = view.findViewById(R.id.floor_title_text)
        val descView: TextView = view.findViewById(R.id.floor_description_text)
        val imageView: ImageView = view.findViewById(R.id.floor_image_view)
        
        titleView.text = "加载失败"
        descView.text = error
        titleView.visibility = View.VISIBLE
        descView.visibility = View.VISIBLE
        imageView.setImageResource(R.drawable.ic_image) // 显示错误图
    }

    /**
     * 加载图片
     */
    private fun loadImage(imageView: ImageView, imageUrl: String?) {
        imageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        if (imageUrl.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_image)
            return
        }

        try {
            Glide.with(imageView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .into(imageView)
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.ic_image)
        }
    }

    /**
     * dp转px
     */
    private fun convertDpToPx(dp: Int, context: android.content.Context): Int {
        val density: Float = context.resources.displayMetrics.density
        return (dp * density).toInt()
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

    @SerializedName("background_color")
    val backgroundColor: String? = null,

    @SerializedName("placeholder_resource")
    val placeholderResource: String? = null,

    @SerializedName("error_resource")
    val errorResource: String? = null
) 