package com.github.flexfloorlib

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.core.BaseFloor
import com.github.flexfloorlib.core.FloorFactory
import com.github.flexfloorlib.core.FloorManager
import com.github.flexfloorlib.model.FloorType

/**
 * FlexFloor库的主入口
 * 提供楼层化布局管理的简化API
 */
object FlexFloor {
    
    /**
     * 使用上下文和生命周期所有者初始化FlexFloor
     */
    fun with(context: Context, lifecycleOwner: LifecycleOwner): FloorBuilder {
        return FloorBuilder(context, lifecycleOwner)
    }
    
    /**
     * 注册楼层类型及其创建器
     */
    fun <T : Any> registerFloor(floorType: FloorType, creator: () -> BaseFloor<T>) {
        FloorFactory.registerFloor(floorType, creator)
    }
    
    /**
     * 使用自定义类型名称注册自定义楼层类型
     */
    fun <T : Any> registerCustomFloor(typeName: String, creator: () -> BaseFloor<T>) {
        FloorFactory.registerCustomFloor(typeName, creator)
    }
    
    /**
     * 检查楼层类型是否已注册
     */
    fun isFloorTypeRegistered(floorType: FloorType): Boolean {
        return FloorFactory.isFloorTypeRegistered(floorType)
    }
    
    /**
     * 获取版本信息
     */
    fun getVersion(): String = "1.0.0"
}

/**
 * FlexFloor配置构建器类
 */
class FloorBuilder internal constructor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    
    private var enablePreloading = true
    private var preloadDistance = 3
    private var enableStickyFloors = false
    
    /**
     * 配置预加载
     */
    fun enablePreloading(enable: Boolean, distance: Int = 3): FloorBuilder {
        this.enablePreloading = enable
        this.preloadDistance = distance
        return this
    }
    
    /**
     * 配置吸顶楼层
     */
    fun enableStickyFloors(enable: Boolean): FloorBuilder {
        this.enableStickyFloors = enable
        return this
    }
    
    /**
     * 构建并与RecyclerView关联
     */
    fun setupWith(recyclerView: RecyclerView): FloorManager {
        return FloorManager.create(context, lifecycleOwner)
            .enablePreloading(enablePreloading, preloadDistance)
            .enableStickyFloors(enableStickyFloors)
            .setupWithRecyclerView(recyclerView)
    }
} 