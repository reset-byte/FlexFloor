package com.github.flexfloorlib.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.flexfloorlib.model.FloorData
import kotlinx.coroutines.launch

/**
 * ViewModel for managing floor data in MVVM architecture
 */
class FloorViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = FloorRepository.getInstance(application)
    
    // LiveData for floor data
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
     * Load floor configuration for a page
     */
    fun loadFloorConfig(pageId: String, useCache: Boolean = true) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""
                
                val floorConfig = repository.loadFloorConfig(pageId, useCache)
                _floorDataList.value = floorConfig
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load floor configuration"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh floor configuration
     */
    fun refreshFloorConfig(pageId: String) {
        loadFloorConfig(pageId, useCache = false)
    }
    
    /**
     * Add a floor to the current list
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
     * Remove a floor from the current list
     */
    fun removeFloor(position: Int) {
        val currentList = _floorDataList.value?.toMutableList() ?: return
        
        if (position in 0 until currentList.size) {
            currentList.removeAt(position)
            _floorDataList.value = currentList
        }
    }
    
    /**
     * Update a floor in the current list
     */
    fun updateFloor(position: Int, floorData: FloorData) {
        val currentList = _floorDataList.value?.toMutableList() ?: return
        
        if (position in 0 until currentList.size) {
            currentList[position] = floorData
            _floorDataList.value = currentList
        }
    }
    
    /**
     * Handle floor click event
     */
    fun onFloorClicked(floorData: FloorData, position: Int) {
        _floorClickEvent.value = Pair(floorData, position)
    }
    
    /**
     * Handle floor exposure event
     */
    fun onFloorExposed(floorId: String, exposureData: Map<String, Any>) {
        _floorExposureEvent.value = Pair(floorId, exposureData)
    }
    
    /**
     * Load floor business data
     */
    fun loadFloorData(floorId: String, floorType: String, params: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            try {
                val data = repository.loadFloorData(floorId, floorType, params)
                // Handle loaded data - could emit through another LiveData if needed
                
            } catch (e: Exception) {
                _error.value = "Failed to load floor data: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Update floor configuration and persist
     */
    fun updateFloorConfig(pageId: String, floorConfig: List<FloorData>) {
        viewModelScope.launch {
            try {
                repository.updateFloorConfig(pageId, floorConfig)
                _floorDataList.value = floorConfig
                
            } catch (e: Exception) {
                _error.value = "Failed to update floor configuration: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Clear cache
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
    
    /**
     * Set data sources
     */
    fun setRemoteDataSource(dataSource: FloorRemoteDataSource) {
        repository.setRemoteDataSource(dataSource)
    }
    
    fun setLocalDataSource(dataSource: FloorLocalDataSource) {
        repository.setLocalDataSource(dataSource)
    }
} 