package com.github.flexfloor.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.flexfloor.R
import com.github.flexfloor.floors.BannerPageData
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.bannerview.BaseBannerAdapter

/**
 * 轮播页面适配器
 * 用于BannerViewPager的页面展示
 */
class BannerPageAdapter : BaseBannerAdapter<BannerPageData>() {
    
    override fun createViewHolder(parent: ViewGroup, itemView: View?, viewType: Int): BaseViewHolder<BannerPageData> {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner_page, parent, false)
        return BannerPageViewHolder(view)
    }
    
    override fun bindData(holder: BaseViewHolder<BannerPageData>, data: BannerPageData?, position: Int, pageSize: Int) {
        (holder as BannerPageViewHolder).bindData(data, position)
    }
    
    override fun getLayoutId(viewType: Int): Int {
        return R.layout.item_banner_page
    }
    
    /**
     * 轮播页面ViewHolder
     */
    class BannerPageViewHolder(itemView: View) : BaseViewHolder<BannerPageData>(itemView) {
        
        private val backgroundView: View = itemView.findViewById(R.id.banner_page_background)
        private val titleView: TextView = itemView.findViewById(R.id.banner_page_title)
        private val descriptionView: TextView = itemView.findViewById(R.id.banner_page_description)
        private val indexView: TextView = itemView.findViewById(R.id.banner_page_index)
        
        fun bindData(pageData: BannerPageData?, position: Int) {
            if (pageData == null) return
            
            // 设置背景颜色
            try {
                backgroundView.setBackgroundColor(Color.parseColor(pageData.backgroundColor))
            } catch (e: IllegalArgumentException) {
                // 颜色解析失败，使用默认颜色
                backgroundView.setBackgroundColor(Color.parseColor("#2196F3"))
            }
            
            // 设置标题
            titleView.text = pageData.title ?: "轮播页面 ${position + 1}"
            
            // 设置描述
            descriptionView.text = pageData.description ?: "这是第${position + 1}个轮播页面"
            
            // 设置序号
            indexView.text = (position + 1).toString()
            
            // 设置可见性
            titleView.visibility = if (pageData.title.isNullOrEmpty()) View.GONE else View.VISIBLE
            descriptionView.visibility = if (pageData.description.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }
} 