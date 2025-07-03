# FlexFloor - Android 楼层化页面架构

FlexFloor 是一个高性能的 Android 楼层化页面架构解决方案，灵感来源于京东商城的楼层化设计。它提供了模块化、动态配置、强解耦的 UI 架构，特别适用于电商首页、频道页等复杂页面场景。

## 核心特性

### 🏗️ 架构特性
- **Kotlin + MVVM + 协程** - 现代化的技术栈
- **模块化设计** - 每个楼层都是独立的模块
- **动态配置** - 支持服务端配置楼层样式和内容
- **强解耦** - 楼层间无依赖，便于开发和维护

### ⚡ 性能优化
- **楼层复用** - 基于 RecyclerView 的高效视图复用
- **数据缓存** - 多级缓存策略（内存+磁盘）
- **懒加载** - 非可见楼层延迟加载数据
- **差异更新** - DiffUtil 智能更新变化的楼层
- **预加载** - 智能预加载即将显示的楼层
- **楼层池** - 维护常用楼层类型的视图池

### 🔧 功能特性
- **骨架屏支持** - 优雅的加载状态展示
- **局部刷新** - 支持单个楼层的独立刷新
- **Sticky 吸顶** - 楼层吸顶功能
- **配置缓存** - 楼层配置的缓存和预加载
- **埋点监听** - 楼层曝光和点击事件监听

## 快速开始

### 1. 添加依赖

```kotlin
dependencies {
    implementation 'com.github.flexfloorlib:flexfloorlib:1.0.0'
}
```

### 2. 注册楼层类型

```kotlin
// 注册内置楼层类型
FlexFloor.registerFloor(FloorType.BANNER) { BannerFloor() }
FlexFloor.registerFloor(FloorType.GRID) { GridFloor() }
FlexFloor.registerFloor(FloorType.LIST_HORIZONTAL) { HorizontalListFloor() }

// 注册自定义楼层类型
FlexFloor.registerCustomFloor("product_recommendation") { ProductRecommendationFloor() }
```

### 3. 在 Activity/Fragment 中使用

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var floorManager: FloorManager
    private lateinit var viewModel: FloorViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[FloorViewModel::class.java]
        
        // 设置数据源
        viewModel.setRemoteDataSource(MyRemoteDataSource())
        viewModel.setLocalDataSource(MyLocalDataSource())
        
        // 初始化楼层管理器
        floorManager = FlexFloor.with(this, this)
            .enablePreloading(true, 3)
            .enableStickyFloors(true)
            .setupWith(recyclerView)
            .setOnFloorClickListener { floorData, position ->
                // 处理楼层点击
                viewModel.onFloorClicked(floorData, position)
            }
            .setOnFloorExposureListener { floorId, exposureData ->
                // 处理楼层曝光
                viewModel.onFloorExposed(floorId, exposureData)
            }
        
        // 观察数据变化
        observeViewModel()
        
        // 加载楼层配置
        viewModel.loadFloorConfig("home_page")
    }
    
    private fun observeViewModel() {
        viewModel.floorDataList.observe(this) { floorList ->
            floorManager.loadFloors(floorList)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // 显示/隐藏加载状态
        }
        
        viewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                // 显示错误信息
            }
        }
    }
}
```

### 4. 创建自定义楼层

```kotlin
class BannerFloor : BaseFloor<BannerData>() {
    
    override fun getLayoutResId(): Int = R.layout.floor_banner
    
    override fun getFloorType(): String = FloorType.BANNER.typeName
    
    override fun bindView(view: View, position: Int) {
        val bannerView = view.findViewById<BannerView>(R.id.banner_view)
        businessData?.let { data ->
            bannerView.setData(data.bannerList)
        }
    }
    
    override suspend fun loadData(): BannerData? {
        return withContext(Dispatchers.IO) {
            // 从网络或缓存加载数据
            api.loadBannerData(floorData?.floorId ?: "")
        }
    }
    
    override fun onFloorVisible() {
        super.onFloorVisible()
        // 楼层可见时的处理
    }
    
    override fun onFloorInvisible() {
        super.onFloorInvisible()
        // 楼层不可见时的处理
    }
}

data class BannerData(
    val bannerList: List<BannerItem>
)
```

### 5. 配置楼层数据

```kotlin
val floorDataList = listOf(
    FloorData(
        floorId = "banner_001",
        floorType = FloorType.BANNER,
        floorConfig = FloorConfig(
            margin = EdgeInsets(16, 16, 16, 8),
            cornerRadius = 12f,
            backgroundColor = "#FFFFFF",
            clickable = true,
            jumpAction = JumpAction(
                actionType = ActionType.WEB,
                url = "https://example.com"
            )
        ),
        businessData = mapOf("banner_id" to "001"),
        isSticky = false,
        loadPolicy = LoadPolicy.EAGER,
        cachePolicy = CachePolicy.BOTH,
        exposureConfig = ExposureConfig(
            trackOnShow = true,
            minVisibleRatio = 0.5f,
            minVisibleDuration = 500L
        )
    ),
    // 更多楼层...
)
```

## 架构设计

### 核心组件

```
FlexFloor (入口)
├── FloorManager (核心管理器)
├── FloorAdapter (RecyclerView适配器)
├── BaseFloor (楼层基类)
├── FloorFactory (楼层工厂)
├── FloorCacheManager (缓存管理)
├── FloorPreloader (预加载器)
├── StickyFloorHelper (吸顶助手)
├── FloorExposureObserver (曝光监听)
└── FloorViewModel (MVVM ViewModel)
```

### 数据流

```
Remote/Local DataSource
        ↓
FloorRepository (缓存策略)
        ↓
FloorViewModel (MVVM)
        ↓
FloorManager (协调器)
        ↓
FloorAdapter (UI渲染)
        ↓
BaseFloor (具体楼层)
```

## 高级特性

### 缓存策略

FlexFloor 提供了灵活的缓存策略：

```kotlin
enum class CachePolicy {
    NONE,       // 不缓存
    MEMORY,     // 仅内存缓存
    DISK,       // 仅磁盘缓存
    BOTH        // 内存+磁盘缓存
}
```

### 加载策略

支持多种数据加载策略：

```kotlin
enum class LoadPolicy {
    EAGER,      // 立即加载
    LAZY,       // 懒加载（可见时加载）
    PRELOAD     // 预加载（即将可见时加载）
}
```

### 曝光监听

精确的曝光事件监听：

```kotlin
ExposureConfig(
    trackOnShow = true,           // 是否监听显示事件
    trackOnClick = true,          // 是否监听点击事件
    minVisibleRatio = 0.5f,       // 最小可见比例
    minVisibleDuration = 500L,    // 最小可见时长
    eventParams = mapOf(          // 自定义参数
        "page_name" to "home"
    )
)
```

## 性能优化最佳实践

### 1. 楼层复用优化
```kotlin
// 合理设置楼层类型，相同类型的楼层会复用ViewHolder
override fun getFloorType(): String = "banner_${businessData?.type}"
```

### 2. 预加载配置
```kotlin
FlexFloor.with(context, lifecycleOwner)
    .enablePreloading(true, distance = 3) // 预加载距离
```

### 3. 缓存配置
```kotlin
val cacheManager = floorManager.getCacheManager()
cacheManager.configureCacheSettings(
    maxMemorySize = 100,        // 内存缓存最大条目数
    memoryTtlMillis = 30 * 60 * 1000L,  // 内存缓存TTL
    diskTtlMillis = 24 * 60 * 60 * 1000L // 磁盘缓存TTL
)
```

### 4. 差异更新优化
```kotlin
// FloorAdapter 会自动使用 DiffUtil 进行差异更新
floorManager.loadFloors(newFloorList) // 只更新变化的楼层
```

## API 文档

### FlexFloor 主要 API

| 方法 | 说明 |
|------|------|
| `FlexFloor.with(context, lifecycle)` | 创建楼层构建器 |
| `registerFloor(type, creator)` | 注册楼层类型 |
| `registerCustomFloor(name, creator)` | 注册自定义楼层 |

### FloorManager 主要 API

| 方法 | 说明 |
|------|------|
| `loadFloors(floorList)` | 加载楼层列表 |
| `addFloor(floorData, position)` | 添加单个楼层 |
| `removeFloor(position)` | 移除楼层 |
| `updateFloor(position, floorData)` | 更新楼层 |
| `refreshFloors()` | 刷新所有楼层 |

### BaseFloor 生命周期

| 方法 | 说明 |
|------|------|
| `bindView(view, position)` | 绑定视图数据 |
| `loadData()` | 异步加载业务数据 |
| `onFloorVisible()` | 楼层变为可见 |
| `onFloorInvisible()` | 楼层变为不可见 |
| `onFloorClick(view)` | 楼层被点击 |

## 示例项目

查看 [示例项目](./sample) 了解完整的使用方法。

## 更新日志

### v1.0.0
- ✅ 基础楼层架构
- ✅ MVVM + 协程支持
- ✅ 缓存和预加载
- ✅ Sticky 吸顶功能
- ✅ 曝光监听

## 许可证

```
Copyright 2024 FlexFloor

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.