package com.example.flowlauncher.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object SearchUtils {
    private const val TAG = "SearchUtils"

    // 搜索引擎配置
    sealed class SearchEngine(val name: String, val searchUrl: String, val iconRes: Int? = null) {
        object Google : SearchEngine("Google", "https://www.google.com/search?q=")
        object Baidu : SearchEngine("百度", "https://www.baidu.com/s?wd=")
        object Bing : SearchEngine("必应", "https://cn.bing.com/search?q=")
    }

    fun isWebSearchQuery(query: String): Boolean {
        // 检查是否是网页搜索查询
        val result = query.contains(".") || // 包含域名
                query.contains("http") || // 包含协议
                query.contains("www") || // 包含www
                query.matches(Regex(".*[a-zA-Z]{3,}.*")) // 包含至少3个连续字母
        
        Log.d(TAG, "isWebSearchQuery: query=$query, result=$result")
        return result
    }

    fun search(context: Context, query: String, engine: SearchEngine = SearchEngine.Google) {
        Log.d(TAG, "search: query=$query, engine=${engine.name}")
        
        val searchUrl = if (isValidUrl(query)) {
            Log.d(TAG, "检测到有效URL")
            // 如果是有效的URL，直接打开
            if (query.startsWith("http")) query else "https://$query"
        } else {
            Log.d(TAG, "使用搜索引擎搜索")
            // 否则使用搜索引擎搜索
            engine.searchUrl + Uri.encode(query)
        }
        
        Log.d(TAG, "最终URL: $searchUrl")
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "成功启动浏览器")
        } catch (e: Exception) {
            Log.e(TAG, "启动浏览器失败", e)
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            // 简单的URL验证
            val pattern = Regex("^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?$")
            val result = pattern.matches(url)
            Log.d(TAG, "isValidUrl: url=$url, result=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "URL验证失败", e)
            false
        }
    }
} 