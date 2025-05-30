package com.example.flowlauncher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Browser
import com.example.flowlauncher.data.model.BrowserHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BrowserUtils {
    private val CHROME_PACKAGE = "com.android.chrome"
    private val CHROME_HISTORY_URI = Uri.parse("content://com.android.chrome.browser/history")
    private val ANDROID_HISTORY_URI = Uri.parse("content://browser/bookmarks")

    suspend fun getHistory(context: Context, query: String? = null): List<BrowserHistory> = withContext(Dispatchers.IO) {
        val histories = mutableListOf<BrowserHistory>()
        
        try {
            // 尝试获取 Chrome 历史记录
            histories.addAll(getChromeHistory(context, query))
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果获取 Chrome 历史失败，尝试获取系统浏览器历史
            try {
                histories.addAll(getAndroidHistory(context, query))
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                "title LIKE ? OR url LIKE ?"
            } else null
            
            val selectionArgs = if (query != null) {
                arrayOf("%$query%", "%$query%")
            } else null
            
            cursor = context.contentResolver.query(
                CHROME_HISTORY_URI,
                projection,
                selection,
                selectionArgs,
                "last_visit_time DESC"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    histories.add(
                        BrowserHistory(
                            id = it.getLong(0),
                            title = it.getString(1) ?: "",
                            url = it.getString(2) ?: "",
                            lastVisitTime = it.getLong(3)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        
        return@withContext histories
    }

    private suspend fun getAndroidHistory(context: Context, query: String? = null): List<BrowserHistory> = withContext(Dispatchers.IO) {
        val histories = mutableListOf<BrowserHistory>()
        var cursor: Cursor? = null
        
        try {
            val projection = arrayOf(
                Browser.BookmarkColumns._ID,
                Browser.BookmarkColumns.TITLE,
                Browser.BookmarkColumns.URL,
                Browser.BookmarkColumns.DATE
            )
            
            val selection = if (query != null) {
                "${Browser.BookmarkColumns.TITLE} LIKE ? OR ${Browser.BookmarkColumns.URL} LIKE ?"
            } else null
            
            val selectionArgs = if (query != null) {
                arrayOf("%$query%", "%$query%")
            } else null
            
            cursor = context.contentResolver.query(
                ANDROID_HISTORY_URI,
                projection,
                selection,
                selectionArgs,
                "${Browser.BookmarkColumns.DATE} DESC"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    histories.add(
                        BrowserHistory(
                            id = it.getLong(0),
                            title = it.getString(1) ?: "",
                            url = it.getString(2) ?: "",
                            lastVisitTime = it.getLong(3)
                        )
                    )
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
} 