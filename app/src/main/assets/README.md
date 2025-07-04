# FlexFloor 楼层数据架构

## 概述

FlexFloor 现在支持通过 JSON 文件模拟接口请求的方式来加载楼层数据，更加贴合真实的网络请求场景。

## 架构组件

### 1. JSON 数据文件
- **位置**: `app/src/main/assets/floor_demo_data.json`
- **格式**: 标准的 API 响应格式，包含 `success` 和 `data` 字段
- **内容**: 包含轮播楼层、文本楼层、图片楼层等示例数据

### 2. 数据传输对象 (DTO)
- **ApiResponse**: 通用 API 响应封装
- **FloorConfigResponse**: 楼层配置响应
- **FloorDataDto**: 楼层数据传输对象
- **FloorConfigDto**: 楼层配置传输对象
- **EdgeInsetsDto**: 边距配置传输对象

### 3. 数据映射器
- **FloorDataMapper**: 负责将 DTO 对象转换为业务实体对象
- 支持楼层类型、加载策略、缓存策略等的解析

### 4. 模拟数据源
- **MockFloorDataSource**: 实现 `FloorRemoteDataSource` 接口
- 从 assets 文件夹中读取 JSON 数据
- 模拟网络延迟（500ms）
- 支持动态数据加载

### 5. 架构说明
- 使用 **FlexFloorLib** 模块中的 `FloorRepository` 和 `FloorManager`
- 直接在 Activity 中处理数据加载，无额外的 ViewModel 层
- 保持架构简洁，专注于楼层功能演示

## 使用方法

### 1. 初始化数据源
```kotlin
// 创建模拟数据源
mockDataSource = MockFloorDataSource(this)

// 使用 FlexFloorLib 模块的 FloorRepository（单例模式）
floorRepository = FloorRepository.getInstance(this)
floorRepository.setRemoteDataSource(mockDataSource)
```

### 2. 加载楼层数据
```kotlin
lifecycleScope.launch {
    try {
        // 使用模拟数据源从 JSON 文件中加载楼层配置
        val floorList = floorRepository.loadFloorConfig("demo_page", useCache = false)
        
        if (floorList.isNotEmpty()) {
            floorManager.loadFloors(floorList)
        }
    } catch (e: Exception) {
        // 处理错误
    }
}
```

### 3. 自定义楼层数据
要添加新的楼层数据，只需修改 `floor_demo_data.json` 文件：

```json
{
  "success": true,
  "data": [
    {
      "floorId": "your_floor_id",
      "floorType": "text",
      "floorConfig": {
        "margin": { "left": 16, "top": 8, "right": 16, "bottom": 8 },
        "padding": { "left": 16, "top": 16, "right": 16, "bottom": 16 },
        "cornerRadius": 16.0,
        "backgroundColor": "#FFFFFF",
        "elevation": 2.0,
        "clickable": true
      },
      "businessData": {
        "title": "你的标题",
        "content": "你的内容"
      }
    }
  ]
}
```

## 架构优势

1. **更真实的网络请求模拟**: 包含网络延迟、错误处理等
2. **易于维护**: 通过修改JSON文件即可调整楼层数据
3. **可扩展性**: 易于替换为真实的API接口
4. **数据驱动**: 支持动态配置楼层样式和内容
5. **错误处理**: 完善的异常处理和用户反馈机制
6. **架构简洁**: 移除了冗余文件，专注于核心功能演示
7. **模块化**: 清晰的职责分离，lib模块提供核心功能，app模块提供业务实现

## 特性

### 1. 模拟网络行为
- 500ms 网络延迟模拟
- 真实的异步数据加载
- 错误处理和重试机制

### 2. 支持的楼层类型
- **banner**: 轮播图楼层
- **text**: 文本楼层
- **image**: 图片楼层
- **grid**: 网格布局楼层
- **list_horizontal**: 水平列表楼层
- **list_vertical**: 垂直列表楼层
- **card**: 卡片楼层
- **video**: 视频楼层
- **webview**: WebView 楼层
- **custom**: 自定义楼层

### 3. 配置选项
- **loadPolicy**: 加载策略（EAGER, LAZY, ON_DEMAND）
- **cachePolicy**: 缓存策略（NONE, MEMORY, DISK, BOTH）
- **楼层样式**: 边距、内边距、圆角、背景色、阴影等

### 4. 动态数据加载
模拟数据源支持根据楼层类型返回不同的动态数据：
- Banner 数据：动态轮播页面
- Text 数据：个性化文本内容
- Image 数据：动态图片资源

## 扩展

### 1. 替换为真实 API
要替换为真实的网络请求，只需：
1. 实现 `FloorRemoteDataSource` 接口
2. 使用 Retrofit 或其他网络库
3. 在 `initDataSource()` 中设置真实的数据源

### 2. 添加缓存机制
框架已内置缓存支持，可以通过 `CachePolicy` 配置：
- `NONE`: 不缓存
- `MEMORY`: 内存缓存
- `DISK`: 磁盘缓存
- `BOTH`: 内存和磁盘缓存

### 3. 自定义楼层类型
1. 创建自定义楼层类继承 `BaseFloor`
2. 注册到 `FloorFactory`
3. 在 JSON 中配置相应的楼层数据

## 最佳实践

1. **数据验证**: 在 JSON 中添加必要的字段验证
2. **错误处理**: 实现完善的错误处理和用户反馈
3. **性能优化**: 使用适当的缓存策略和懒加载
4. **可扩展性**: 设计灵活的 JSON 结构以支持未来扩展 