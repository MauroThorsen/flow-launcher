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
        
        println("添加短信搜索插件")
        plugins.add(SmsSearchPlugin())
        
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