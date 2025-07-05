package com.github.flexfloor.network

import android.content.Context
import com.github.flexfloor.utils.FloorDataMapper
import com.github.flexfloorlib.core.FloorRemoteDataSource
import com.github.flexfloorlib.model.FloorData
import com.google.gson.Gson
import kotlinx.coroutines.delay
import java.io.IOException
import java.io.InputStream

/**
 * 模拟楼层数据源
 * 从assets文件夹中读取JSON数据，模拟网络请求的行为
 */
class MockFloorDataSource(private val context: Context) : FloorRemoteDataSource {

    private val gson = Gson()
    private val networkDelayMs = 5000L // 模拟网络延迟5000ms

    /**
     * 模拟加载楼层配置
     * @param pageId 页面ID
     * @return 楼层配置列表
     */
    override suspend fun loadFloorConfig(pageId: String): List<FloorData>? {
        return try {
            // 模拟网络延迟
            delay(networkDelayMs)

            // 从assets中读取JSON数据
            val jsonString = readJsonFromAssets("floor_demo_data.json")

            // 解析JSON数据
            val floorConfigResponse = gson.fromJson(
                jsonString,
                FloorConfigResponse::class.java
            )

            // 转换为业务实体对象
            if (floorConfigResponse.success && floorConfigResponse.data != null) {
                FloorDataMapper.fromDtoList(floorConfigResponse.data)
            } else {
                emptyList()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 模拟加载楼层业务数据
     * @param floorId 楼层ID
     * @param floorType 楼层类型
     * @param params 请求参数
     * @return 业务数据
     */
    override suspend fun loadFloorData(
        floorId: String,
        floorType: String,
        params: Map<String, Any>
    ): Any? {
        return try {
            // 模拟网络延迟
            delay(networkDelayMs)

            // 这里可以根据楼层ID和类型返回不同的数据
            // 暂时返回一个示例数据
            when (floorType) {
                "banner" -> getBannerData()
                "text" -> getTextData()
                "image" -> getImageData()
                else -> null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 模拟更新楼层配置
     * @param pageId 页面ID
     * @param floorConfig 楼层配置
     * @return 是否更新成功
     */
    override suspend fun updateFloorConfig(
        pageId: String,
        floorConfig: List<FloorData>
    ): Boolean {
        return try {
            // 模拟网络延迟
            delay(networkDelayMs)

            // 模拟更新成功
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从assets文件夹中读取JSON文件
     */
    private fun readJsonFromAssets(fileName: String): String {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 获取Banner数据
     */
    private fun getBannerData(): Map<String, Any> {
        return mapOf(
            "title" to "动态Banner标题",
            "pages" to listOf(
                mapOf(
                    "title" to "动态内容 1",
                    "description" to "这是动态加载的第一个页面",
                    "background_color" to "#FF5722"
                ),
                mapOf(
                    "title" to "动态内容 2",
                    "description" to "这是动态加载的第二个页面",
                    "background_color" to "#9E9E9E"
                )
            )
        )
    }

    /**
     * 获取Text数据
     */
    private fun getTextData(): Map<String, Any> {
        return mapOf(
            "title" to "动态文本标题",
            "content" to "这是动态加载的文本内容，可以根据用户偏好、时间等因素进行个性化展示。",
            "title_color" to "#E91E63",
            "content_color" to "#757575"
        )
    }

    /**
     * 获取Image数据
     */
    private fun getImageData(): Map<String, Any> {
        return mapOf(
            "title" to "动态图片标题",
            "description" to "这是动态加载的图片内容描述。",
            "image_url" to "https://via.placeholder.com/400x200/4CAF50/FFFFFF?text=Dynamic+Image",
            "title_color" to "#4CAF50",
            "description_color" to "#616161"
        )
    }
} 