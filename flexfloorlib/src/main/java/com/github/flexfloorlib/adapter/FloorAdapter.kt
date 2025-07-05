package com.github.flexfloorlib.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.core.BaseFloor
import com.github.flexfloorlib.core.FloorFactory
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.viewholder.BaseFloorViewHolder
import com.github.flexfloorlib.viewholder.DefaultFloorViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 楼层化布局的RecyclerView适配器
 */
class FloorAdapter : RecyclerView.Adapter<BaseFloorViewHolder>() {
    
    private var floorDataList = mutableListOf<FloorData>()
    private val floorInstanceCache = mutableMapOf<String, BaseFloor<*>>()
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 回调函数
    private var onFloorClickListener: ((FloorData, Int) -> Unit)? = null
    private var onFloorExposureListener: ((String, Map<String, Any>) -> Unit)? = null
    private var onFloorLoadListener: ((FloorData) -> Unit)? = null
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseFloorViewHolder {
        val floorData = getFloorDataByViewType(viewType)
        val floor = getOrCreateFloor(floorData)
        
        return if (floor != null) {
            val layoutId = floor.getLayoutResId()
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            DefaultFloorViewHolder(view)
        } else {
            // 降级到默认ViewHolder
            val view = android.widget.TextView(parent.context).apply {
                text = "未知楼层类型: ${floorData.floorType}"
                setPadding(16, 16, 16, 16)
            }
            DefaultFloorViewHolder(view)
        }
    }
    
    override fun onBindViewHolder(holder: BaseFloorViewHolder, position: Int) {
        val floorData = floorDataList[position]
        val floor = getOrCreateFloor(floorData)
        
        // 如果需要则加载数据
        if (floor?.needsDataLoading() == true) {
            loadFloorData(floorData, floor)
        }
        
        holder.bindFloor(floorData, floor, position)
    }
    
    override fun getItemCount(): Int = floorDataList.size
    
    override fun getItemViewType(position: Int): Int {
        // 使用楼层类型哈希作为视图类型
        return floorDataList[position].floorType.hashCode()
    }
    
    override fun onViewRecycled(holder: BaseFloorViewHolder) {
        super.onViewRecycled(holder)
        holder.onViewRecycled()
    }
    
    override fun onViewAttachedToWindow(holder: BaseFloorViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }
    
    override fun onViewDetachedFromWindow(holder: BaseFloorViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }
    
    /**
     * 使用DiffUtil更新楼层数据
     */
    fun updateFloorData(newFloorDataList: List<FloorData>) {
        // 清除旧的楼层实例缓存，确保重新创建
        floorInstanceCache.values.forEach { it.onDestroy() }
        floorInstanceCache.clear()
        
        val diffCallback = FloorDiffCallback(floorDataList, newFloorDataList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        floorDataList.clear()
        floorDataList.addAll(newFloorDataList)
        
        diffResult.dispatchUpdatesTo(this)
    }
    
    /**
     * 添加单个楼层数据
     */
    fun addFloorData(floorData: FloorData, position: Int = floorDataList.size) {
        addFloorData(floorData, position, true)
    }
    
    /**
     * 添加单个楼层数据（支持动画控制）
     */
    fun addFloorData(floorData: FloorData, position: Int = floorDataList.size, withAnimation: Boolean = true) {
        floorDataList.add(position, floorData)
        
        if (withAnimation) {
            notifyItemInserted(position)
        } else {
            notifyDataSetChanged()
        }
    }
    
    /**
     * 移除楼层数据
     */
    fun removeFloorData(position: Int) {
        if (position in 0 until floorDataList.size) {
            val floorData = floorDataList.removeAt(position)
            floorInstanceCache.remove(floorData.floorId)
            notifyItemRemoved(position)
        }
    }
    
    /**
     * 更新单个楼层数据
     */
    fun updateFloorData(position: Int, floorData: FloorData) {
        if (position in 0 until floorDataList.size) {
            floorDataList[position] = floorData
            notifyItemChanged(position)
        }
    }
    
    /**
     * 根据视图类型获取楼层数据
     */
    private fun getFloorDataByViewType(viewType: Int): FloorData {
        return floorDataList.find { it.floorType.hashCode() == viewType }
            ?: floorDataList.first() // 降级处理
    }
    
    /**
     * 获取或创建楼层实例
     */
    private fun getOrCreateFloor(floorData: FloorData): BaseFloor<*>? {
        return floorInstanceCache.getOrPut(floorData.floorId) {
            FloorFactory.createFloor(floorData) ?: return null
        }
    }
    
    /**
     * 异步加载楼层数据
     */
    private fun loadFloorData(floorData: FloorData, floor: BaseFloor<*>) {
        adapterScope.launch {
            try {
                onFloorLoadListener?.invoke(floorData)
                val data = floor.loadData()
                @Suppress("UNCHECKED_CAST")
                (floor as BaseFloor<Any>).initFloor(floorData, data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 设置楼层点击监听器
     */
    fun setOnFloorClickListener(listener: (FloorData, Int) -> Unit) {
        onFloorClickListener = listener
    }
    
    /**
     * 设置楼层曝光监听器
     */
    fun setOnFloorExposureListener(listener: (String, Map<String, Any>) -> Unit) {
        onFloorExposureListener = listener
    }
    
    /**
     * 设置楼层加载监听器
     */
    fun setOnFloorLoadListener(listener: (FloorData) -> Unit) {
        onFloorLoadListener = listener
    }
    
    /**
     * 清除所有缓存的楼层实例
     */
    fun clearCache() {
        floorInstanceCache.values.forEach { it.onDestroy() }
        floorInstanceCache.clear()
    }
    
    /**
     * 获取当前楼层数据列表
     */
    fun getFloorDataList(): List<FloorData> = floorDataList.toList()
} 