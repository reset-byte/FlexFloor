package com.github.flexfloor.floors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.base.BaseFloor
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 横幅楼层实现
 * 用于显示横幅广告或轮播图的楼层
 */
class BannerFloor : BaseFloor() {
    
    override fun getFloorType(): String = FloorType.BANNER.typeName
    
    override fun onCreateView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(
            com.github.flexfloor.R.layout.floor_banner,
            parent,
            false
        )
    }
    
    override fun onBindData(holder: RecyclerView.ViewHolder, floorData: FloorData, position: Int) {
        val bannerData: BannerFloorData = parseBannerData(floorData.data)
        
        val containerView: LinearLayout = holder.itemView.findViewById(com.github.flexfloor.R.id.banner_container)
        val titleView: TextView = holder.itemView.findViewById(com.github.flexfloor.R.id.banner_title_text)
        val imageView: ImageView = holder.itemView.findViewById(com.github.flexfloor.R.id.banner_image_view)
        
        // 设置标题
        titleView.text = bannerData.title ?: floorData.title ?: ""
        titleView.visibility = if (bannerData.title.isNullOrEmpty()) View.GONE else View.VISIBLE
        
        // 设置标题样式
        bannerData.titleColor?.let { color ->
            try {
                titleView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }
        
        bannerData.titleSize?.let { size ->
            titleView.textSize = size
        }
        
        // 设置横幅图片
        loadBannerImage(imageView, bannerData.imageUrl)
        
        // 设置横幅高度
        bannerData.height?.let { height ->
            val layoutParams: ViewGroup.LayoutParams = imageView.layoutParams
            layoutParams.height = convertDpToPx(height, holder.itemView.context)
            imageView.layoutParams = layoutParams
        }
        
        // 设置图片缩放类型
        imageView.scaleType = when (bannerData.scaleType) {
            "center_crop" -> ImageView.ScaleType.CENTER_CROP
            "center_inside" -> ImageView.ScaleType.CENTER_INSIDE
            "fit_center" -> ImageView.ScaleType.FIT_CENTER
            "fit_xy" -> ImageView.ScaleType.FIT_XY
            "matrix" -> ImageView.ScaleType.MATRIX
            else -> ImageView.ScaleType.CENTER_CROP
        }
        
        // 设置容器背景
        bannerData.backgroundColor?.let { color ->
            try {
                containerView.setBackgroundColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }
        
        // 设置圆角
        if (bannerData.cornerRadius > 0) {
            // 设置圆角背景，实际项目中可以使用更复杂的drawable
            containerView.background = createRoundedBackground(
                bannerData.backgroundColor ?: "#FFFFFF",
                bannerData.cornerRadius
            )
        }
    }
    
    override fun isSupportLazyLoad(): Boolean = true
    
    override fun getPriority(): Int = 3
    

    
    /**
     * 加载横幅图片
     */
    private fun loadBannerImage(imageView: ImageView, imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_slideshow)
            return
        }
        
        // 简单实现，实际项目中建议使用Glide、Picasso等图片加载库
        try {
            // 这里可以集成Glide等图片加载库
            // Glide.with(imageView.context)
            //     .load(imageUrl)
            //     .placeholder(android.R.drawable.ic_menu_slideshow)
            //     .error(android.R.drawable.ic_menu_report_image)
            //     .centerCrop()
            //     .into(imageView)
            
            // 暂时使用默认图片
            imageView.setImageResource(android.R.drawable.ic_menu_slideshow)
        } catch (e: Exception) {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }
    
    /**
     * 创建圆角背景
     */
    private fun createRoundedBackground(color: String, cornerRadius: Float): android.graphics.drawable.Drawable {
        val drawable = android.graphics.drawable.GradientDrawable()
        try {
            drawable.setColor(android.graphics.Color.parseColor(color))
        } catch (e: IllegalArgumentException) {
            drawable.setColor(android.graphics.Color.WHITE)
        }
        drawable.cornerRadius = convertDpToPx(cornerRadius.toInt(), null).toFloat()
        return drawable
    }
    
    /**
     * dp转px
     */
    private fun convertDpToPx(dp: Int, context: android.content.Context?): Int {
        val displayMetrics = context?.resources?.displayMetrics 
            ?: android.content.res.Resources.getSystem().displayMetrics
        val density: Float = displayMetrics.density
        return (dp * density).toInt()
    }
    
    /**
     * 解析横幅数据
     */
    private fun parseBannerData(data: Any?): BannerFloorData {
        return try {
            when (data) {
                is BannerFloorData -> data
                is String -> Gson().fromJson(data, BannerFloorData::class.java)
                is Map<*, *> -> {
                    val gson = Gson()
                    val json = gson.toJson(data)
                    gson.fromJson(json, BannerFloorData::class.java)
                }
                else -> BannerFloorData()
            }
        } catch (e: Exception) {
            BannerFloorData()
        }
    }
}

/**
 * 横幅楼层数据模型
 */
data class BannerFloorData(
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("image_url")
    val imageUrl: String? = null,
    
    @SerializedName("width")
    val width: Int? = null,
    
    @SerializedName("height")
    val height: Int = 200,
    
    @SerializedName("scale_type")
    val scaleType: String = "center_crop",
    
    @SerializedName("background_color")
    val backgroundColor: String? = null,
    
    @SerializedName("corner_radius")
    val cornerRadius: Float = 0f,
    
    @SerializedName("title_color")
    val titleColor: String? = null,
    
    @SerializedName("title_size")
    val titleSize: Float? = null,
    
    @SerializedName("auto_play")
    val autoPlay: Boolean = false,
    
    @SerializedName("play_interval")
    val playInterval: Long = 3000L,
    
    @SerializedName("show_indicators")
    val showIndicators: Boolean = true,
    
    @SerializedName("infinite_loop")
    val infiniteLoop: Boolean = true,
    
    @SerializedName("images")
    val images: List<BannerImageData>? = null
)

/**
 * 横幅图片数据模型
 */
data class BannerImageData(
    @SerializedName("image_url")
    val imageUrl: String,
    
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("link_url")
    val linkUrl: String? = null,
    
    @SerializedName("link_type")
    val linkType: String? = null
) 