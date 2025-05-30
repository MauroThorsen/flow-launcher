package com.example.flowlauncher.utils

import android.content.Context
import android.content.pm.PackageManager
import com.example.flowlauncher.data.model.AppInfo

object AppUtils {
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.map {
            AppInfo(
                appName = pm.getApplicationLabel(it).toString(),
                packageName = it.packageName,
                icon = pm.getApplicationIcon(it)
            )
        }
    }
} 