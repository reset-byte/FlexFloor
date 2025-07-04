package com.github.flexfloorlib.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.core.BaseFloor
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.observer.FloorExposureObserver

/**
 * 楼层项目的基础ViewHolder
 */
abstract class BaseFloorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    
    protected var floorData: FloorData? = null
    protected var floor: BaseFloor<*>? = null
    private var exposureObserver: FloorExposureObserver? = null
    
    /**
     * 绑定楼层数据到视图
     */
    fun bindFloor(floorData: FloorData, floor: BaseFloor<*>?, position: Int) {
        this.floorData = floorData
        this.floor = floor
        
        // 应用基础样式
        applyFloorConfig(floorData)
        
        // 绑定楼层特定数据
        floor?.bindView(itemView, position)
        
        // 设置点击监听器
        setupClickListener()
        
        // 设置曝光追踪
        setupExposureTracking()
    }
    
    /**
     * 应用楼层配置（样式、边距等）
     */
    private fun applyFloorConfig(floorData: FloorData) {
        val config = floorData.floorConfig
        
        // 应用外边距（将dp转换为px）
        val marginLeft = convertDpToPx(config.margin.left.toFloat(), itemView.context)
        val marginTop = convertDpToPx(config.margin.top.toFloat(), itemView.context)
        val marginRight = convertDpToPx(config.margin.right.toFloat(), itemView.context)
        val marginBottom = convertDpToPx(config.margin.bottom.toFloat(), itemView.context)
        
        if (itemView.layoutParams is android.view.ViewGroup.MarginLayoutParams) {
            val marginParams = itemView.layoutParams as android.view.ViewGroup.MarginLayoutParams
            marginParams.setMargins(
                marginLeft.toInt(),
                marginTop.toInt(),
                marginRight.toInt(),
                marginBottom.toInt()
            )
            itemView.layoutParams = marginParams
        }
        
        // 应用内边距（将dp转换为px）
        val paddingLeft = convertDpToPx(config.padding.left.toFloat(), itemView.context)
        val paddingTop = convertDpToPx(config.padding.top.toFloat(), itemView.context)
        val paddingRight = convertDpToPx(config.padding.right.toFloat(), itemView.context)
        val paddingBottom = convertDpToPx(config.padding.bottom.toFloat(), itemView.context)
        
        itemView.setPadding(
            paddingLeft.toInt(),
            paddingTop.toInt(),
            paddingRight.toInt(),
            paddingBottom.toInt()
        )
        
        // 应用背景色和圆角半径
        applyBackgroundWithCornerRadius(config)
        
        // 应用阴影
        if (config.elevation > 0) {
            itemView.elevation = config.elevation
        }
    }
    
    /**
     * 使用GradientDrawable应用背景色和圆角半径
     */
    private fun applyBackgroundWithCornerRadius(config: com.github.flexfloorlib.model.FloorConfig) {
        if (config.cornerRadius > 0 || config.backgroundColor != null) {
            val drawable = android.graphics.drawable.GradientDrawable()
            
            // 设置背景颜色
            if (config.backgroundColor != null) {
                try {
                    drawable.setColor(android.graphics.Color.parseColor(config.backgroundColor))
                } catch (e: IllegalArgumentException) {
                    // 颜色解析失败，使用默认颜色
                    drawable.setColor(android.graphics.Color.WHITE)
                }
            }
            
            // 设置圆角半径
            if (config.cornerRadius > 0) {
                drawable.cornerRadius = convertDpToPx(config.cornerRadius, itemView.context)
            }
            
            // 应用drawable作为背景
            itemView.background = drawable
        }
    }
    
    /**
     * 将dp转换为px
     */
    private fun convertDpToPx(dp: Float, context: android.content.Context): Float {
        return dp * context.resources.displayMetrics.density
    }
    
    /**
     * 设置楼层点击监听器
     */
    private fun setupClickListener() {
        if (floorData?.floorConfig?.clickable == true) {
            itemView.setOnClickListener {
                floor?.onFloorClick(it)
                onFloorClicked(it)
            }
        } else {
            itemView.setOnClickListener(null)
            itemView.isClickable = false
        }
    }
    
    /**
     * 设置曝光追踪
     */
    private fun setupExposureTracking() {
        floorData?.exposureConfig?.let { exposureConfig ->
            exposureObserver = FloorExposureObserver(
                view = itemView,
                floorId = floorData?.floorId ?: "",
                exposureConfig = exposureConfig,
                onExposed = { floorId, exposureData ->
                    onFloorExposed(floorId, exposureData)
                }
            )
        }
    }
    
    /**
     * 楼层被点击时调用
     */
    protected open fun onFloorClicked(view: View) {
        // 子类可以重写此方法
    }
    
    /**
     * 楼层被曝光时调用
     */
    protected open fun onFloorExposed(floorId: String, exposureData: Map<String, Any>) {
        // 子类可以重写此方法
    }
    
    /**
     * ViewHolder被回收时调用
     */
    open fun onViewRecycled() {
        exposureObserver?.stopTracking()
        floor?.onDetach()
    }
    
    /**
     * ViewHolder被附加到窗口时调用
     */
    open fun onViewAttachedToWindow() {
        exposureObserver?.startTracking()
        floor?.onAttach()
    }
    
    /**
     * ViewHolder从窗口分离时调用
     */
    open fun onViewDetachedFromWindow() {
        exposureObserver?.stopTracking()
        floor?.onDetach()
    }
} 