package com.github.flexfloorlib.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.flexfloorlib.model.ActionType
import com.github.flexfloorlib.model.JumpAction

/**
 * 处理楼层动作的处理器，如跳转、分享等
 */
object FloorActionHandler {
    
    // 自定义动作处理器
    private var customActionHandler: ((Context, JumpAction) -> Boolean)? = null
    
    /**
     * 设置自定义动作处理器，用于特定应用逻辑
     */
    fun setCustomActionHandler(handler: (Context, JumpAction) -> Boolean) {
        customActionHandler = handler
    }
    
    /**
     * 处理楼层跳转动作
     */
    fun handleAction(context: Context, jumpAction: JumpAction) {
        // 首先尝试使用自定义处理器
        if (customActionHandler?.invoke(context, jumpAction) == true) {
            return
        }
        
        // 默认处理逻辑
        when (jumpAction.actionType) {
            ActionType.WEB -> handleWebAction(context, jumpAction)
            ActionType.NATIVE -> handleNativeAction(context, jumpAction)
            ActionType.SHARE -> handleShareAction(context, jumpAction)
            ActionType.PHONE -> handlePhoneAction(context, jumpAction)
            ActionType.NONE -> {
                // 不执行任何操作
            }
        }
    }
    
    /**
     * 处理网页跳转动作
     */
    private fun handleWebAction(context: Context, jumpAction: JumpAction) {
        jumpAction.url?.let { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 处理原生页面跳转动作
     */
    private fun handleNativeAction(context: Context, jumpAction: JumpAction) {
        jumpAction.url?.let { path ->
            try {
                // 这里可以根据项目需求实现路由跳转
                // 例如：Router.navigate(context, path, jumpAction.params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 处理分享动作
     */
    private fun handleShareAction(context: Context, jumpAction: JumpAction) {
        jumpAction.url?.let { content ->
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, content)
                }
                context.startActivity(Intent.createChooser(intent, "分享到"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 处理电话动作
     */
    private fun handlePhoneAction(context: Context, jumpAction: JumpAction) {
        jumpAction.url?.let { phoneNumber ->
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 