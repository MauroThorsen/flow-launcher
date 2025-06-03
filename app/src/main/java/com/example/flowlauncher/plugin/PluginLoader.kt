package com.example.flowlauncher.plugin

import android.content.Context
import dalvik.system.DexClassLoader

class PluginLoader(private val context: Context) {
    private val pluginDir = context.getExternalFilesDir("plugins")

    fun loadPlugins(): List<ISearchPlugin> {
        println("开始加载插件，插件目录: ${pluginDir?.absolutePath}")
        val plugins = mutableListOf<ISearchPlugin>()
        
        // 添加内置插件
        println("添加内置联系人搜索插件")
        plugins.add(ContactsSearchPlugin())
        
        // 加载外部插件
        pluginDir?.listFiles()?.forEach { file ->
            println("检查文件: ${file.name}")
            if (file.extension == "jar" || file.extension == "dex") {
                try {
                    println("尝试加载插件: ${file.absolutePath}")
                    val dexClassLoader = DexClassLoader(
                        file.absolutePath,
                        context.codeCacheDir.absolutePath,
                        null,
                        context.classLoader
                    )
                    // 约定插件实现类名为 com.example.flowlauncher.plugin.PluginImpl
                    val clazz = dexClassLoader.loadClass("com.example.flowlauncher.plugin.PluginImpl")
                    val plugin = clazz.newInstance() as ISearchPlugin
                    println("成功加载插件: ${plugin.getPluginName()}")
                    plugins.add(plugin)
                } catch (e: Exception) {
                    println("加载插件失败: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        println("插件加载完成，总数: ${plugins.size}")
        return plugins
    }
}

// 内置的联系人搜索插件
class ContactsSearchPlugin : ISearchPlugin {
    override fun getPluginName(): String = "联系人搜索"

    override fun search(context: Context, query: String): List<SearchResult> {
        println("联系人搜索插件开始搜索: $query")
        val results = mutableListOf<SearchResult>()
        try {
            val cursor = context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                null
            )
            println("查询联系人数据库完成")
            
            cursor?.use {
                println("联系人游标是否为空: ${cursor == null}")
                println("联系人数量: ${cursor.count}")
                while (it.moveToNext()) {
                    val name = it.getString(0)
                    val number = it.getString(1)
                    println("找到联系人: $name - $number")
                    results.add(
                        SearchResult(
                            title = name,
                            subtitle = number,
                            iconBase64 = null,
                            actionIntent = ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("联系人搜索出错: ${e.message}")
            e.printStackTrace()
        }
        println("联系人搜索完成，找到 ${results.size} 个结果")
        return results
    }
} 