# 骨架屏使用说明

## 概述
FlexFloor框架支持骨架屏功能，但**不预设任何骨架屏样式**。所有骨架屏都需要用户根据自己的楼层设计来创建和注册。

## 为什么这样设计？
1. **个性化需求**：每个应用的楼层设计都不同，预设的骨架屏无法满足所有场景
2. **设计一致性**：用户最了解自己的设计语言，应该由用户来定义骨架屏样式
3. **灵活性**：用户可以根据楼层数据动态调整骨架屏样式

## 如何使用

### 1. 创建骨架屏布局文件
为每种楼层类型创建对应的骨架屏布局文件，例如：
- `skeleton_floor_text.xml` - 文本楼层骨架屏
- `skeleton_floor_image.xml` - 图片楼层骨架屏
- `skeleton_floor_banner.xml` - 轮播图楼层骨架屏

### 2. 注册骨架屏
在应用启动时注册骨架屏：

```kotlin
// 方法1：使用布局资源注册
FlexFloor.registerSkeletonLayout(FloorType.TEXT, R.layout.skeleton_floor_text)
FlexFloor.registerSkeletonLayout(FloorType.IMAGE, R.layout.skeleton_floor_image)

// 方法2：使用自定义创建器注册（适用于复杂场景）
FlexFloor.registerSkeletonCreator(FloorType.TEXT) { context, floorData ->
    val skeletonView = LayoutInflater.from(context)
        .inflate(R.layout.skeleton_floor_text, null)
    
    // 可以根据floorData进行定制
    // 例如根据数据设置不同的高度、颜色等
    
    skeletonView
}
```

### 3. 启用骨架屏
在FloorManager中启用骨架屏功能：

```kotlin
floorManager = FloorManager.create(this)
    .setupWithRecyclerView(recyclerView)
    .enableSkeletonScreen(true)  // 启用骨架屏
```

## 示例文件说明
本目录下的骨架屏布局文件（skeleton_floor_*.xml）仅作为示例参考，展示如何创建骨架屏布局。

实际使用时，请根据自己的楼层设计创建相应的骨架屏布局文件。

## 动画效果
骨架屏支持闪烁动画效果，可以使用 `skeleton_shimmer.xml` 作为背景来实现。

## 注意事项
1. 只有注册了骨架屏的楼层类型才会显示骨架屏
2. 如果没有注册骨架屏，楼层加载时不会显示任何loading状态
3. 用户可以重写 `BaseFloor.showLoadingState()` 方法来自定义没有骨架屏时的加载状态 