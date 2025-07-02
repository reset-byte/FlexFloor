package com.github.flexfloorlib.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.facebook.shimmer.ShimmerFrameLayout

/**
 * 楼层骨架屏视图
 * 用于在楼层数据加载时显示占位效果
 */
class FloorSkeletonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ShimmerFrameLayout(context, attrs, defStyleAttr) {
    
    companion object {
        const val DEFAULT_SKELETON_COLOR = 0xFFE0E0E0.toInt()
        const val DEFAULT_SHIMMER_COLOR = 0xFFF0F0F0.toInt()
        const val DEFAULT_CORNER_RADIUS = 8f
    }
    
    private val skeletonItems: MutableList<SkeletonItem> = mutableListOf()
    
    init {
        setupShimmerEffect()
    }
    
    /**
     * 设置Shimmer效果
     */
    private fun setupShimmerEffect() {
        // 配置Shimmer效果
        val shimmer = com.facebook.shimmer.Shimmer.AlphaHighlightBuilder()
            .setDuration(1500)
            .setBaseAlpha(0.7f)
            .setHighlightAlpha(0.6f)
            .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()
        
        setShimmer(shimmer)
    }
    
    /**
     * 添加骨架屏项目
     */
    fun addSkeletonItem(item: SkeletonItem) {
        skeletonItems.add(item)
        addSkeletonView(item)
    }
    
    /**
     * 批量添加骨架屏项目
     */
    fun addSkeletonItems(items: List<SkeletonItem>) {
        items.forEach { addSkeletonItem(it) }
    }
    
    /**
     * 清除所有骨架屏项目
     */
    fun clearSkeletonItems() {
        skeletonItems.clear()
        removeAllViews()
    }
    
    /**
     * 显示骨架屏
     */
    fun showSkeleton() {
        visibility = View.VISIBLE
        startShimmer()
    }
    
    /**
     * 隐藏骨架屏
     */
    fun hideSkeleton() {
        stopShimmer()
        visibility = View.GONE
    }
    
    /**
     * 添加骨架屏视图
     */
    private fun addSkeletonView(item: SkeletonItem) {
        val skeletonView = SkeletonItemView(context, item)
        addView(skeletonView, item.layoutParams)
    }
}

/**
 * 骨架屏项目数据类
 */
data class SkeletonItem(
    val width: Int,
    val height: Int,
    val marginTop: Int = 0,
    val marginBottom: Int = 0,
    val marginLeft: Int = 0,
    val marginRight: Int = 0,
    val cornerRadius: Float = FloorSkeletonView.DEFAULT_CORNER_RADIUS,
    val backgroundColor: Int = FloorSkeletonView.DEFAULT_SKELETON_COLOR,
    val layoutParams: ViewGroup.LayoutParams = ViewGroup.LayoutParams(width, height)
)

/**
 * 骨架屏项目视图
 */
private class SkeletonItemView(
    context: Context,
    private val item: SkeletonItem
) : View(context) {
    
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = item.backgroundColor
        style = Paint.Style.FILL
    }
    
    private val rectF: RectF = RectF()
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF.set(0f, 0f, w.toFloat(), h.toFloat())
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(rectF, item.cornerRadius, item.cornerRadius, paint)
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(item.width, item.height)
    }
}

/**
 * 楼层骨架屏构建器
 * 提供便捷的骨架屏构建方法
 */
class FloorSkeletonBuilder(private val context: Context) {
    
    private val skeletonItems: MutableList<SkeletonItem> = mutableListOf()
    private var containerLayoutParams: ViewGroup.LayoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    
    /**
     * 设置容器布局参数
     */
    fun setContainerLayoutParams(layoutParams: ViewGroup.LayoutParams): FloorSkeletonBuilder {
        this.containerLayoutParams = layoutParams
        return this
    }
    
    /**
     * 添加矩形骨架屏项目
     */
    fun addRectangle(
        width: Int,
        height: Int,
        marginTop: Int = 0,
        marginBottom: Int = 0,
        marginLeft: Int = 0,
        marginRight: Int = 0,
        cornerRadius: Float = FloorSkeletonView.DEFAULT_CORNER_RADIUS
    ): FloorSkeletonBuilder {
        val layoutParams = LinearLayout.LayoutParams(width, height).apply {
            setMargins(marginLeft, marginTop, marginRight, marginBottom)
        }
        
        val item = SkeletonItem(
            width = width,
            height = height,
            marginTop = marginTop,
            marginBottom = marginBottom,
            marginLeft = marginLeft,
            marginRight = marginRight,
            cornerRadius = cornerRadius,
            layoutParams = layoutParams
        )
        
        skeletonItems.add(item)
        return this
    }
    
    /**
     * 添加圆形骨架屏项目
     */
    fun addCircle(
        size: Int,
        marginTop: Int = 0,
        marginBottom: Int = 0,
        marginLeft: Int = 0,
        marginRight: Int = 0
    ): FloorSkeletonBuilder {
        return addRectangle(
            width = size,
            height = size,
            marginTop = marginTop,
            marginBottom = marginBottom,
            marginLeft = marginLeft,
            marginRight = marginRight,
            cornerRadius = size / 2f
        )
    }
    
    /**
     * 添加文本行骨架屏项目
     */
    fun addTextLine(
        width: Int,
        height: Int = dpToPx(16),
        marginTop: Int = dpToPx(8),
        marginBottom: Int = dpToPx(8),
        marginLeft: Int = 0,
        marginRight: Int = 0
    ): FloorSkeletonBuilder {
        return addRectangle(
            width = width,
            height = height,
            marginTop = marginTop,
            marginBottom = marginBottom,
            marginLeft = marginLeft,
            marginRight = marginRight,
            cornerRadius = height / 2f
        )
    }
    
    /**
     * 构建骨架屏视图
     */
    fun build(): FloorSkeletonView {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = containerLayoutParams
        }
        
        val skeletonView = FloorSkeletonView(context).apply {
            addView(container)
            addSkeletonItems(skeletonItems)
        }
        
        return skeletonView
    }
    
    /**
     * dp转px
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    companion object {
        /**
         * 创建构建器实例
         */
        fun create(context: Context): FloorSkeletonBuilder {
            return FloorSkeletonBuilder(context)
        }
        
        /**
         * 创建文本楼层骨架屏
         */
        fun createTextFloorSkeleton(context: Context): FloorSkeletonView {
            return create(context)
                .addTextLine(
                    width = ViewGroup.LayoutParams.MATCH_PARENT,
                    height = 20.dpToPx(context),
                    marginTop = 16.dpToPx(context),
                    marginLeft = 16.dpToPx(context),
                    marginRight = 16.dpToPx(context)
                )
                .addTextLine(
                    width = (200).dpToPx(context),
                    height = 16.dpToPx(context),
                    marginTop = 8.dpToPx(context),
                    marginBottom = 16.dpToPx(context),
                    marginLeft = 16.dpToPx(context)
                )
                .build()
        }
        
        /**
         * 创建卡片楼层骨架屏
         */
        fun createCardFloorSkeleton(context: Context): FloorSkeletonView {
            return create(context)
                .addRectangle(
                    width = ViewGroup.LayoutParams.MATCH_PARENT,
                    height = 120.dpToPx(context),
                    marginTop = 16.dpToPx(context),
                    marginBottom = 16.dpToPx(context),
                    marginLeft = 16.dpToPx(context),
                    marginRight = 16.dpToPx(context),
                    cornerRadius = 8f
                )
                .build()
        }
        
        /**
         * 创建网格楼层骨架屏
         */
        fun createGridFloorSkeleton(context: Context, columns: Int = 2): FloorSkeletonView {
            val builder = create(context)
            val itemWidth = (context.resources.displayMetrics.widthPixels - 48.dpToPx(context)) / columns
            
            repeat(columns) {
                builder.addRectangle(
                    width = itemWidth,
                    height = 80.dpToPx(context),
                    marginTop = 8.dpToPx(context),
                    marginBottom = 8.dpToPx(context),
                    marginLeft = if (it == 0) 16.dpToPx(context) else 8.dpToPx(context),
                    marginRight = if (it == columns - 1) 16.dpToPx(context) else 8.dpToPx(context)
                )
            }
            
            return builder.build()
        }
        
        private fun Int.dpToPx(context: Context): Int {
            return (this * context.resources.displayMetrics.density).toInt()
        }
    }
} 