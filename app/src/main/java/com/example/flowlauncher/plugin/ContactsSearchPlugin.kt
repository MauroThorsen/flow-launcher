package com.example.flowlauncher.plugin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.example.flowlauncher.CopyToClipboardActivity

class ContactsSearchPlugin : ISearchPlugin {
    companion object {
        private const val TAG = "FlowLauncher.ContactsPlugin"
    }

    override fun getPluginName(): String = "联系人搜索"

    override fun search(context: Context, query: String): List<SearchResult> {
        Log.d(TAG, "开始搜索联系人，搜索词: $query")
        val results = mutableListOf<SearchResult>()
        try {
            Log.d(TAG, "开始查询联系人数据库")
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                null
            )
            Log.d(TAG, "联系人数据库查询完成")
            
            cursor?.use {
                Log.d(TAG, "联系人游标状态 - 是否为空: ${cursor == null}, 数量: ${cursor.count}")
                while (it.moveToNext()) {
                    val contactId = it.getLong(0)
                    val name = it.getString(1)
                    val number = it.getString(2)
                    Log.d(TAG, "找到联系人: $name ($number)")
                    
                    // 创建多个操作选项
                    val actions = mutableListOf<SearchResult>()
                    
                    try {
                        // 1. 拨打电话
                        val callIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$number")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        actions.add(
                            SearchResult(
                                title = "拨打电话",
                                subtitle = number,
                                iconBase64 = null,
                                actionIntent = callIntent.toUri(Intent.URI_INTENT_SCHEME)
                            )
                        )
                        Log.d(TAG, "已添加拨打电话操作")
                        
                        // 2. 发送短信
                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$number")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        actions.add(
                            SearchResult(
                                title = "发送短信",
                                subtitle = number,
                                iconBase64 = null,
                                actionIntent = smsIntent.toUri(Intent.URI_INTENT_SCHEME)
                            )
                        )
                        Log.d(TAG, "已添加发送短信操作")
                        
                        // 3. 查看联系人详情
                        val viewContactIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.withAppendedPath(
                                ContactsContract.Contacts.CONTENT_URI,
                                contactId.toString()
                            )
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        actions.add(
                            SearchResult(
                                title = "查看详情",
                                subtitle = "查看联系人完整信息",
                                iconBase64 = null,
                                actionIntent = viewContactIntent.toUri(Intent.URI_INTENT_SCHEME)
                            )
                        )
                        Log.d(TAG, "已添加查看详情操作")
                        
                        // 4. 复制号码
                        val copyIntent = Intent(context, CopyToClipboardActivity::class.java).apply {
                            putExtra("text", number)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        actions.add(
                            SearchResult(
                                title = "复制号码",
                                subtitle = number,
                                iconBase64 = null,
                                actionIntent = copyIntent.toUri(Intent.URI_INTENT_SCHEME)
                            )
                        )
                        Log.d(TAG, "已添加复制号码操作")
                        
                        // 添加主结果
                        results.add(
                            SearchResult(
                                title = name,
                                subtitle = number,
                                iconBase64 = null,
                                actionIntent = viewContactIntent.toUri(Intent.URI_INTENT_SCHEME),
                                subResults = actions
                            )
                        )
                        Log.d(TAG, "已添加联系人主结果: $name")
                    } catch (e: Exception) {
                        Log.e(TAG, "处理联系人 $name 的操作时出错", e)
                    }
                }
            } ?: Log.w(TAG, "联系人游标为空")
        } catch (e: Exception) {
            Log.e(TAG, "联系人搜索过程出错", e)
        }
        Log.d(TAG, "联系人搜索完成，找到 ${results.size} 个结果")
        return results
    }
} 