package com.example.flowlauncher.plugin

import android.content.Context
import android.provider.ContactsContract

class PluginImpl : ISearchPlugin {
    override fun getPluginName(): String = "联系人搜索"

    override fun search(context: Context, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),
            null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val number = it.getString(1)
                results.add(
                    SearchResult(
                        title = name,
                        subtitle = number,
                        iconBase64 = null, // 可选：可实现头像转base64
                        actionIntent = ""  // 可选：可实现拨号Intent序列化
                    )
                )
            }
        }
        return results
    }
} 