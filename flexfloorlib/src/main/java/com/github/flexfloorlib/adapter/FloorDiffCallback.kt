package com.github.flexfloorlib.adapter

import androidx.recyclerview.widget.DiffUtil
import com.github.flexfloorlib.model.FloorData

/**
 * DiffUtil callback for floor data comparison
 */
class FloorDiffCallback(
    private val oldList: List<FloorData>,
    private val newList: List<FloorData>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldFloor = oldList[oldItemPosition]
        val newFloor = newList[newItemPosition]
        return oldFloor.floorId == newFloor.floorId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldFloor = oldList[oldItemPosition]
        val newFloor = newList[newItemPosition]
        
        return oldFloor.floorType == newFloor.floorType &&
                oldFloor.floorConfig == newFloor.floorConfig &&
                oldFloor.businessData == newFloor.businessData &&
                oldFloor.priority == newFloor.priority &&
                oldFloor.isVisible == newFloor.isVisible &&
                oldFloor.isSticky == newFloor.isSticky &&
                oldFloor.loadPolicy == newFloor.loadPolicy &&
                oldFloor.cachePolicy == newFloor.cachePolicy &&
                oldFloor.exposureConfig == newFloor.exposureConfig
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldFloor = oldList[oldItemPosition]
        val newFloor = newList[newItemPosition]
        
        val changes = mutableMapOf<String, Any>()
        
        if (oldFloor.floorConfig != newFloor.floorConfig) {
            changes["config"] = newFloor.floorConfig
        }
        
        if (oldFloor.businessData != newFloor.businessData) {
            changes["data"] = newFloor.businessData
        }
        
        if (oldFloor.isVisible != newFloor.isVisible) {
            changes["visibility"] = newFloor.isVisible
        }
        
        return if (changes.isNotEmpty()) changes else null
    }
} 