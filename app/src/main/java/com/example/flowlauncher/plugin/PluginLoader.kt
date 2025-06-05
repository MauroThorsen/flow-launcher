package com.example.flowlauncher.plugin

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader

class PluginLoader(private val context: Context) {
    companion object {
        private const val TAG = "FlowLauncher.PluginLoader"
    }
    
    private val pluginDir = context.getExternalFilesDir("plugins")

    fun loadPlugins(): List<ISearchPlugin> {
        Log.d(TAG, "开始加载插件，插件目录: ${pluginDir?.absolutePath}")
        val plugins = mutableListOf<ISearchPlugin>()
        
        // 添加内置插件
        try {
            Log.d(TAG, "开始加载内置联系人搜索插件")
            plugins.add(ContactsSearchPlugin())
            Log.d(TAG, "内置联系人搜索插件加载成功")
        } catch (e: Exception) {
            Log.e(TAG, "内置联系人搜索插件加载失败", e)
        }
        
        try {
            Log.d(TAG, "开始加载内置短信搜索插件")
            plugins.add(SmsSearchPlugin())
            Log.d(TAG, "内置短信搜索插件加载成功")
        } catch (e: Exception) {
            Log.e(TAG, "内置短信搜索插件加载失败", e)
        }
        
        // 加载外部插件
        pluginDir?.listFiles()?.forEach { file ->
            Log.d(TAG, "检查文件: ${file.name} (${file.absolutePath})")
            if (file.extension == "jar" || file.extension == "dex") {
                try {
                    Log.d(TAG, "尝试加载外部插件: ${file.absolutePath}")
                    val dexClassLoader = DexClassLoader(
                        file.absolutePath,
                        context.codeCacheDir.absolutePath,
                        null,
                        context.classLoader
                    )
                    Log.d(TAG, "DexClassLoader创建成功，开始加载插件类")
                    
                    // 约定插件实现类名为 com.example.flowlauncher.plugin.PluginImpl
                    val clazz = dexClassLoader.loadClass("com.example.flowlauncher.plugin.PluginImpl")
                    Log.d(TAG, "插件类加载成功: ${clazz.name}")
                    
                    val plugin = clazz.newInstance() as ISearchPlugin
                    Log.d(TAG, "插件实例化成功: ${plugin.getPluginName()}")
                    plugins.add(plugin)
                } catch (e: ClassNotFoundException) {
                    Log.e(TAG, "找不到插件类: com.example.flowlauncher.plugin.PluginImpl", e)
                } catch (e: ClassCastException) {
                    Log.e(TAG, "插件类未实现ISearchPlugin接口", e)
                } catch (e: Exception) {
                    Log.e(TAG, "加载插件失败: ${file.name}", e)
                }
            } else {
                Log.d(TAG, "跳过非插件文件: ${file.name}")
            }
        }
        
        Log.d(TAG, "插件加载完成，总数: ${plugins.size}")
        plugins.forEach { plugin ->
            Log.d(TAG, "已加载插件: ${plugin.getPluginName()}")
        }
        return plugins
    }
} 