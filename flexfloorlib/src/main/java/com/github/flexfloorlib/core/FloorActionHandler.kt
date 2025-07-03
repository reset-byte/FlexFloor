package com.github.flexfloorlib.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.flexfloorlib.model.ActionType
import com.github.flexfloorlib.model.JumpAction

/**
 * Handler for floor actions like jumps, sharing, etc.
 */
object FloorActionHandler {
    
    private var customActionHandler: ((Context, JumpAction) -> Boolean)? = null
    
    /**
     * Set custom action handler for specific app logic
     */
    fun setCustomActionHandler(handler: (Context, JumpAction) -> Boolean) {
        customActionHandler = handler
    }
    
    /**
     * Handle floor jump action
     */
    fun handleAction(context: Context, jumpAction: JumpAction) {
        // Try custom handler first
        if (customActionHandler?.invoke(context, jumpAction) == true) {
            return
        }
        
        // Default handling
        when (jumpAction.actionType) {
            ActionType.WEB -> handleWebAction(context, jumpAction)
            ActionType.NATIVE -> handleNativeAction(context, jumpAction)
            ActionType.SHARE -> handleShareAction(context, jumpAction)
            ActionType.PHONE -> handlePhoneAction(context, jumpAction)
            ActionType.NONE -> {
                // Do nothing
            }
        }
    }
    
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
    
    private fun handleNativeAction(context: Context, jumpAction: JumpAction) {
        jumpAction.url?.let { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun handleShareAction(context: Context, jumpAction: JumpAction) {
        jumpAction.url?.let { content ->
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, content)
                }
                context.startActivity(Intent.createChooser(intent, "Share"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
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