package com.github.flexfloorlib.widget

import android.graphics.PointF
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.adapter.FloorAdapter
import com.github.flexfloorlib.model.FloorData

/**
 * 支持吸顶楼层的自定义LayoutManager
 * 继承自LinearLayoutManager，添加吸顶功能
 */
class StickyFloorLayoutManager(
    private val recyclerView: RecyclerView,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false
) : LinearLayoutManager(recyclerView.context, orientation, reverseLayout) {
    
    private var stickyView: View? = null
    private var stickyPosition: Int = RecyclerView.NO_POSITION
    private var stickyOffset: Int = 0
    
    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val scrolled: Int = super.scrollVerticallyBy(dy, recycler, state)
        updateStickyView(recycler)
        return scrolled
    }
    
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        updateStickyView(recycler)
    }
    
    /**
     * 更新吸顶视图
     */
    private fun updateStickyView(recycler: RecyclerView.Recycler?) {
        if (recycler == null || childCount == 0) {
            clearStickyView()
            return
        }
        
        val adapter: RecyclerView.Adapter<*>? = recyclerView.adapter
        if (adapter !is FloorAdapter) {
            clearStickyView()
            return
        }
        
        // 查找需要吸顶的楼层
        val stickyCandidate: StickyCandidate? = findStickyCandidate(adapter)
        
        if (stickyCandidate != null) {
            createOrUpdateStickyView(stickyCandidate, recycler)
        } else {
            clearStickyView()
        }
    }
    
    /**
     * 查找吸顶候选楼层
     */
    private fun findStickyCandidate(adapter: FloorAdapter): StickyCandidate? {
        val firstVisiblePosition: Int = findFirstVisibleItemPosition()
        if (firstVisiblePosition == RecyclerView.NO_POSITION) {
            return null
        }
        
        var candidatePosition: Int = RecyclerView.NO_POSITION
        var candidateData: FloorData? = null
        
        // 从第一个可见位置向前查找最近的吸顶楼层
        for (i in firstVisiblePosition downTo 0) {
            val floorData: FloorData? = adapter.getFloorData(i)
            if (floorData?.isSticky == true) {
                candidatePosition = i
                candidateData = floorData
                break
            }
        }
        
        return if (candidatePosition != RecyclerView.NO_POSITION && candidateData != null) {
            StickyCandidate(candidatePosition, candidateData)
        } else {
            null
        }
    }
    
    /**
     * 创建或更新吸顶视图
     */
    private fun createOrUpdateStickyView(candidate: StickyCandidate, recycler: RecyclerView.Recycler) {
        val adapter: FloorAdapter = recyclerView.adapter as? FloorAdapter ?: return
        
        // 如果吸顶位置发生变化，重新创建吸顶视图
        if (stickyPosition != candidate.position) {
            clearStickyView()
            
            val stickyViewHolder: RecyclerView.ViewHolder = adapter.createViewHolder(recyclerView, adapter.getItemViewType(candidate.position))
            adapter.bindViewHolder(stickyViewHolder, candidate.position)
            
            stickyView = stickyViewHolder.itemView
            stickyPosition = candidate.position
            
            // 添加到RecyclerView中
            addView(stickyView, 0)
        }
        
        // 计算吸顶视图的位置
        val stickyViewHeight: Int = stickyView?.height ?: 0
        val nextStickyPosition: Int = findNextStickyPosition(candidate.position + 1, adapter)
        
        stickyOffset = if (nextStickyPosition != RecyclerView.NO_POSITION) {
            val nextStickyView: View? = findViewByPosition(nextStickyPosition)
            if (nextStickyView != null) {
                val nextTop: Int = getDecoratedTop(nextStickyView)
                minOf(0, nextTop - stickyViewHeight)
            } else {
                0
            }
        } else {
            0
        }
        
        // 布局吸顶视图
        layoutStickyView()
    }
    
    /**
     * 查找下一个吸顶楼层位置
     */
    private fun findNextStickyPosition(startPosition: Int, adapter: FloorAdapter): Int {
        for (i in startPosition until adapter.itemCount) {
            val floorData: FloorData? = adapter.getFloorData(i)
            if (floorData?.isSticky == true) {
                return i
            }
        }
        return RecyclerView.NO_POSITION
    }
    
    /**
     * 布局吸顶视图
     */
    private fun layoutStickyView() {
        val stickyView: View = this.stickyView ?: return
        val recyclerView: RecyclerView = stickyView.parent as? RecyclerView ?: return
        
        val left: Int = paddingLeft
        val right: Int = width - paddingRight
        val top: Int = paddingTop + stickyOffset
        val bottom: Int = top + stickyView.height
        
        stickyView.layout(left, top, right, bottom)
        
        // 确保吸顶视图在最上层
        stickyView.translationZ = 1f
    }
    
    /**
     * 清除吸顶视图
     */
    private fun clearStickyView() {
        stickyView?.let { view ->
            removeView(view)
        }
        stickyView = null
        stickyPosition = RecyclerView.NO_POSITION
        stickyOffset = 0
    }
    
    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        return super.computeScrollVectorForPosition(targetPosition)
    }
    
    override fun canScrollVertically(): Boolean {
        return orientation == RecyclerView.VERTICAL
    }
    
    override fun canScrollHorizontally(): Boolean {
        return orientation == RecyclerView.HORIZONTAL
    }
    
    /**
     * 获取吸顶视图
     */
    fun getStickyView(): View? {
        return stickyView
    }
    
    /**
     * 获取吸顶位置
     */
    fun getStickyPosition(): Int {
        return stickyPosition
    }
    
    /**
     * 检查指定位置是否正在吸顶
     */
    fun isStickyPosition(position: Int): Boolean {
        return stickyPosition == position
    }
}

/**
 * 吸顶候选数据类
 */
private data class StickyCandidate(
    val position: Int,
    val floorData: FloorData
) 