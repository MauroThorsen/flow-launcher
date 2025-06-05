package com.example.flowlauncher.plugin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.text.format.DateUtils
import android.util.Log
import com.example.flowlauncher.CopyToClipboardActivity

class SmsSearchPlugin : ISearchPlugin {
    companion object {
        private const val TAG = "FlowLauncher.SmsPlugin"
    }

    override fun getPluginName(): String = "短信搜索"

    override fun search(context: Context, query: String): List<SearchResult> {
        Log.d(TAG, "开始搜索短信，搜索词: $query")
        val results = mutableListOf<SearchResult>()
        
        try {
            Log.d(TAG, "开始查询短信数据库")
            // 查询短信内容
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                "${Telephony.Sms.BODY} LIKE ? OR ${Telephony.Sms.ADDRESS} LIKE ?",
                arrayOf("%$query%", "%$query%"),
                "${Telephony.Sms.DATE} DESC LIMIT 20" // 按时间倒序，限制20条
            )
            
            Log.d(TAG, "短信数据库查询完成")
            
            cursor?.use {
                Log.d(TAG, "短信游标状态 - 是否为空: ${cursor == null}, 数量: ${cursor.count}")
                
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val address = it.getString(1)
                    val body = it.getString(2)
                    val date = it.getLong(3)
                    
                    // 格式化时间
                    val timeString = DateUtils.getRelativeTimeSpanString(
                        date,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                    
                    Log.d(TAG, "找到短信: $address - $body (时间: $timeString)")
                    
                    try {
                        // 创建多个操作选项
                        val actions = mutableListOf<SearchResult>()
                        
                        // 1. 查看短信详情
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("content://sms/inbox")
                            putExtra("message_id", id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        actions.add(
                            SearchResult(
                                title = "查看详情",
                                subtitle = "在短信应用中查看",
                                iconBase64 = null,
                                actionIntent = viewIntent.toUri(Intent.URI_INTENT_SCHEME)
                            )
                        )
                        Log.d(TAG, "已添加查看详情操作")
                        
                        // 2. 回复短信
                        val replyIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$address")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        actions.add(
                            SearchResult(
                                title = "回复短信",
                                subtitle = address,
                                iconBase64 = null,
                                actionIntent = replyIntent.toUri(Intent.URI_INTENT_SCHEME)
                            )
                        )
                        Log.d(TAG, "已添加回复短信操作")
                        
                        // 3. 拨打电话
                        val callIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$address")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        actions.add(
                            SearchResult(
                                title = "拨打电话",
                                subtitle = address,
                                iconBase64 = null,
                                actionIntent = callIntent.toUri(Intent.URI_INTENT_SCHEME)
                            )
                        )
                        Log.d(TAG, "已添加拨打电话操作")
                        
                        // 4. 复制短信内容
                        val copyIntent = Intent(context, CopyToClipboardActivity::class.java).apply {
                            putExtra("text", body)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        actions.add(
                            SearchResult(
                                title = "复制内容",
                                subtitle = body,
                                iconBase64 = null,
                                actionIntent = copyIntent.toUri(Intent.URI_INTENT_SCHEME)
                            )
                        )
                        Log.d(TAG, "已添加复制内容操作")
                        
                        // 添加主结果
                        results.add(
                            SearchResult(
                                title = address,
                                subtitle = "$body\n$timeString",
                                iconBase64 = null,
                                actionIntent = viewIntent.toUri(Intent.URI_INTENT_SCHEME),
                                subResults = actions
                            )
                        )
                        Log.d(TAG, "已添加短信主结果: $address")
                    } catch (e: Exception) {
                        Log.e(TAG, "处理短信 $address 的操作时出错", e)
                    }
                }
            } ?: Log.w(TAG, "短信游标为空")
        } catch (e: Exception) {
            Log.e(TAG, "短信搜索过程出错", e)
        }
        
        Log.d(TAG, "短信搜索完成，找到 ${results.size} 个结果")
        return results
    }
} 