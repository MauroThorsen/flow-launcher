package com.example.flowlauncher.utils

import com.example.flowlauncher.data.model.FileInfo
import java.io.File

object FileUtils {
    fun getFilesInDirectory(path: String): List<FileInfo> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()?.map {
            FileInfo(
                name = it.name,
                path = it.absolutePath,
                isDirectory = it.isDirectory
            )
        } ?: emptyList()
    }
} 