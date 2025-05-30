package com.example.flowlauncher

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flowlauncher.data.model.AppInfo
import com.example.flowlauncher.data.model.FileInfo
import com.example.flowlauncher.ui.theme.FlowLauncherTheme
import com.example.flowlauncher.utils.AppUtils
import com.example.flowlauncher.utils.FileUtils
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissions = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
        requestPermissionLauncher.launch(permissions.toTypedArray())

        setContent {
            FlowLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var searchText by rememberSaveable { mutableStateOf("") }
    val apps = remember { AppUtils.getInstalledApps(context) }
    val files = remember { FileUtils.getFilesInDirectory("/storage/emulated/0/") }
    val frequentApps = remember { apps.take(5) } // 临时使用前5个应用作为常用应用
    val filteredApps = apps.filter { 
        it.appName.contains(searchText, true) || 
        it.packageName.contains(searchText, true) 
    }
    val filteredFiles = files.filter { it.name.contains(searchText, true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 常用应用图标栏
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(frequentApps) { app ->
                FrequentAppItem(app)
            }
        }

        // 搜索框
        SearchBar(
            query = searchText,
            onQueryChange = { searchText = it },
            onSearch = { },
            active = false,
            onActiveChange = { },
            placeholder = { Text("搜索应用或文件") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { }

        Spacer(modifier = Modifier.height(16.dp))

        // 搜索结果
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (searchText.isNotEmpty()) {
                // 搜索服务项
                item {
                    SearchServiceSection(searchText)
                }
                
                // 应用搜索结果
                items(filteredApps) { app ->
                    SearchResultItem(app)
                }
            } else {
                // 文件搜索结果
                items(filteredFiles) { file ->
                    FileItem(file)
                }
            }
        }
    }
}

@Composable
fun FrequentAppItem(app: AppInfo) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            intent?.let { context.startActivity(it) }
        }
    ) {
        Image(
            bitmap = remember(app.icon) { app.icon.toBitmap(96, 96).asImageBitmap() },
            contentDescription = app.appName,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SearchServiceSection(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "Search on Google" to Icons.Default.Search,
            "Search on Chrome" to Icons.Default.Search,
            "Search on YouTube" to Icons.Default.Search
        ).forEach { (text, icon) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* TODO: 实现搜索服务跳转 */ }
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "$text: $query",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(app: AppInfo) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_LAUNCHER)
                action = Intent.ACTION_MAIN
            }
            intent?.let { context.startActivity(it) }
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = remember(app.icon) { app.icon.toBitmap(48, 48).asImageBitmap() },
                contentDescription = app.appName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun FileItem(file: FileInfo) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        val iconRes = if (file.isDirectory) android.R.drawable.ic_menu_agenda else android.R.drawable.ic_menu_save
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = if (file.isDirectory) "文件夹" else "文件",
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(file.name, style = MaterialTheme.typography.bodyLarge)
            Text(file.path, style = MaterialTheme.typography.bodySmall)
        }
    }
}