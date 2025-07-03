package com.github.flexfloorlib.utils

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.adapter.FloorAdapter
import com.github.flexfloorlib.model.FloorData

/**
 * 吸顶楼层功能的辅助类
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
     * 附加到RecyclerView
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
     * 从RecyclerView分离
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
     * 根据滚动位置更新吸顶头部
     */
    private fun updateStickyHeader() {
        val adapter = recyclerView.adapter as? FloorAdapter ?: return
        val floorDataList = adapter.getFloorDataList()
        
        // 查找第一个可见位置
        val firstVisiblePosition = findFirstVisiblePosition()
        if (firstVisiblePosition == RecyclerView.NO_POSITION) return
        
        // 查找当前位置的吸顶楼层
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
     * 查找第一个可见位置
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
     * 为给定位置查找吸顶位置
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
     * 为位置创建吸顶视图
     */
    private fun createStickyView(position: Int) {
        val adapter = recyclerView.adapter as? FloorAdapter ?: return
        val viewType = adapter.getItemViewType(position)
        
        // 为吸顶头部创建新的ViewHolder
        val stickyViewHolder = adapter.onCreateViewHolder(recyclerView, viewType)
        adapter.onBindViewHolder(stickyViewHolder, position)
        
        currentStickyView = stickyViewHolder.itemView
        
        // 测量和布局吸顶视图
        measureAndLayoutStickyView(currentStickyView!!)
    }
    
    /**
     * 测量和布局吸顶视图
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
     * 绘制吸顶头部
     */
    private fun drawStickyHeader(canvas: Canvas, parent: RecyclerView) {
        val stickyView = currentStickyView ?: return
        
        // 计算吸顶位置
        val stickyTop = getStickyViewTop()
        
        // 保存画布状态
        val saveCount = canvas.save()
        
        // 将画布平移到吸顶位置
        canvas.translate(0f, stickyTop.toFloat())
        
        // 绘制吸顶视图
        stickyView.draw(canvas)
        
        // 恢复画布状态
        canvas.restoreToCount(saveCount)
    }
    
    /**
     * 计算吸顶视图顶部位置
     */
    private fun getStickyViewTop(): Int {
        val stickyView = currentStickyView ?: return 0
        val stickyHeight = stickyView.height
        
        // 检查下一个吸顶项是否正在推动当前项向上
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
     * 查找当前位置之后的下一个吸顶位置
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
     * 检查吸顶辅助类是否已附加
     */
    fun isAttached(): Boolean = isAttached
} 