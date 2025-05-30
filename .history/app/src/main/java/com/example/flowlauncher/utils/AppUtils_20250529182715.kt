package com.example.flowlauncher.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import com.example.flowlauncher.data.model.AppInfo
import java.util.concurrent.TimeUnit

object AppUtils {
    fun getInstalledApps(context: Context, showSystemApps: Boolean = false): List<AppInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!showSystemApps && isSystemApp) {
                    false
                } else {
                    packageManager.getLaunchIntentForPackage(app.packageName) != null
                }
            }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    appName = packageManager.getApplicationLabel(app).toString(),
                    icon = packageManager.getApplicationIcon(app.packageName)
                )
            }
            .sortedBy { it.appName }
    }

    fun getFrequentlyUsedApps(context: Context, limit: Int = 5, showSystemApps: Boolean = false): List<AppInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return getInstalledApps(context, showSystemApps).take(limit)

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(30) // 获取最近30天的使用统计

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        )

        val packageManager = context.packageManager
        return usageStats
            .filter { stats ->
                val packageInfo = try {
                    packageManager.getApplicationInfo(stats.packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
                
                if (packageInfo == null) false
                else {
                    val isSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (!showSystemApps && isSystemApp) {
                        false
                    } else {
                        packageManager.getLaunchIntentForPackage(stats.packageName) != null
                    }
                }
            }
            .sortedByDescending { it.totalTimeInForeground }
            .take(limit)
            .mapNotNull { stats ->
                try {
                    val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                    AppInfo(
                        packageName = stats.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        icon = packageManager.getApplicationIcon(stats.packageName)
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
    }
} 