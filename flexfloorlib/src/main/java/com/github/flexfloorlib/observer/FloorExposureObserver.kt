package com.github.flexfloorlib.observer

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import com.github.flexfloorlib.model.ExposureConfig

/**
 * Observer for tracking floor exposure and visibility
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
     * Start tracking exposure
     */
    fun startTracking() {
        if (isTracking) return
        
        isTracking = true
        view.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        view.viewTreeObserver.addOnScrollChangedListener(scrollChangeListener)
        
        // Initial check
        checkExposure()
    }
    
    /**
     * Stop tracking exposure
     */
    fun stopTracking() {
        if (!isTracking) return
        
        isTracking = false
        view.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        view.viewTreeObserver.removeOnScrollChangedListener(scrollChangeListener)
        
        exposureCheckRunnable?.let { handler.removeCallbacks(it) }
        exposureCheckRunnable = null
        
        // Reset exposure state
        if (isExposed) {
            isExposed = false
            exposureStartTime = 0L
        }
    }
    
    /**
     * Check if view is currently exposed
     */
    private fun checkExposure() {
        if (!isTracking) return
        
        val isCurrentlyVisible = isViewVisible()
        
        if (isCurrentlyVisible && !isExposed) {
            // Start exposure
            isExposed = true
            exposureStartTime = System.currentTimeMillis()
            
            // Schedule exposure duration check
            scheduleExposureCheck()
            
        } else if (!isCurrentlyVisible && isExposed) {
            // End exposure
            isExposed = false
            exposureStartTime = 0L
            
            // Cancel pending exposure check
            exposureCheckRunnable?.let { handler.removeCallbacks(it) }
            exposureCheckRunnable = null
        }
    }
    
    /**
     * Check if view is visible according to exposure config
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
     * Schedule exposure duration check
     */
    private fun scheduleExposureCheck() {
        exposureCheckRunnable?.let { handler.removeCallbacks(it) }
        
        exposureCheckRunnable = Runnable {
            if (isExposed && isTracking) {
                val exposureDuration = System.currentTimeMillis() - exposureStartTime
                if (exposureDuration >= exposureConfig.minVisibleDuration) {
                    // Fire exposure event
                    fireExposureEvent()
                }
            }
        }
        
        handler.postDelayed(exposureCheckRunnable!!, exposureConfig.minVisibleDuration)
    }
    
    /**
     * Fire exposure event
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