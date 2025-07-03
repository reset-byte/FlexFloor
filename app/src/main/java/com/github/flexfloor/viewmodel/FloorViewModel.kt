package com.github.flexfloor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.flexfloor.network.FloorApiService
import com.github.flexfloor.repository.FloorRepository
import com.github.flexfloorlib.model.FloorData
import com.github.flexfloorlib.model.ActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 楼层ViewModel
 * 管理楼层数据和UI状态
 */
class FloorViewModel(
    private val repository: FloorRepository,
    private val apiService: FloorApiService
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(FloorUiState())
    val uiState: StateFlow<FloorUiState> = _uiState.asStateFlow()
    
    // 楼层数据
    private val _floorData = MutableStateFlow<List<FloorData>>(emptyList())
    val floorData: StateFlow<List<FloorData>> = _floorData.asStateFlow()
    
    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * 加载楼层配置
     */
    fun loadFloorConfig(pageId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                repository.loadFloorConfig(pageId).collect { floors ->
                    _floorData.value = floors
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEmpty = floors.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _errorMessage.value = "加载楼层配置失败: ${e.message}"
            }
        }
    }
    
    /**
     * 刷新楼层数据
     */
    fun refreshFloors(pageId: String) {
        viewModelScope.launch {
            try {
                repository.refreshFloors(pageId).collect { floors ->
                    _floorData.value = floors
                    _uiState.value = _uiState.value.copy(isEmpty = floors.isEmpty())
                }
            } catch (e: Exception) {
                _errorMessage.value = "刷新楼层数据失败: ${e.message}"
            }
        }
    }
    
    /**
     * 添加楼层
     */
    fun addFloor(floorData: FloorData) {
        val currentFloors = _floorData.value.toMutableList()
        currentFloors.add(floorData)
        _floorData.value = currentFloors
        _uiState.value = _uiState.value.copy(isEmpty = false)
    }
    
    /**
     * 处理楼层点击事件
     */
    fun handleFloorClick(floorData: FloorData, position: Int) {
        floorData.floorConfig.jumpAction?.let { jumpAction ->
            val navigationEvent = NavigationEvent(
                actionType = jumpAction.actionType.name,
                actionUrl = jumpAction.url
            )
            _uiState.value = _uiState.value.copy(navigationEvent = navigationEvent)
        }
    }
    
    /**
     * 处理楼层曝光事件
     */
    fun handleFloorExposure(floorData: FloorData, position: Int, exposureRatio: Float) {
        viewModelScope.launch {
            try {
                val exposureData = com.github.flexfloor.network.FloorExposureData(
                    floorId = floorData.floorId,
                    pageId = "home_page",
                    position = position,
                    exposureRatio = exposureRatio,
                    exposureTime = System.currentTimeMillis(),
                    userId = null,
                    sessionId = "session_${System.currentTimeMillis()}"
                )
                apiService.reportFloorExposure(exposureData)
            } catch (e: Exception) {
                // 忽略曝光上报失败
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 清除导航事件
     */
    fun clearNavigationEvent() {
        _uiState.value = _uiState.value.copy(navigationEvent = null)
    }
}

/**
 * UI状态数据类
 */
data class FloorUiState(
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val navigationEvent: NavigationEvent? = null
)

/**
 * 导航事件数据类
 */
data class NavigationEvent(
    val actionType: String,
    val actionUrl: String?
)

/**
 * FloorViewModel工厂类
 */
class FloorViewModelFactory(
    private val repository: FloorRepository,
    private val apiService: FloorApiService
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FloorViewModel::class.java)) {
            return FloorViewModel(repository, apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 