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

    fun getInstalledApps(context: Context, showSystemApps: Boolean = false): List<AppInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                if (!showSystemApps && isSystemComponent(app.packageName, app)) {
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
                    if (!showSystemApps && isSystemComponent(stats.packageName, packageInfo)) {
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