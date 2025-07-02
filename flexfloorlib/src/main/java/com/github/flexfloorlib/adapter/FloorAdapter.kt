package com.github.flexfloorlib.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.base.IFloor
import com.github.flexfloorlib.base.IFloorClickListener
import com.github.flexfloorlib.base.IFloorExposureListener
import com.github.flexfloorlib.manager.FloorManager
import com.github.flexfloorlib.model.FloorData

/**
 * 楼层RecyclerView适配器
 * 支持多种楼层类型，自动处理DiffUtil差异更新
 */
class FloorAdapter : ListAdapter<FloorData, RecyclerView.ViewHolder>(FloorDiffCallback()) {
    
    private val floorManager: FloorManager = FloorManager.getInstance()
    private var clickListener: IFloorClickListener? = null
    private var exposureListener: IFloorExposureListener? = null
    
    // 缓存楼层实例，避免重复创建
    private val floorInstanceCache: MutableMap<String, IFloor> = mutableMapOf()
    
    /**
     * 设置楼层点击监听器
     */
    fun setFloorClickListener(listener: IFloorClickListener?) {
        this.clickListener = listener
    }
    
    /**
     * 设置楼层曝光监听器
     */
    fun setFloorExposureListener(listener: IFloorExposureListener?) {
        this.exposureListener = listener
    }
    
    override fun getItemViewType(position: Int): Int {
        val floorData: FloorData = getItem(position)
        // 使用楼层类型的hashCode作为ViewType
        return floorData.floorType.typeName.hashCode()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // 根据ViewType找到对应的楼层数据
        val floorData: FloorData? = currentList.find { 
            it.floorType.typeName.hashCode() == viewType 
        }
        
        if (floorData != null) {
            val floor: IFloor = getFloorInstance(floorData.floorType.typeName)
            return floor.createViewHolder(parent)
        }
        
        // 如果找不到对应的楼层类型，抛出异常
        throw IllegalArgumentException("Unknown floor type for viewType: $viewType")
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val floorData: FloorData = getItem(position)
        val floor: IFloor = getFloorInstance(floorData.floorType.typeName)
        
        // 设置监听器
        floor.setClickListener(clickListener)
        floor.setExposureListener(exposureListener)
        
        // 绑定数据
        floor.bindViewHolder(holder, floorData, position)
    }
    
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        
        // 找到对应位置的楼层数据
        val position: Int = holder.bindingAdapterPosition
        if (position in 0..<itemCount) {
            val floorData: FloorData = getItem(position)
            val floor: IFloor = getFloorInstance(floorData.floorType.typeName)
            floor.onViewRecycled(holder)
        }
    }
    
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        
        val position: Int = holder.bindingAdapterPosition
        if (position in 0..<itemCount) {
            val floorData: FloorData = getItem(position)
            val floor: IFloor = getFloorInstance(floorData.floorType.typeName)
            floor.onViewAttachedToWindow(holder)
        }
    }
    
    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        
        val position: Int = holder.bindingAdapterPosition
        if (position in 0..<itemCount) {
            val floorData: FloorData = getItem(position)
            val floor: IFloor = getFloorInstance(floorData.floorType.typeName)
            floor.onViewDetachedFromWindow(holder)
        }
    }
    
    /**
     * 获取楼层实例，使用缓存避免重复创建
     */
    private fun getFloorInstance(floorType: String): IFloor {
        return floorInstanceCache.getOrPut(floorType) {
            floorManager.createFloor(floorType) 
                ?: throw IllegalArgumentException("Floor not registered: $floorType")
        }
    }
    
    /**
     * 清除楼层实例缓存
     */
    fun clearFloorCache() {
        floorInstanceCache.values.forEach { floor ->
            floor.onDestroy()
        }
        floorInstanceCache.clear()
    }
    
    /**
     * 更新楼层数据
     * @param newFloorList 新的楼层数据列表
     */
    fun updateFloors(newFloorList: List<FloorData>) {
        submitList(newFloorList)
    }
    
    /**
     * 获取指定位置的楼层数据
     * @param position 位置
     * @return 楼层数据，如果位置无效则返回null
     */
    fun getFloorData(position: Int): FloorData? {
        return if (position in 0..<itemCount) {
            getItem(position)
        } else {
            null
        }
    }
    
    /**
     * 获取指定楼层ID的位置
     * @param floorId 楼层ID
     * @return 位置，如果找不到则返回-1
     */
    fun getFloorPosition(floorId: String): Int {
        return currentList.indexOfFirst { it.floorId == floorId }
    }
    
    /**
     * 检查是否包含指定类型的楼层
     * @param floorType 楼层类型
     * @return 是否包含
     */
    fun hasFloorType(floorType: String): Boolean {
        return currentList.any { it.floorType.typeName == floorType }
    }
}

/**
 * 楼层DiffUtil回调
 * 用于计算楼层数据的差异，提升RecyclerView性能
 */
class FloorDiffCallback : DiffUtil.ItemCallback<FloorData>() {
    
    override fun areItemsTheSame(oldItem: FloorData, newItem: FloorData): Boolean {
        // 比较楼层ID是否相同
        return oldItem.floorId == newItem.floorId
    }
    
    override fun areContentsTheSame(oldItem: FloorData, newItem: FloorData): Boolean {
        // 比较楼层内容是否相同
        return oldItem == newItem
    }
    
    override fun getChangePayload(oldItem: FloorData, newItem: FloorData): Any? {
        // 返回变化的部分，用于局部更新
        val changes = mutableListOf<String>()
        
        if (oldItem.title != newItem.title) {
            changes.add("title")
        }
        
        if (oldItem.data != newItem.data) {
            changes.add("data")
        }
        
        if (oldItem.style != newItem.style) {
            changes.add("style")
        }
        
        if (oldItem.isVisible != newItem.isVisible) {
            changes.add("visibility")
        }
        
        return changes.ifEmpty { null }
    }
} 