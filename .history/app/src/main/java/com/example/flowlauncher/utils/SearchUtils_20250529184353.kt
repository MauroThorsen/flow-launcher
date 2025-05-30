package com.example.flowlauncher.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object SearchUtils {
    // 搜索引擎配置
    sealed class SearchEngine(val name: String, val searchUrl: String, val iconRes: Int? = null) {
        object Google : SearchEngine("Google", "https://www.google.com/search?q=")
        object Baidu : SearchEngine("百度", "https://www.baidu.com/s?wd=")
        object Bing : SearchEngine("必应", "https://cn.bing.com/search?q=")
    }

    fun isWebSearchQuery(query: String): Boolean {
        // 检查是否是网页搜索查询
        return query.contains(".") || // 包含域名
                query.contains("http") || // 包含协议
                query.contains("www") || // 包含www
                query.matches(Regex(".*[a-zA-Z]{3,}.*")) // 包含至少3个连续字母
    }

    fun search(context: Context, query: String, engine: SearchEngine = SearchEngine.Google) {
        val searchUrl = if (isValidUrl(query)) {
            // 如果是有效的URL，直接打开
            if (query.startsWith("http")) query else "https://$query"
        } else {
            // 否则使用搜索引擎搜索
            engine.searchUrl + Uri.encode(query)
        }
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            // 简单的URL验证
            val pattern = Regex("^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?$")
            pattern.matches(url)
        } catch (e: Exception) {
            false
        }
    }
} 