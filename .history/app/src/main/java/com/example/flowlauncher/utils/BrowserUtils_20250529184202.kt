package com.example.flowlauncher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import com.example.flowlauncher.data.model.BrowserHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BrowserUtils {
    // Chrome 的内容提供者 URI
    private val CHROME_CONTENT_URI = Uri.parse("content://com.android.chrome.browser/history")
    
    // 常见浏览器包名
    private val BROWSER_PACKAGES = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.microsoft.emmx"
    )

    suspend fun getHistory(context: Context, query: String? = null): List<BrowserHistory> = withContext(Dispatchers.IO) {
        val histories = mutableListOf<BrowserHistory>()
        
        // 尝试获取 Chrome 历史记录
        try {
            histories.addAll(getChromeHistory(context, query))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext histories
    }

    private suspend fun getChromeHistory(context: Context, query: String? = null): List<BrowserHistory> = withContext(Dispatchers.IO) {
        val histories = mutableListOf<BrowserHistory>()
        var cursor: Cursor? = null
        
        try {
            val projection = arrayOf(
                "_id",
                "title",
                "url",
                "last_visit_time"
            )
            
            val selection = if (query != null) {
                "(title LIKE ? OR url LIKE ?) AND url NOT LIKE 'file:%' AND url NOT LIKE 'content:%'"
            } else {
                "url NOT LIKE 'file:%' AND url NOT LIKE 'content:%'"
            }
            
            val selectionArgs = if (query != null) {
                arrayOf("%$query%", "%$query%")
            } else null
            
            cursor = context.contentResolver.query(
                CHROME_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "last_visit_time DESC LIMIT 100" // 限制返回数量
            )
            
            cursor?.use {
                val idColumn = it.getColumnIndex("_id")
                val titleColumn = it.getColumnIndex("title")
                val urlColumn = it.getColumnIndex("url")
                val timeColumn = it.getColumnIndex("last_visit_time")
                
                while (it.moveToNext()) {
                    if (idColumn >= 0 && titleColumn >= 0 && urlColumn >= 0 && timeColumn >= 0) {
                        val url = it.getString(urlColumn) ?: ""
                        // 过滤掉非 http(s) 链接
                        if (url.startsWith("http")) {
                            histories.add(
                                BrowserHistory(
                                    id = it.getLong(idColumn),
                                    title = it.getString(titleColumn) ?: "",
                                    url = url,
                                    lastVisitTime = it.getLong(timeColumn)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        
        return@withContext histories
    }

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getDefaultBrowser(context: Context): String? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }
} 