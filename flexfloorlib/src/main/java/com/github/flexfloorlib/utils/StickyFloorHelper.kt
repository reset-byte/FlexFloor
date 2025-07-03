package com.github.flexfloorlib.utils

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.adapter.FloorAdapter
import com.github.flexfloorlib.model.FloorData

/**
 * Helper for sticky floor functionality
 */
class StickyFloorHelper(private val recyclerView: RecyclerView) {
    
    private var currentStickyView: View? = null
    private var currentStickyPosition = -1
    private var isAttached = false
    
    private val itemDecoration = object : RecyclerView.ItemDecoration() {
        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDrawOver(c, parent, state)
            drawStickyHeader(c, parent)
        }
    }
    
    /**
     * Attach to RecyclerView
     */
    fun attachToRecyclerView() {
        if (!isAttached) {
            recyclerView.addItemDecoration(itemDecoration)
            recyclerView.addOnScrollListener(scrollListener)
            isAttached = true
            updateStickyHeader()
        }
    }
    
    /**
     * Detach from RecyclerView
     */
    fun detachFromRecyclerView() {
        if (isAttached) {
            recyclerView.removeItemDecoration(itemDecoration)
            recyclerView.removeOnScrollListener(scrollListener)
            isAttached = false
            currentStickyView = null
            currentStickyPosition = -1
        }
    }
    
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            updateStickyHeader()
        }
    }
    
    /**
     * Update sticky header based on scroll position
     */
    private fun updateStickyHeader() {
        val adapter = recyclerView.adapter as? FloorAdapter ?: return
        val floorDataList = adapter.getFloorDataList()
        
        // Find the first visible position
        val firstVisiblePosition = findFirstVisiblePosition()
        if (firstVisiblePosition == RecyclerView.NO_POSITION) return
        
        // Find the sticky floor for current position
        val stickyPosition = findStickyPositionForPosition(floorDataList, firstVisiblePosition)
        
        if (stickyPosition != currentStickyPosition) {
            currentStickyPosition = stickyPosition
            
            if (stickyPosition >= 0) {
                createStickyView(stickyPosition)
            } else {
                currentStickyView = null
            }
        }
    }
    
    /**
     * Find first visible position
     */
    private fun findFirstVisiblePosition(): Int {
        return recyclerView.layoutManager?.let { layoutManager ->
            val childCount = layoutManager.childCount
            if (childCount == 0) return RecyclerView.NO_POSITION
            
            val firstChild = layoutManager.getChildAt(0) ?: return RecyclerView.NO_POSITION
            recyclerView.getChildAdapterPosition(firstChild)
        } ?: RecyclerView.NO_POSITION
    }
    
    /**
     * Find sticky position for given position
     */
    private fun findStickyPositionForPosition(floorDataList: List<FloorData>, position: Int): Int {
        for (i in position downTo 0) {
            if (i < floorDataList.size && floorDataList[i].isSticky) {
                return i
            }
        }
        return -1
    }
    
    /**
     * Create sticky view for position
     */
    private fun createStickyView(position: Int) {
        val adapter = recyclerView.adapter as? FloorAdapter ?: return
        val viewType = adapter.getItemViewType(position)
        
        // Create a new ViewHolder for the sticky header
        val stickyViewHolder = adapter.onCreateViewHolder(recyclerView, viewType)
        adapter.onBindViewHolder(stickyViewHolder, position)
        
        currentStickyView = stickyViewHolder.itemView
        
        // Measure and layout the sticky view
        measureAndLayoutStickyView(currentStickyView!!)
    }
    
    /**
     * Measure and layout sticky view
     */
    private fun measureAndLayoutStickyView(stickyView: View) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(recyclerView.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        
        val layoutParams = stickyView.layoutParams
        if (layoutParams != null && layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(recyclerView.width, View.MeasureSpec.AT_MOST)
            stickyView.measure(widthMeasureSpec, heightSpec)
        } else {
            stickyView.measure(widthSpec, heightSpec)
        }
        
        stickyView.layout(0, 0, stickyView.measuredWidth, stickyView.measuredHeight)
    }
    
    /**
     * Draw sticky header
     */
    private fun drawStickyHeader(canvas: Canvas, parent: RecyclerView) {
        val stickyView = currentStickyView ?: return
        
        // Calculate sticky position
        val stickyTop = getStickyViewTop()
        
        // Save canvas state
        val saveCount = canvas.save()
        
        // Translate canvas to sticky position
        canvas.translate(0f, stickyTop.toFloat())
        
        // Draw the sticky view
        stickyView.draw(canvas)
        
        // Restore canvas state
        canvas.restoreToCount(saveCount)
    }
    
    /**
     * Calculate sticky view top position
     */
    private fun getStickyViewTop(): Int {
        val stickyView = currentStickyView ?: return 0
        val stickyHeight = stickyView.height
        
        // Check if next sticky item is pushing current one up
        val nextStickyPosition = findNextStickyPosition(currentStickyPosition)
        if (nextStickyPosition != -1) {
            val nextStickyView = recyclerView.layoutManager?.findViewByPosition(nextStickyPosition)
            if (nextStickyView != null) {
                val nextStickyTop = nextStickyView.top
                if (nextStickyTop < stickyHeight) {
                    return nextStickyTop - stickyHeight
                }
            }
        }
        
        return 0
    }
    
    /**
     * Find next sticky position after current position
     */
    private fun findNextStickyPosition(currentPosition: Int): Int {
        val adapter = recyclerView.adapter as? FloorAdapter ?: return -1
        val floorDataList = adapter.getFloorDataList()
        
        for (i in currentPosition + 1 until floorDataList.size) {
            if (floorDataList[i].isSticky) {
                return i
            }
        }
        
        return -1
    }
    
    /**
     * Check if sticky helper is attached
     */
    fun isAttached(): Boolean = isAttached
} 