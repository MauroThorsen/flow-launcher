package com.example.flowlauncher.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.Log
import com.example.flowlauncher.data.db.AppDatabase
import com.example.flowlauncher.data.db.AppEntity
import com.example.flowlauncher.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object AppUtils {
    private const val TAG = "FlowLauncher.AppUtils"

    private val SYSTEM_PACKAGE_PREFIXES = listOf(
        "com.android.internal.",
        "com.android.systemui",
        "com.android.settings",
        "com.android.providers.",
        "com.android.cts.",
        "com.android.shell",
        "com.android.bluetooth",
        "com.android.inputdevices",
        "com.android.certinstaller",
        "android",
        "com.android.backupconfirm"
    )

    private fun isSystemComponent(packageName: String, applicationInfo: ApplicationInfo): Boolean {
        // 检查是否是系统组件
        return SYSTEM_PACKAGE_PREFIXES.any { prefix -> packageName.startsWith(prefix) } ||
                // 检查是否没有启动器图标
                (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0 &&
                applicationInfo.flags and ApplicationInfo.FLAG_HAS_CODE == 0)
    }

    suspend fun getInstalledApps(context: Context, showSystemApps: Boolean = false): List<AppInfo> {
        Log.d(TAG, "开始获取已安装应用列表 - showSystemApps: $showSystemApps")
        return withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val dao = db.appDao()
            Log.d(TAG, "数据库连接已建立")

            // 取出所有应用实体
            val entitiesFlow = if (showSystemApps) dao.getAllApps() else dao.getNonSystemApps()
            val entityList = try {
                Log.d(TAG, "开始从数据库读取应用列表")
                val result = entitiesFlow.first()
                Log.d(TAG, "数据库读取完成 - 数量: ${result.size}")
                result
            } catch (e: Exception) {
                Log.e(TAG, "数据库读取失败", e)
                emptyList<AppEntity>()
            }
            
            val lastUpdate = entityList.maxOfOrNull { it.lastUpdated } ?: 0L
            val currentTime = System.currentTimeMillis()
            Log.d(TAG, "上次更新时间: $lastUpdate, 当前时间: $currentTime")

            if (currentTime - lastUpdate > TimeUnit.HOURS.toMillis(1) || entityList.isEmpty()) {
                Log.d(TAG, "需要更新应用列表")
                // 更新数据库
                val packageManager = context.packageManager
                val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { app ->
                        val isSystem = isSystemComponent(app.packageName, app)
                        val hasLauncher = packageManager.getLaunchIntentForPackage(app.packageName) != null
                        val shouldInclude = if (!showSystemApps && isSystem) false else hasLauncher
                        Log.d(TAG, "应用 ${app.packageName} - 系统应用: $isSystem, 有启动器: $hasLauncher, 是否包含: $shouldInclude")
                        shouldInclude
                    }
                    .map { app ->
                        AppEntity(
                            packageName = app.packageName,
                            appName = packageManager.getApplicationLabel(app).toString(),
                            lastUpdated = currentTime,
                            isSystemApp = isSystemComponent(app.packageName, app),
                            iconPath = app.sourceDir
                        )
                    }
                Log.d(TAG, "准备更新数据库 - 应用数量: ${apps.size}")
                dao.deleteAllApps()
                dao.insertApps(apps)
                Log.d(TAG, "数据库更新完成")
                
                // 重新获取
                val updatedEntitiesFlow = if (showSystemApps) dao.getAllApps() else dao.getNonSystemApps()
                updatedEntitiesFlow.first()
            } else {
                Log.d(TAG, "使用缓存的应用列表")
                entityList
            }.map { entity ->
                try {
                    AppInfo(
                        packageName = entity.packageName,
                        appName = entity.appName,
                        icon = context.packageManager.getApplicationIcon(entity.packageName)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "获取应用图标失败: ${entity.packageName}", e)
                    null
                }
            }.filterNotNull()
        }
    }

    suspend fun getFrequentlyUsedApps(context: Context, limit: Int = 5, showSystemApps: Boolean = false): List<AppInfo> {
        Log.d(TAG, "开始获取常用应用列表 - limit: $limit, showSystemApps: $showSystemApps")
        return withContext(Dispatchers.IO) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                Log.w(TAG, "无法获取使用情况统计服务，将返回前 $limit 个应用")
                return@withContext getInstalledApps(context, showSystemApps).take(limit)
            }

            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(30)
            Log.d(TAG, "查询使用情况统计 - 开始时间: $startTime, 结束时间: $endTime")

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
            Log.d(TAG, "获取到使用情况统计 - 数量: ${usageStats?.size ?: 0}")

            val packageManager = context.packageManager
            usageStats
                ?.filter { stats ->
                    val packageInfo = try {
                        packageManager.getApplicationInfo(stats.packageName, 0)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w(TAG, "找不到应用信息: ${stats.packageName}")
                        null
                    }
                    if (packageInfo == null) {
                        Log.d(TAG, "跳过应用 ${stats.packageName} - 找不到包信息")
                        false
                    } else {
                        val isSystem = isSystemComponent(stats.packageName, packageInfo)
                        val hasLauncher = packageManager.getLaunchIntentForPackage(stats.packageName) != null
                        val shouldInclude = if (!showSystemApps && isSystem) false else hasLauncher
                        Log.d(TAG, "应用 ${stats.packageName} - 系统应用: $isSystem, 有启动器: $hasLauncher, 是否包含: $shouldInclude")
                        shouldInclude
                    }
                }
                ?.sortedByDescending { it.totalTimeInForeground }
                ?.take(limit)
                ?.mapNotNull { stats ->
                    try {
                        val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                        AppInfo(
                            packageName = stats.packageName,
                            appName = packageManager.getApplicationLabel(appInfo).toString(),
                            icon = packageManager.getApplicationIcon(stats.packageName)
                        ).also {
                            Log.d(TAG, "添加常用应用: ${it.appName} (${it.packageName}) - 使用时间: ${stats.totalTimeInForeground}ms")
                        }
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.e(TAG, "获取应用信息失败: ${stats.packageName}", e)
                        null
                    }
                } ?: emptyList()
        }
    }
} 