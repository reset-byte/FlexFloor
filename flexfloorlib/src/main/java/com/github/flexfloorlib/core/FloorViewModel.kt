package com.github.flexfloorlib.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.github.flexfloorlib.model.FloorData
import kotlinx.coroutines.launch

/**
 * 楼层数据管理的 ViewModel，用于 MVVM 架构
 * 支持依赖注入和多种创建方式
 */
class FloorViewModel(
    application: Application,
    private val repository: FloorRepository
) : AndroidViewModel(application) {
    
    // 楼层数据的 LiveData
    private val _floorDataList = MutableLiveData<List<FloorData>>()
    val floorDataList: LiveData<List<FloorData>> = _floorDataList
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _floorClickEvent = MutableLiveData<Pair<FloorData, Int>>()
    val floorClickEvent: LiveData<Pair<FloorData, Int>> = _floorClickEvent
    
    private val _floorExposureEvent = MutableLiveData<Pair<String, Map<String, Any>>>()
    val floorExposureEvent: LiveData<Pair<String, Map<String, Any>>> = _floorExposureEvent
    
    /**
     * 便利构造函数，使用默认的Repository
     */
    constructor(application: Application) : this(
        application,
        FloorRepository.getInstance(application)
    )
    
    /**
     * 加载页面的楼层配置
     */
    fun loadFloorConfig(pageId: String, useCache: Boolean = true) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""
                
                val floorConfig = repository.loadFloorConfig(pageId, useCache)
                _floorDataList.value = floorConfig
                
            } catch (e: Exception) {
                _error.value = e.message ?: "加载楼层配置失败"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 刷新楼层配置
     */
    fun refreshFloorConfig(pageId: String) {
        loadFloorConfig(pageId, useCache = false)
    }
    
    /**
     * 添加楼层到当前列表
     */
    fun addFloor(floorData: FloorData, position: Int = -1) {
        val currentList = _floorDataList.value?.toMutableList() ?: mutableListOf()
        
        if (position >= 0 && position <= currentList.size) {
            currentList.add(position, floorData)
        } else {
            currentList.add(floorData)
        }
        
        _floorDataList.value = currentList
    }
    
    /**
     * 从当前列表移除楼层
     */
    fun removeFloor(position: Int) {
        val currentList = _floorDataList.value?.toMutableList() ?: return
        
        if (position in 0 until currentList.size) {
            currentList.removeAt(position)
            _floorDataList.value = currentList
        }
    }
    
    /**
     * 更新当前列表中的楼层
     */
    fun updateFloor(position: Int, floorData: FloorData) {
        val currentList = _floorDataList.value?.toMutableList() ?: return
        
        if (position in 0 until currentList.size) {
            currentList[position] = floorData
            _floorDataList.value = currentList
        }
    }
    
    /**
     * 处理楼层点击事件
     */
    fun onFloorClicked(floorData: FloorData, position: Int) {
        _floorClickEvent.value = Pair(floorData, position)
    }
    
    /**
     * 处理楼层曝光事件
     */
    fun onFloorExposed(floorId: String, exposureData: Map<String, Any>) {
        _floorExposureEvent.value = Pair(floorId, exposureData)
    }
    
    /**
     * 加载楼层业务数据
     */
    fun loadFloorData(floorId: String, floorType: String, params: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            try {
                val data = repository.loadFloorData(floorId, floorType, params)
                // 处理加载的数据 - 如果需要，可以通过另一个 LiveData 发出
                
            } catch (e: Exception) {
                _error.value = "加载楼层数据失败: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 更新楼层配置并持久化
     */
    fun updateFloorConfig(pageId: String, floorConfig: List<FloorData>) {
        viewModelScope.launch {
            try {
                repository.updateFloorConfig(pageId, floorConfig)
                _floorDataList.value = floorConfig
                
            } catch (e: Exception) {
                _error.value = "更新楼层配置失败: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                repository.clearAllCache()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * FloorViewModel 工厂类，支持依赖注入
 */
class FloorViewModelFactory(
    private val application: Application,
    private val repository: FloorRepository? = null
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FloorViewModel::class.java)) {
            val repo = repository ?: FloorRepository.getInstance(application)
            return FloorViewModel(application, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * FloorRepository 构建器，用于配置数据源
 */
class FloorRepositoryBuilder(private val application: Application) {
    private var remoteDataSource: FloorRemoteDataSource? = null
    
    fun setRemoteDataSource(dataSource: FloorRemoteDataSource): FloorRepositoryBuilder {
        this.remoteDataSource = dataSource
        return this
    }
    
    fun build(): FloorRepository {
        val repository = FloorRepository.getInstance(application)
        remoteDataSource?.let { repository.setRemoteDataSource(it) }
        return repository
    }
}

/**
 * 楼层架构初始化器
 */
object FloorArchitecture {
    
    /**
     * 创建配置了数据源的 FloorViewModel
     */
    fun createViewModel(
        application: Application,
        remoteDataSource: FloorRemoteDataSource
    ): FloorViewModel {
        val repository = FloorRepositoryBuilder(application)
            .setRemoteDataSource(remoteDataSource)
            .build()
        return FloorViewModel(application, repository)
    }
    
    /**
     * 创建 FloorViewModel 工厂
     */
    fun createViewModelFactory(
        application: Application,
        remoteDataSource: FloorRemoteDataSource
    ): FloorViewModelFactory {
        val repository = FloorRepositoryBuilder(application)
            .setRemoteDataSource(remoteDataSource)
            .build()
        return FloorViewModelFactory(application, repository)
    }
} 