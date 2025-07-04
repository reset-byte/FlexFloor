package com.github.flexfloorlib.core

import com.github.flexfloorlib.model.FloorData
import java.util.concurrent.ConcurrentHashMap

/**
 * 楼层错误分类系统
 * 提供详细的错误类型和处理策略
 */
sealed class FloorError(
    open val errorCode: String,
    open val errorMessage: String,
    open val cause: Throwable? = null,
    open val floorData: FloorData? = null
) {
    
    /**
     * 网络相关错误
     */
    sealed class NetworkError(
        override val errorCode: String,
        override val errorMessage: String,
        override val cause: Throwable? = null,
        override val floorData: FloorData? = null
    ) : FloorError(errorCode, errorMessage, cause, floorData) {
        
        data class ConnectionTimeout(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : NetworkError("NETWORK_001", "网络连接超时", cause, floorData)
        
        data class NoInternetConnection(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : NetworkError("NETWORK_002", "网络连接不可用", cause, floorData)
        
        data class ServerError(
            val httpCode: Int,
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : NetworkError("NETWORK_003", "服务器错误: $httpCode", cause, floorData)
        
        data class RequestFailed(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : NetworkError("NETWORK_004", "请求失败", cause, floorData)
    }
    
    /**
     * 数据解析错误
     */
    sealed class DataParseError(
        override val errorCode: String,
        override val errorMessage: String,
        override val cause: Throwable? = null,
        override val floorData: FloorData? = null
    ) : FloorError(errorCode, errorMessage, cause, floorData) {
        
        data class JsonParseError(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : DataParseError("DATA_001", "JSON数据解析失败", cause, floorData)
        
        data class InvalidDataFormat(
            val expectedFormat: String,
            val actualFormat: String,
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : DataParseError("DATA_002", "数据格式不匹配，期望: $expectedFormat, 实际: $actualFormat", cause, floorData)
        
        data class MissingRequiredField(
            val fieldName: String,
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : DataParseError("DATA_003", "缺少必需字段: $fieldName", cause, floorData)
        
        data class InvalidFieldValue(
            val fieldName: String,
            val value: Any?,
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : DataParseError("DATA_004", "字段值无效: $fieldName = $value", cause, floorData)
    }
    
    /**
     * 缓存相关错误
     */
    sealed class CacheError(
        override val errorCode: String,
        override val errorMessage: String,
        override val cause: Throwable? = null,
        override val floorData: FloorData? = null
    ) : FloorError(errorCode, errorMessage, cause, floorData) {
        
        data class CacheWriteError(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : CacheError("CACHE_001", "缓存写入失败", cause, floorData)
        
        data class CacheReadError(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : CacheError("CACHE_002", "缓存读取失败", cause, floorData)
        
        data class CacheExpired(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : CacheError("CACHE_003", "缓存已过期", cause, floorData)
        
        data class InsufficientStorage(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : CacheError("CACHE_004", "存储空间不足", cause, floorData)
    }
    
    /**
     * 楼层创建错误
     */
    sealed class FloorCreationError(
        override val errorCode: String,
        override val errorMessage: String,
        override val cause: Throwable? = null,
        override val floorData: FloorData? = null
    ) : FloorError(errorCode, errorMessage, cause, floorData) {
        
        data class FloorTypeNotRegistered(
            val floorType: String,
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : FloorCreationError("FLOOR_001", "楼层类型未注册: $floorType", cause, floorData)
        
        data class FloorInstantiationFailed(
            val floorType: String,
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : FloorCreationError("FLOOR_002", "楼层实例化失败: $floorType", cause, floorData)
        
        data class FloorInitializationFailed(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : FloorCreationError("FLOOR_003", "楼层初始化失败", cause, floorData)
        
        data class InvalidFloorConfig(
            val configKey: String,
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : FloorCreationError("FLOOR_004", "楼层配置无效: $configKey", cause, floorData)
    }
    
    /**
     * 资源相关错误
     */
    sealed class ResourceError(
        override val errorCode: String,
        override val errorMessage: String,
        override val cause: Throwable? = null,
        override val floorData: FloorData? = null
    ) : FloorError(errorCode, errorMessage, cause, floorData) {
        
        data class LayoutResourceNotFound(
            val resourceId: Int,
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : ResourceError("RESOURCE_001", "布局资源未找到: $resourceId", cause, floorData)
        
        data class ImageLoadFailed(
            val imageUrl: String,
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : ResourceError("RESOURCE_002", "图片加载失败: $imageUrl", cause, floorData)
        
        data class OutOfMemoryError(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : ResourceError("RESOURCE_003", "内存不足", cause, floorData)
    }
    
    /**
     * 生命周期相关错误
     */
    sealed class LifecycleError(
        override val errorCode: String,
        override val errorMessage: String,
        override val cause: Throwable? = null,
        override val floorData: FloorData? = null
    ) : FloorError(errorCode, errorMessage, cause, floorData) {
        
        data class ManagerDestroyed(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : LifecycleError("LIFECYCLE_001", "FloorManager已销毁", cause, floorData)
        
        data class ViewDetached(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : LifecycleError("LIFECYCLE_002", "视图已分离", cause, floorData)
        
        data class ContextLost(
            override val cause: Throwable? = null,
            override val floorData: FloorData? = null
        ) : LifecycleError("LIFECYCLE_003", "上下文丢失", cause, floorData)
    }
    
    /**
     * 自定义错误
     */
    data class CustomError(
        override val errorCode: String,
        override val errorMessage: String,
        override val cause: Throwable? = null,
        override val floorData: FloorData? = null
    ) : FloorError(errorCode, errorMessage, cause, floorData)
}

/**
 * 错误处理策略
 */
enum class ErrorHandlingStrategy {
    IGNORE,         // 忽略错误，继续执行
    RETRY,          // 重试操作
    FALLBACK,       // 使用降级方案
    FAIL_FAST,      // 快速失败
    NOTIFY_USER     // 通知用户
}

/**
 * 错误恢复动作
 */
sealed class ErrorRecoveryAction {
    object None : ErrorRecoveryAction()
    data class Retry(val maxRetries: Int = 3, val delayMs: Long = 1000) : ErrorRecoveryAction()
    data class Fallback(val fallbackAction: suspend () -> Unit) : ErrorRecoveryAction()
    data class ShowErrorView(val errorMessage: String) : ErrorRecoveryAction()
    data class Custom(val action: suspend (FloorError) -> Unit) : ErrorRecoveryAction()
}

/**
 * 错误处理器
 */
class FloorErrorHandler {
    
    private val errorStrategies = ConcurrentHashMap<String, ErrorHandlingStrategy>()
    private val recoveryActions = ConcurrentHashMap<String, ErrorRecoveryAction>()
    private val errorListeners = mutableListOf<(FloorError) -> Unit>()
    private val errorStats = ConcurrentHashMap<String, Int>()
    
    /**
     * 设置错误处理策略
     */
    fun setErrorStrategy(errorCode: String, strategy: ErrorHandlingStrategy) {
        errorStrategies[errorCode] = strategy
    }
    
    /**
     * 设置错误恢复动作
     */
    fun setRecoveryAction(errorCode: String, action: ErrorRecoveryAction) {
        recoveryActions[errorCode] = action
    }
    
    /**
     * 添加错误监听器
     */
    fun addErrorListener(listener: (FloorError) -> Unit) {
        errorListeners.add(listener)
    }
    
    /**
     * 移除错误监听器
     */
    fun removeErrorListener(listener: (FloorError) -> Unit) {
        errorListeners.remove(listener)
    }
    
    /**
     * 处理错误
     */
    suspend fun handleError(error: FloorError): Boolean {
        // 更新错误统计
        errorStats[error.errorCode] = (errorStats[error.errorCode] ?: 0) + 1
        
        // 通知所有监听器
        errorListeners.forEach { listener ->
            try {
                listener(error)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 获取处理策略
        val strategy = errorStrategies[error.errorCode] ?: getDefaultStrategy(error)
        
        // 执行恢复动作
        return when (strategy) {
            ErrorHandlingStrategy.IGNORE -> true
            ErrorHandlingStrategy.RETRY -> executeRetry(error)
            ErrorHandlingStrategy.FALLBACK -> executeFallback(error)
            ErrorHandlingStrategy.FAIL_FAST -> false
            ErrorHandlingStrategy.NOTIFY_USER -> {
                notifyUser(error)
                true
            }
        }
    }
    
    /**
     * 执行重试
     */
    private suspend fun executeRetry(error: FloorError): Boolean {
        val recoveryAction = recoveryActions[error.errorCode] as? ErrorRecoveryAction.Retry
            ?: ErrorRecoveryAction.Retry()
        
        // 重试逻辑将在调用处实现
        return true
    }
    
    /**
     * 执行降级方案
     */
    private suspend fun executeFallback(error: FloorError): Boolean {
        val recoveryAction = recoveryActions[error.errorCode] as? ErrorRecoveryAction.Fallback
            ?: return false
        
        try {
            recoveryAction.fallbackAction()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * 通知用户
     */
    private fun notifyUser(error: FloorError) {
        val recoveryAction = recoveryActions[error.errorCode] as? ErrorRecoveryAction.ShowErrorView
        // 具体的用户通知逻辑将在UI层实现
    }
    
    /**
     * 获取默认策略
     */
    private fun getDefaultStrategy(error: FloorError): ErrorHandlingStrategy {
        return when (error) {
            is FloorError.NetworkError -> ErrorHandlingStrategy.RETRY
            is FloorError.DataParseError -> ErrorHandlingStrategy.FALLBACK
            is FloorError.CacheError -> ErrorHandlingStrategy.IGNORE
            is FloorError.FloorCreationError -> ErrorHandlingStrategy.FALLBACK
            is FloorError.ResourceError -> ErrorHandlingStrategy.FALLBACK
            is FloorError.LifecycleError -> ErrorHandlingStrategy.FAIL_FAST
            is FloorError.CustomError -> ErrorHandlingStrategy.NOTIFY_USER
        }
    }
    
    /**
     * 获取错误统计
     */
    fun getErrorStats(): Map<String, Int> = errorStats.toMap()
    
    /**
     * 清除错误统计
     */
    fun clearErrorStats() {
        errorStats.clear()
    }
    
    /**
     * 重置错误处理器
     */
    fun reset() {
        errorStrategies.clear()
        recoveryActions.clear()
        errorListeners.clear()
        errorStats.clear()
    }
}

/**
 * 错误工具类
 */
object FloorErrorUtils {
    
    /**
     * 将异常转换为FloorError
     */
    fun convertThrowableToFloorError(throwable: Throwable, floorData: FloorData? = null): FloorError {
        return when (throwable) {
            is java.net.SocketTimeoutException -> FloorError.NetworkError.ConnectionTimeout(throwable, floorData)
            is java.net.ConnectException -> FloorError.NetworkError.NoInternetConnection(throwable, floorData)
            is java.net.UnknownHostException -> FloorError.NetworkError.NoInternetConnection(throwable, floorData)
            is org.json.JSONException -> FloorError.DataParseError.JsonParseError(throwable, floorData)
            is java.lang.OutOfMemoryError -> FloorError.ResourceError.OutOfMemoryError(throwable, floorData)
            is java.lang.ClassNotFoundException -> FloorError.FloorCreationError.FloorTypeNotRegistered(
                throwable.message ?: "未知类型", throwable, floorData
            )
            else -> FloorError.CustomError(
                "UNKNOWN_ERROR",
                throwable.message ?: "未知错误",
                throwable,
                floorData
            )
        }
    }
    
    /**
     * 检查错误是否可恢复
     */
    fun isRecoverableError(error: FloorError): Boolean {
        return when (error) {
            is FloorError.NetworkError -> true
            is FloorError.CacheError -> true
            is FloorError.DataParseError.JsonParseError -> true
            is FloorError.ResourceError.ImageLoadFailed -> true
            is FloorError.LifecycleError.ManagerDestroyed -> false
            is FloorError.ResourceError.OutOfMemoryError -> false
            else -> true
        }
    }
    
    /**
     * 获取错误的用户友好消息
     */
    fun getUserFriendlyMessage(error: FloorError): String {
        return when (error) {
            is FloorError.NetworkError -> "网络连接异常，请检查网络设置"
            is FloorError.DataParseError -> "数据格式异常，请稍后重试"
            is FloorError.CacheError -> "缓存异常，请清除缓存后重试"
            is FloorError.FloorCreationError -> "页面组件加载异常"
            is FloorError.ResourceError -> "资源加载异常，请稍后重试"
            is FloorError.LifecycleError -> "页面状态异常，请重新进入"
            is FloorError.CustomError -> error.errorMessage
        }
    }
    
    /**
     * 显示默认错误UI
     */
    fun showDefaultErrorUI(context: android.content.Context, error: FloorError) {
        val message = getUserFriendlyMessage(error)
        val isRecoverable = isRecoverableError(error)
        
        try {
            // 尝试找到Activity
            val activity = context as? androidx.activity.ComponentActivity
                ?: (context as? android.app.Activity)
                
            if (activity != null) {
                // 如果是Activity，显示Snackbar
                val rootView = activity.findViewById<android.view.View>(android.R.id.content)
                if (rootView != null) {
                    if (isRecoverable) {
                        com.google.android.material.snackbar.Snackbar.make(
                            rootView, 
                            message, 
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        com.google.android.material.snackbar.Snackbar.make(
                            rootView, 
                            message, 
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    return
                }
            }
        } catch (e: Exception) {
            // 如果Snackbar失败，回退到Toast
        }
        
        // 回退到Toast
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }
} 