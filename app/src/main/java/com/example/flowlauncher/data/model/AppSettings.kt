package com.example.flowlauncher.data.model
 
data class AppSettings(
    val showSystemApps: Boolean = false,
    val frequentAppCount: Int = 5,
    val appItemWidth: Int = 80 // dp
) 