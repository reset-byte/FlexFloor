# FlexFloor

一个灵活的 Android 楼层化框架，采用 MVVM 架构，支持动态配置、缓存管理、错误处理和多种楼层类型。

## 核心特性

- **楼层化架构**：基于楼层概念的页面组织方式
- **MVVM 架构**：清晰的数据流和状态管理
- **动态配置**：支持运行时添加、删除、更新楼层
- **智能缓存**：多级缓存策略，提升性能
- **错误处理**：完善的错误处理和恢复机制
- **多种楼层类型**：文本、图片、轮播等内置楼层类型
- **易于扩展**：简单的楼层注册和自定义机制
- **依赖注入**：支持自定义数据源的灵活注入

## 快速开始

### 1. 实现数据源

首先，用户需要实现自己的数据源，可以使用 Retrofit、OkHttp 等网络框架：

```kotlin
class YourFloorDataSource(
    private val apiService: YourApiService,
    private val authManager: AuthManager
) : FloorRemoteDataSource {
    
    override suspend fun loadFloorConfig(pageId: String): List<FloorData>? {
        return try {
            // 使用你的网络框架实现
            val response = apiService.getFloorConfig(
                pageId = pageId,
                token = authManager.getToken()
            )
            
            if (response.isSuccessful) {
                // 转换为 FloorData 格式
                convertToFloorData(response.body())
            } else {
                null
            }
        } catch (e: Exception) {
            // 处理网络异常
            null
        }
    }
    
    override suspend fun loadFloorData(
        floorId: String, 
        floorType: String, 
        params: Map<String, Any>
    ): Any? {
        return try {
            val response = apiService.getFloorBusinessData(
                floorId = floorId,
                params = params,
                token = authManager.getToken()
            )
            response.body()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun updateFloorConfig(
        pageId: String, 
        floorConfig: List<FloorData>
    ): Boolean {
        return try {
            val response = apiService.updateFloorConfig(pageId, floorConfig)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
```

### 2. 在Activity中使用

```kotlin
class YourActivity : ComponentActivity() {
    
    private lateinit var floorManager: FloorManager
    private lateinit var viewModel: FloorViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. 创建你的数据源（在应用层管理）
        val dataSource = YourFloorDataSource(
            apiService = RetrofitClient.apiService,
            authManager = AuthManager.getInstance()
        )
        
        // 2. 使用依赖注入创建 ViewModel（推荐方式）
        viewModel = FloorArchitecture.createViewModel(application, dataSource)
        
        // 3. 注册楼层类型
        FloorFactory.registerFloor(FloorType.TEXT) { TextFloor() }
        FloorFactory.registerFloor(FloorType.IMAGE) { ImageFloor() }
        FloorFactory.registerFloor(FloorType.BANNER) { BannerFloor() }
        
        // 4. 配置 FloorManager（专注于楼层展示）
        floorManager = FloorManager.create(this)
            .setupWithRecyclerView(recyclerView)
            .enablePreloading(true, 5)
            .enableStickyFloors(true)
            .enableAutoErrorHandling(true)
            .setOnFloorClickListener { floorData, position ->
                viewModel.onFloorClicked(floorData, position)
            }
            .setOnFloorExposureListener { floorId, exposureData ->
                viewModel.onFloorExposed(floorId, exposureData)
            }
            .configureErrorHandling {
                // 配置错误处理策略
                onNetworkError(
                    ErrorHandlingStrategy.RETRY,
                    ErrorRecoveryAction.Retry(maxRetries = 3)
                )
                onDataParseError(
                    ErrorHandlingStrategy.FALLBACK,
                    ErrorRecoveryAction.Fallback {
                        showToast("数据解析失败，使用缓存数据")
                    }
                )
            }
        
        // 5. 观察数据变化
        observeViewModel()
        
        // 6. 加载楼层数据
        viewModel.loadFloorConfig("your_page_id")
    }
    
    private fun observeViewModel() {
        viewModel.floorDataList.observe(this) { floorList ->
            floorManager.loadFloors(floorList)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // 处理加载状态
        }
        
        viewModel.error.observe(this) { error ->
            // 处理错误
        }
    }
}
```

### 3. 可选的使用方式

#### 使用 ViewModelProvider 和 Factory

```kotlin
// 如果你习惯使用传统的 ViewModelProvider
val factory = FloorArchitecture.createViewModelFactory(application, dataSource)
viewModel = ViewModelProvider(this, factory)[FloorViewModel::class.java]
```

#### 使用仓库构建器

```kotlin
// 如果需要更复杂的配置
val repository = FloorRepositoryBuilder(application)
    .setRemoteDataSource(dataSource)
    .build()

val factory = FloorViewModelFactory(application, repository)
viewModel = ViewModelProvider(this, factory)[FloorViewModel::class.java]
```

## 架构设计

### 为什么这样设计？

1. **数据源在应用层管理**：
   - 用户可以使用任何网络框架（Retrofit、OkHttp、Volley等）
   - 方便处理认证、头部信息、错误重试等业务逻辑
   - 支持复杂的网络配置和拦截器

2. **依赖注入模式**：
   - 明确的依赖关系，便于测试
   - 支持模拟数据源进行单元测试
   - 符合 SOLID 原则

3. **职责分离清晰**：
   - `FloorManager`: 专注于楼层展示和交互
   - `FloorViewModel`: 专注于数据状态管理
   - `FloorRepository`: 负责数据获取和缓存
   - `DataSource`: 负责具体的网络实现

### 架构优势

| 组件 | 职责 | 优势 |
|------|------|------|
| **YourDataSource** | 网络请求实现 | 用户完全控制，支持任何网络框架 |
| **FloorArchitecture** | 依赖注入配置 | 简化初始化，明确依赖关系 |
| **FloorViewModel** | 数据状态管理 | 标准 MVVM，易于测试 |
| **FloorManager** | 楼层展示管理 | 专注UI，链式配置 |

### 数据流向

```
YourActivity
    ↓ 创建
YourDataSource (用户实现)
    ↓ 注入
FloorRepository
    ↓ 注入
FloorViewModel
    ↓ 数据流
FloorManager → RecyclerView
```

## 实战示例

### 使用 Retrofit 的完整示例

```kotlin
// 1. 定义API接口
interface FloorApiService {
    @GET("api/floors/{pageId}")
    suspend fun getFloorConfig(@Path("pageId") pageId: String): Response<FloorConfigResponse>
    
    @POST("api/floors/{floorId}/data")
    suspend fun getFloorData(@Path("floorId") floorId: String): Response<Any>
}

// 2. 实现数据源
class RetrofitFloorDataSource(
    private val apiService: FloorApiService
) : FloorRemoteDataSource {
    
    override suspend fun loadFloorConfig(pageId: String): List<FloorData>? {
        return try {
            val response = apiService.getFloorConfig(pageId)
            if (response.isSuccessful) {
                response.body()?.data?.let { FloorDataMapper.fromDtoList(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // ... 其他方法实现
}

// 3. 在Activity中使用
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://your-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val apiService = retrofit.create(FloorApiService::class.java)
        val dataSource = RetrofitFloorDataSource(apiService)
        
        // 使用架构初始化器
        val viewModel = FloorArchitecture.createViewModel(application, dataSource)
        
        // ... 其余配置
    }
}
```

### 错误处理示例

```kotlin
floorManager.configureErrorHandling {
    // 网络错误：重试3次
    onNetworkError(
        ErrorHandlingStrategy.RETRY,
        ErrorRecoveryAction.Retry(maxRetries = 3, delayMs = 2000)
    )
    
    // 认证错误：跳转登录
    onErrorCode("AUTH_401", 
        ErrorHandlingStrategy.NOTIFY_USER,
        ErrorRecoveryAction.Custom { error ->
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        }
    )
    
    // 数据解析错误：使用缓存
    onDataParseError(
        ErrorHandlingStrategy.FALLBACK,
        ErrorRecoveryAction.Fallback {
            loadFromCache()
        }
    )
}
```

## 最佳实践

### 1. 数据源设计

```kotlin
// ✅ 推荐：在应用层创建和管理数据源
class YourDataSource : FloorRemoteDataSource {
    // 用户完全控制网络实现
}

// ❌ 不推荐：在框架内部设置数据源
// floorManager.setRemoteDataSource(dataSource) // 已移除
```

### 2. 依赖注入

```kotlin
// ✅ 推荐：使用架构初始化器
val viewModel = FloorArchitecture.createViewModel(application, dataSource)

// ✅ 可选：使用 ViewModelFactory
val factory = FloorArchitecture.createViewModelFactory(application, dataSource)
val viewModel = ViewModelProvider(this, factory)[FloorViewModel::class.java]

// ❌ 避免：混合多种方式
```

### 3. 错误处理

```kotlin
// ✅ 推荐：配置详细的错误处理策略
floorManager.configureErrorHandling {
    onNetworkError(ErrorHandlingStrategy.RETRY, ...)
    onDataParseError(ErrorHandlingStrategy.FALLBACK, ...)
}

// ✅ 推荐：启用自动错误处理
floorManager.enableAutoErrorHandling(true)
```

## 项目结构

```
FlexFloor/
├── app/                          # 示例应用
│   ├── src/main/java/com/github/flexfloor/
│   │   ├── FloorDemoActivity.kt  # 使用示例
│   │   ├── floors/               # 楼层实现
│   │   └── network/              # 数据源实现
│   └── src/main/assets/          # 示例数据
└── flexfloorlib/                 # 核心库
    └── src/main/java/com/github/flexfloorlib/
        ├── core/                 # 核心组件
        ├── adapter/              # 适配器
        ├── cache/                # 缓存管理
        ├── model/                # 数据模型
        └── utils/                # 工具类
```

## 许可证

MIT License