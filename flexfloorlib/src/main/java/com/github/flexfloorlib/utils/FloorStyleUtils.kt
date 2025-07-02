package com.github.flexfloorlib.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.github.flexfloorlib.model.FloorStyle

/**
 * 楼层样式工具类
 * 提供样式应用的统一方法
 */
object FloorStyleUtils {
    
    /**
     * 应用楼层样式
     * @param view 目标视图
     * @param style 样式配置
     */
    fun applyStyle(view: View, style: FloorStyle?) {
        if (style == null) return
        
        // 应用背景色和圆角
        applyBackgroundStyle(view, style)
        
        // 应用外边距
        applyMarginStyle(view, style)
        
        // 应用内边距
        applyPaddingStyle(view, style)
        
        // 应用阴影
        applyElevationStyle(view, style)
    }
    
    /**
     * 应用楼层样式（从Map数据）
     * @param view 目标视图
     * @param styleMap 样式映射数据
     */
    fun applyStyle(view: View, styleMap: Map<String, Any>?) {
        if (styleMap == null) return
        
        val style = FloorStyle(
            backgroundColor = styleMap["background_color"] as? String,
            cornerRadius = (styleMap["corner_radius"] as? Number)?.toFloat() ?: 0f,
            marginTop = (styleMap["margin_top"] as? Number)?.toInt() ?: 0,
            marginBottom = (styleMap["margin_bottom"] as? Number)?.toInt() ?: 0,
            marginLeft = (styleMap["margin_left"] as? Number)?.toInt() ?: 0,
            marginRight = (styleMap["margin_right"] as? Number)?.toInt() ?: 0,
            paddingTop = (styleMap["padding_top"] as? Number)?.toInt() ?: 0,
            paddingBottom = (styleMap["padding_bottom"] as? Number)?.toInt() ?: 0,
            paddingLeft = (styleMap["padding_left"] as? Number)?.toInt() ?: 0,
            paddingRight = (styleMap["padding_right"] as? Number)?.toInt() ?: 0,
            elevation = (styleMap["elevation"] as? Number)?.toFloat() ?: 0f
        )
        
        applyStyle(view, style)
    }
    
    /**
     * 应用背景样式
     */
    private fun applyBackgroundStyle(view: View, style: FloorStyle) {
        val drawable = GradientDrawable()
        
        // 设置背景色
        if (!style.backgroundColor.isNullOrEmpty()) {
            try {
                val color: Int = Color.parseColor(style.backgroundColor)
                drawable.setColor(color)
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认透明色
                drawable.setColor(Color.TRANSPARENT)
            }
        } else {
            drawable.setColor(Color.TRANSPARENT)
        }
        
        // 设置圆角
        if (style.cornerRadius > 0) {
            drawable.cornerRadius = dpToPx(view, style.cornerRadius)
        }
        
        view.background = drawable
    }
    
    /**
     * 应用外边距样式
     */
    private fun applyMarginStyle(view: View, style: FloorStyle) {
        val layoutParams: ViewGroup.LayoutParams? = view.layoutParams
        if (layoutParams is ViewGroup.MarginLayoutParams) {
            layoutParams.setMargins(
                dpToPx(view, style.marginLeft.toFloat()).toInt(),
                dpToPx(view, style.marginTop.toFloat()).toInt(),
                dpToPx(view, style.marginRight.toFloat()).toInt(),
                dpToPx(view, style.marginBottom.toFloat()).toInt()
            )
            view.layoutParams = layoutParams
        }
    }
    
    /**
     * 应用内边距样式
     */
    private fun applyPaddingStyle(view: View, style: FloorStyle) {
        view.setPadding(
            dpToPx(view, style.paddingLeft.toFloat()).toInt(),
            dpToPx(view, style.paddingTop.toFloat()).toInt(),
            dpToPx(view, style.paddingRight.toFloat()).toInt(),
            dpToPx(view, style.paddingBottom.toFloat()).toInt()
        )
    }
    
    /**
     * 应用阴影样式
     */
    private fun applyElevationStyle(view: View, style: FloorStyle) {
        if (style.elevation > 0) {
            ViewCompat.setElevation(view, dpToPx(view, style.elevation))
        }
    }
    
    /**
     * dp转px
     */
    private fun dpToPx(view: View, dp: Float): Float {
        return dp * view.context.resources.displayMetrics.density
    }
    
    /**
     * 创建渐变背景
     * @param startColor 开始颜色
     * @param endColor 结束颜色
     * @param orientation 渐变方向
     * @param cornerRadius 圆角半径
     */
    fun createGradientDrawable(
        startColor: String,
        endColor: String,
        orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.TOP_BOTTOM,
        cornerRadius: Float = 0f
    ): GradientDrawable {
        val colors = intArrayOf(
            Color.parseColor(startColor),
            Color.parseColor(endColor)
        )
        
        return GradientDrawable(orientation, colors).apply {
            if (cornerRadius > 0) {
                this.cornerRadius = cornerRadius
            }
        }
    }
    
    /**
     * 创建纯色背景
     * @param color 背景色
     * @param cornerRadius 圆角半径
     */
    fun createSolidDrawable(color: String, cornerRadius: Float = 0f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            if (cornerRadius > 0) {
                this.cornerRadius = cornerRadius
            }
        }
    }
    
    /**
     * 创建描边背景
     * @param strokeColor 描边颜色
     * @param strokeWidth 描边宽度
     * @param fillColor 填充颜色，null为透明
     * @param cornerRadius 圆角半径
     */
    fun createStrokeDrawable(
        strokeColor: String,
        strokeWidth: Float,
        fillColor: String? = null,
        cornerRadius: Float = 0f
    ): GradientDrawable {
        return GradientDrawable().apply {
            setStroke(strokeWidth.toInt(), Color.parseColor(strokeColor))
            fillColor?.let { setColor(Color.parseColor(it)) }
            if (cornerRadius > 0) {
                this.cornerRadius = cornerRadius
            }
        }
    }
} 