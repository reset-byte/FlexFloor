package com.github.flexfloorlib.base

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.model.FloorData

/**
 * 楼层基础接口
 * 定义楼层的核心功能和生命周期方法
 */
interface IFloor {
    
    /**
     * 获取楼层类型
     */
    fun getFloorType(): String
    
    /**
     * 创建ViewHolder
     * @param parent 父容器
     * @return ViewHolder
     */
    fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder
    
    /**
     * 绑定数据到ViewHolder
     * @param holder ViewHolder
     * @param floorData 楼层数据
     * @param position 位置
     */
    fun bindViewHolder(holder: RecyclerView.ViewHolder, floorData: FloorData, position: Int)
    
    /**
     * ViewHolder被回收时调用
     * @param holder ViewHolder
     */
    fun onViewRecycled(holder: RecyclerView.ViewHolder) {}
    
    /**
     * ViewHolder附着到窗口时调用
     * @param holder ViewHolder
     */
    fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {}
    
    /**
     * ViewHolder从窗口分离时调用
     * @param holder ViewHolder
     */
    fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {}
    
    /**
     * 获取楼层优先级，用于排序和预加载
     */
    fun getPriority(): Int = 0
    
    /**
     * 是否支持懒加载
     */
    fun isSupportLazyLoad(): Boolean = false
    
    /**
     * 是否需要预加载数据
     */
    fun isNeedPreload(): Boolean = false
    
    /**
     * 预加载数据
     */
    fun preloadData() {}
    
    /**
     * 设置点击监听器
     */
    fun setClickListener(listener: IFloorClickListener?)
    
    /**
     * 设置曝光监听器
     */
    fun setExposureListener(listener: IFloorExposureListener?)
    
    /**
     * 楼层创建时调用
     */
    fun onCreate()
    
    /**
     * 楼层销毁时调用
     */
    fun onDestroy()
}

/**
 * 楼层生命周期监听接口
 */
interface IFloorLifecycle {
    
    /**
     * 楼层创建时调用
     */
    fun onCreate()
    
    /**
     * 楼层可见时调用
     */
    fun onVisible()
    
    /**
     * 楼层不可见时调用
     */
    fun onInvisible()
    
    /**
     * 楼层销毁时调用
     */
    fun onDestroy()
}

/**
 * 楼层点击事件监听接口
 */
interface IFloorClickListener {
    
    /**
     * 楼层点击事件
     * @param floorData 楼层数据
     * @param view 被点击的View
     * @param position 位置
     */
    fun onFloorClick(floorData: FloorData, view: View, position: Int)
    
    /**
     * 楼层长按事件
     * @param floorData 楼层数据
     * @param view 被长按的View
     * @param position 位置
     * @return 是否消费事件
     */
    fun onFloorLongClick(floorData: FloorData, view: View, position: Int): Boolean = false
}

/**
 * 楼层曝光监听接口
 */
interface IFloorExposureListener {
    
    /**
     * 楼层曝光事件
     * @param floorData 楼层数据
     * @param position 位置
     * @param exposureRatio 曝光比例 0.0-1.0
     */
    fun onFloorExposure(floorData: FloorData, position: Int, exposureRatio: Float)
}

/**
 * 楼层数据加载监听接口
 */
interface IFloorDataLoader {
    
    /**
     * 加载楼层数据
     * @param floorData 楼层数据
     * @param callback 加载回调
     */
    fun loadFloorData(floorData: FloorData, callback: FloorDataCallback)
}

/**
 * 楼层数据加载回调接口
 */
interface FloorDataCallback {
    
    /**
     * 数据加载成功
     * @param data 加载的数据
     */
    fun onDataLoaded(data: Any?)
    
    /**
     * 数据加载失败
     * @param error 错误信息
     */
    fun onDataLoadFailed(error: Throwable)
}

/**
 * 楼层样式应用接口
 */
interface IFloorStyleApplier {
    
    /**
     * 应用楼层样式
     * @param view 目标View
     * @param floorData 楼层数据
     */
    fun applyFloorStyle(view: View, floorData: FloorData)
} 