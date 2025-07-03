package com.github.flexfloorlib.observer

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import com.github.flexfloorlib.model.ExposureConfig

/**
 * 楼层曝光和可见性追踪观察者
 */
class FloorExposureObserver(
    private val view: View,
    private val floorId: String,
    private val exposureConfig: ExposureConfig,
    private val onExposed: (String, Map<String, Any>) -> Unit
) {
    
    private var isTracking = false
    private var isExposed = false
    private var exposureStartTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var exposureCheckRunnable: Runnable? = null
    
    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        checkExposure()
    }
    
    private val scrollChangeListener = ViewTreeObserver.OnScrollChangedListener {
        checkExposure()
    }
    
    /**
     * 开始追踪曝光
     */
    fun startTracking() {
        if (isTracking) return
        
        isTracking = true
        view.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        view.viewTreeObserver.addOnScrollChangedListener(scrollChangeListener)
        
        // 初始检查
        checkExposure()
    }
    
    /**
     * 停止追踪曝光
     */
    fun stopTracking() {
        if (!isTracking) return
        
        isTracking = false
        view.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        view.viewTreeObserver.removeOnScrollChangedListener(scrollChangeListener)
        
        exposureCheckRunnable?.let { handler.removeCallbacks(it) }
        exposureCheckRunnable = null
        
        // 重置曝光状态
        if (isExposed) {
            isExposed = false
            exposureStartTime = 0L
        }
    }
    
    /**
     * 检查视图是否当前处于曝光状态
     */
    private fun checkExposure() {
        if (!isTracking) return
        
        val isCurrentlyVisible = isViewVisible()
        
        if (isCurrentlyVisible && !isExposed) {
            // 开始曝光
            isExposed = true
            exposureStartTime = System.currentTimeMillis()
            
            // 安排曝光持续时间检查
            scheduleExposureCheck()
            
        } else if (!isCurrentlyVisible && isExposed) {
            // 结束曝光
            isExposed = false
            exposureStartTime = 0L
            
            // 取消待定的曝光检查
            exposureCheckRunnable?.let { handler.removeCallbacks(it) }
            exposureCheckRunnable = null
        }
    }
    
    /**
     * 根据曝光配置检查视图是否可见
     */
    private fun isViewVisible(): Boolean {
        if (!view.isShown) return false
        
        val rect = Rect()
        val isVisible = view.getGlobalVisibleRect(rect)
        
        if (!isVisible) return false
        
        val viewHeight = view.height
        val viewWidth = view.width
        val visibleHeight = rect.height()
        val visibleWidth = rect.width()
        
        val visibleArea = visibleHeight * visibleWidth
        val totalArea = viewHeight * viewWidth
        
        if (totalArea == 0) return false
        
        val visibleRatio = visibleArea.toFloat() / totalArea.toFloat()
        
        return visibleRatio >= exposureConfig.minVisibleRatio
    }
    
    /**
     * 安排曝光持续时间检查
     */
    private fun scheduleExposureCheck() {
        exposureCheckRunnable?.let { handler.removeCallbacks(it) }
        
        exposureCheckRunnable = Runnable {
            if (isExposed && isTracking) {
                val exposureDuration = System.currentTimeMillis() - exposureStartTime
                if (exposureDuration >= exposureConfig.minVisibleDuration) {
                    // 触发曝光事件
                    fireExposureEvent()
                }
            }
        }
        
        handler.postDelayed(exposureCheckRunnable!!, exposureConfig.minVisibleDuration)
    }
    
    /**
     * 触发曝光事件
     */
    private fun fireExposureEvent() {
        val exposureData = mutableMapOf<String, Any>().apply {
            put("floor_id", floorId)
            put("exposure_time", System.currentTimeMillis())
            put("exposure_duration", System.currentTimeMillis() - exposureStartTime)
            putAll(exposureConfig.eventParams)
        }
        
        onExposed(floorId, exposureData)
    }
} 