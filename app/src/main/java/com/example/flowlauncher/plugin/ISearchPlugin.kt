package com.example.flowlauncher.plugin

import android.content.Context

interface ISearchPlugin {
    fun getPluginName(): String
    fun search(context: Context, query: String): List<SearchResult>
}

data class SearchResult(
    val title: String,
    val subtitle: String,
    val iconBase64: String?, // 可选，图片用Base64传递
    val actionIntent: String, // 可选，Intent的序列化字符串
    val subResults: List<SearchResult>? = null
) 