package com.github.flexfloorlib.base

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.FloorStyle
import com.github.flexfloorlib.utils.FloorStyleUtils

/**
 * 楼层基础抽象类
 * 提供楼层的默认实现和通用功能
 */
abstract class BaseFloor : IFloor, IFloorLifecycle, IFloorStyleApplier {
    
    private var floorClickListener: IFloorClickListener? = null
    private var floorExposureListener: IFloorExposureListener? = null
    private var floorDataLoader: IFloorDataLoader? = null
    protected var isCreated: Boolean = false
    protected var isVisible: Boolean = false
    
    /**
     * 设置点击监听器
     */
    override fun setClickListener(listener: IFloorClickListener?) {
        this.floorClickListener = listener
    }
    
    /**
     * 设置曝光监听器
     */
    override fun setExposureListener(listener: IFloorExposureListener?) {
        this.floorExposureListener = listener
    }
    
    /**
     * 设置数据加载器
     */
    fun setDataLoader(loader: IFloorDataLoader?) {
        this.floorDataLoader = loader
    }
    
    override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view: View = onCreateView(parent)
        return BaseFloorViewHolder(view)
    }
    
    override fun bindViewHolder(holder: RecyclerView.ViewHolder, floorData: FloorData, position: Int) {
        // 应用样式
        applyFloorStyle(holder.itemView, floorData)
        
        // 设置点击事件
        setupClickListener(holder.itemView, floorData, position)
        
        // 绑定数据
        onBindData(holder, floorData, position)
        
        // 处理曝光监听
        handleExposure(holder.itemView, floorData, position)
    }
    
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        onViewRecycledInternal(holder)
    }
    
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (!isVisible) {
            isVisible = true
            onVisible()
        }
        onViewAttachedToWindowInternal(holder)
    }
    
    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (isVisible) {
            isVisible = false
            onInvisible()
        }
        onViewDetachedFromWindowInternal(holder)
    }
    
    override fun onCreate() {
        if (!isCreated) {
            isCreated = true
            onCreateInternal()
        }
    }
    
    override fun onVisible() {
        onVisibleInternal()
    }
    
    override fun onInvisible() {
        onInvisibleInternal()
    }
    
    override fun onDestroy() {
        isCreated = false
        isVisible = false
        onDestroyInternal()
    }
    
    override fun applyFloorStyle(view: View, floorData: FloorData) {
        when (val style = floorData.style) {
            is FloorStyle -> FloorStyleUtils.applyStyle(view, style)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                FloorStyleUtils.applyStyle(view, style as? Map<String, Any>)
            }
            null -> {
                // 检查data中是否包含style信息
                when (val data = floorData.data) {
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        val dataMap = data as? Map<String, Any>
                        val styleMap = dataMap?.get("style") as? Map<String, Any>
                        FloorStyleUtils.applyStyle(view, styleMap)
                    }
                }
            }
        }
    }
    
    /**
     * 创建View，子类需要实现
     */
    protected abstract fun onCreateView(parent: ViewGroup): View
    
    /**
     * 绑定数据，子类需要实现
     */
    protected abstract fun onBindData(holder: RecyclerView.ViewHolder, floorData: FloorData, position: Int)
    
    /**
     * 楼层创建时的内部处理
     */
    protected open fun onCreateInternal() {}
    
    /**
     * 楼层可见时的内部处理
     */
    protected open fun onVisibleInternal() {}
    
    /**
     * 楼层不可见时的内部处理
     */
    protected open fun onInvisibleInternal() {}
    
    /**
     * 楼层销毁时的内部处理
     */
    protected open fun onDestroyInternal() {}
    
    /**
     * ViewHolder回收时的内部处理
     */
    protected open fun onViewRecycledInternal(holder: RecyclerView.ViewHolder) {}
    
    /**
     * ViewHolder附着到窗口时的内部处理
     */
    protected open fun onViewAttachedToWindowInternal(holder: RecyclerView.ViewHolder) {}
    
    /**
     * ViewHolder从窗口分离时的内部处理
     */
    protected open fun onViewDetachedFromWindowInternal(holder: RecyclerView.ViewHolder) {}
    
    /**
     * 设置点击监听器
     */
    private fun setupClickListener(view: View, floorData: FloorData, position: Int) {
        view.setOnClickListener {
            floorClickListener?.onFloorClick(floorData, it, position)
        }
        
        view.setOnLongClickListener {
            floorClickListener?.onFloorLongClick(floorData, it, position) ?: false
        }
    }
    
    /**
     * 处理曝光监听
     */
    private fun handleExposure(view: View, floorData: FloorData, position: Int) {
        // 使用ViewTreeObserver监听View的可见性变化
        view.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            view.getGlobalVisibleRect(rect)
            val viewHeight: Int = view.height
            val visibleHeight: Int = rect.height()
            
            if (viewHeight > 0) {
                val exposureRatio: Float = visibleHeight.toFloat() / viewHeight.toFloat()
                if (exposureRatio > 0.5f) { // 曝光比例大于50%才算曝光
                    floorExposureListener?.onFloorExposure(floorData, position, exposureRatio)
                }
            }
        }
    }
    
    /**
     * 获取Context
     */
    protected fun getContext(holder: RecyclerView.ViewHolder): Context {
        return holder.itemView.context
    }
}

/**
 * 基础楼层ViewHolder
 */
class BaseFloorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) 