package com.github.flexfloorlib.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.core.BaseFloor
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.observer.FloorExposureObserver

/**
 * Base ViewHolder for floor items
 */
abstract class BaseFloorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    
    protected var floorData: FloorData? = null
    protected var floor: BaseFloor<*>? = null
    private var exposureObserver: FloorExposureObserver? = null
    
    /**
     * Bind floor data to view
     */
    open fun bindFloor(floorData: FloorData, floor: BaseFloor<*>?, position: Int) {
        this.floorData = floorData
        this.floor = floor
        
        // Apply basic styling
        applyFloorConfig(floorData)
        
        // Bind floor-specific data
        floor?.bindView(itemView, position)
        
        // Setup click listener
        setupClickListener()
        
        // Setup exposure tracking
        setupExposureTracking()
    }
    
    /**
     * Apply floor configuration (styling, margins, etc.)
     */
    private fun applyFloorConfig(floorData: FloorData) {
        val config = floorData.floorConfig
        val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams
        
        // Apply margins
        layoutParams.setMargins(
            config.margin.left,
            config.margin.top,
            config.margin.right,
            config.margin.bottom
        )
        
        // Apply padding
        itemView.setPadding(
            config.padding.left,
            config.padding.top,
            config.padding.right,
            config.padding.bottom
        )
        
        // Apply background color and corner radius together
        applyBackgroundWithCornerRadius(config)
        
        // Apply elevation
        if (config.elevation > 0) {
            itemView.elevation = config.elevation
        }
        
        itemView.layoutParams = layoutParams
    }
    
    /**
     * Apply background color and corner radius using GradientDrawable
     */
    private fun applyBackgroundWithCornerRadius(config: com.github.flexfloorlib.model.FloorConfig) {
        val drawable = android.graphics.drawable.GradientDrawable()
        
        // Set background color
        config.backgroundColor?.let { color ->
            try {
                drawable.setColor(android.graphics.Color.parseColor(color))
            } catch (e: Exception) {
                e.printStackTrace()
                drawable.setColor(android.graphics.Color.WHITE) // 默认白色
            }
        } ?: run {
            drawable.setColor(android.graphics.Color.TRANSPARENT) // 默认透明
        }
        
        // Set corner radius
        if (config.cornerRadius > 0) {
            val cornerRadiusPx = convertDpToPx(config.cornerRadius, itemView.context)
            drawable.cornerRadius = cornerRadiusPx
        }
        
        // Apply the drawable as background
        itemView.background = drawable
    }
    
    /**
     * Convert dp to px
     */
    private fun convertDpToPx(dp: Float, context: android.content.Context): Float {
        val density = context.resources.displayMetrics.density
        return dp * density
    }
    
    /**
     * Setup click listener for floor
     */
    private fun setupClickListener() {
        if (floorData?.floorConfig?.clickable == true) {
            itemView.setOnClickListener { view ->
                floor?.onFloorClick(view)
                onFloorClicked(view)
            }
        } else {
            itemView.setOnClickListener(null)
            itemView.isClickable = false
        }
    }
    
    /**
     * Setup exposure tracking
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
     * Called when floor is clicked
     */
    protected open fun onFloorClicked(view: View) {
        // Override in subclasses if needed
    }
    
    /**
     * Called when floor is exposed
     */
    protected open fun onFloorExposed(floorId: String, exposureData: Map<String, Any>) {
        // Override in subclasses if needed
    }
    
    /**
     * Called when ViewHolder is recycled
     */
    open fun onViewRecycled() {
        exposureObserver?.stopTracking()
        floor?.onDetach()
    }
    
    /**
     * Called when ViewHolder is attached to window
     */
    open fun onViewAttachedToWindow() {
        exposureObserver?.startTracking()
        floor?.onAttach()
    }
    
    /**
     * Called when ViewHolder is detached from window
     */
    open fun onViewDetachedFromWindow() {
        exposureObserver?.stopTracking()
        floor?.onDetach()
    }
} 