package com.github.flexfloor.floors

import android.view.View
import android.widget.TextView
import com.github.flexfloor.R
import com.github.flexfloor.adapter.BannerPageAdapter
import com.github.flexfloorlib.core.BaseFloor
import com.github.flexfloorlib.model.FloorType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.zhpan.bannerview.BannerViewPager

/**
 * 横幅楼层实现
 */
class BannerFloor : BaseFloor<BannerFloorData>() {
    
    private var bannerAdapter: BannerPageAdapter? = null
    private var bannerViewPager: BannerViewPager<BannerPageData>? = null
    
    override fun getFloorType(): String = FloorType.BANNER.typeName
    
    override fun getLayoutResId(): Int = R.layout.floor_banner
    
    /**
     * 解析业务数据
     * 空数据时返回null以显示骨架屏
     */
    override fun parseBusinessData(configData: Map<String, Any>): BannerFloorData? {
        // 如果没有业务数据，返回null显示骨架屏
        if (configData.isEmpty()) {
            return null
        }
        
        return try {
            val gson = Gson()
            val json = gson.toJson(configData)
            gson.fromJson(json, BannerFloorData::class.java)
        } catch (e: Exception) {
            // 解析失败时手动解析Map数据
            val title = configData["title"] as? String
            val titleColor = configData["title_color"] as? String
            val titleSize = (configData["title_size"] as? Number)?.toFloat()
            val autoPlay = configData["auto_play"] as? Boolean ?: true
            val playInterval = (configData["play_interval"] as? Number)?.toLong() ?: 3000L
            val showIndicators = configData["show_indicators"] as? Boolean ?: true
            val infiniteLoop = configData["infinite_loop"] as? Boolean ?: true
            val cornerRadius = (configData["corner_radius"] as? Number)?.toFloat() ?: 8f
            
            // 解析页面数据
            val pages = (configData["pages"] as? List<*>)?.mapNotNull { pageData ->
                when (pageData) {
                    is Map<*, *> -> {
                        BannerPageData(
                            title = pageData["title"] as? String,
                            description = pageData["description"] as? String,
                            backgroundColor = pageData["background_color"] as? String ?: "#2196F3",
                            linkUrl = pageData["link_url"] as? String,
                            linkType = pageData["link_type"] as? String
                        )
                    }
                    else -> null
                }
            }
            
            BannerFloorData(
                title = title,
                titleColor = titleColor,
                titleSize = titleSize,
                autoPlay = autoPlay,
                playInterval = playInterval,
                showIndicators = showIndicators,
                infiniteLoop = infiniteLoop,
                cornerRadius = cornerRadius,
                pages = pages
            )
        }
    }
    
    /**
     * 渲染视图 - 纯视图渲染逻辑
     */
    override fun renderView(view: View, data: BannerFloorData, position: Int) {
        val titleView: TextView = view.findViewById(R.id.banner_title_text)
        bannerViewPager = view.findViewById(R.id.banner_view_pager)
        
        // 设置标题
        val title = data.title ?: "轮播推荐"
        titleView.text = title
        titleView.visibility = if (title.isNotEmpty()) View.VISIBLE else View.GONE
        
        // 设置标题样式
        data.titleColor?.let { color ->
            try {
                titleView.setTextColor(android.graphics.Color.parseColor(color))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
            }
        }
        
        data.titleSize?.let { size ->
            titleView.textSize = size
        }
        
        // 初始化轮播适配器
        bannerAdapter = BannerPageAdapter()
        
        // 配置轮播图
        bannerViewPager?.apply {
            // 设置适配器
            adapter = bannerAdapter
            
            // 设置轮播属性
            setAutoPlay(data.autoPlay)
            setCanLoop(data.infiniteLoop)
            setInterval(data.playInterval.toInt())
            
            // 设置指示器
            setIndicatorVisibility(if (data.showIndicators) View.VISIBLE else View.GONE)
            
            // 设置圆角
            if (data.cornerRadius > 0) {
                setRoundCorner(data.cornerRadius.toInt())
            }
            
            // 设置页面点击事件
            setOnPageClickListener { _, pagePosition ->
                onFloorClick(view)
            }
            
            // 设置轮播页面数据
            val pages = data.pages ?: createDefaultPages()
            create(pages)
            
            // 开始轮播
            if (data.autoPlay && pages.isNotEmpty()) {
                startLoop()
            }
        }
    }
    
    /**
     * 异步加载数据 - 可选的远程数据加载
     */
    override suspend fun loadData(): BannerFloorData? {
        return null // 当前示例中不需要异步加载
    }
    
    /**
     * 自定义加载状态显示（骨架屏）
     */
    override fun showLoadingState(view: View) {
        val titleView: TextView = view.findViewById(R.id.banner_title_text)
        val bannerView: BannerViewPager<*>? = view.findViewById(R.id.banner_view_pager)
        
        titleView.text = "轮播加载中..."
        titleView.visibility = View.VISIBLE
        
        // 隐藏轮播图或显示占位内容
        bannerView?.visibility = View.VISIBLE
        
        // 添加加载动画效果
        titleView.alpha = 0.5f
        val animation = android.animation.ObjectAnimator.ofFloat(titleView, "alpha", 0.5f, 1.0f)
        animation.duration = 500
        animation.repeatCount = android.animation.ObjectAnimator.INFINITE
        animation.repeatMode = android.animation.ObjectAnimator.REVERSE
        animation.start()
    }
    
    /**
     * 自定义错误状态显示
     */
    override fun showErrorState(view: View, error: String) {
        val titleView: TextView = view.findViewById(R.id.banner_title_text)
        titleView.text = "轮播加载失败: $error"
        titleView.visibility = View.VISIBLE
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 停止轮播，避免内存泄漏
        bannerViewPager?.stopLoop()
        bannerViewPager = null
        bannerAdapter = null
    }
    
    /**
     * 创建默认轮播页面
     */
    private fun createDefaultPages(): List<BannerPageData> {
        return listOf(
            BannerPageData(
                title = "精彩内容 1",
                description = "这是第一个轮播页面，展示精彩内容",
                backgroundColor = "#2196F3"
            ),
            BannerPageData(
                title = "优质服务 2", 
                description = "这是第二个轮播页面，提供优质服务",
                backgroundColor = "#4CAF50"
            ),
            BannerPageData(
                title = "创新体验 3",
                description = "这是第三个轮播页面，带来创新体验", 
                backgroundColor = "#FF9800"
            ),
            BannerPageData(
                title = "贴心关怀 4",
                description = "这是第四个轮播页面，给您贴心关怀",
                backgroundColor = "#9C27B0"
            )
        )
    }
}

/**
 * 横幅楼层数据模型
 */
data class BannerFloorData(
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("title_color")
    val titleColor: String? = null,
    
    @SerializedName("title_size")
    val titleSize: Float? = null,
    
    @SerializedName("auto_play")
    val autoPlay: Boolean = true,
    
    @SerializedName("play_interval")
    val playInterval: Long = 3000L,
    
    @SerializedName("show_indicators")
    val showIndicators: Boolean = true,
    
    @SerializedName("infinite_loop")
    val infiniteLoop: Boolean = true,
    
    @SerializedName("corner_radius")
    val cornerRadius: Float = 8f,
    
    @SerializedName("pages")
    val pages: List<BannerPageData>? = null
)

/**
 * 轮播页面数据模型
 */
data class BannerPageData(
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("background_color")
    val backgroundColor: String = "#2196F3",
    
    @SerializedName("link_url")
    val linkUrl: String? = null,
    
    @SerializedName("link_type")
    val linkType: String? = null
) 