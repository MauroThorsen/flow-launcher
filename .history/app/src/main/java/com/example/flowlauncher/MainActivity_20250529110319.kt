package com.example.flowlauncher

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.res.painterResource
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
import androidx.core.view.WindowCompat
import com.example.flowlauncher.data.model.SearchBarPosition
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URLEncoder
import java.net.URL
import android.net.Uri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边缘到边缘显示
        enableEdgeToEdge()
        // 设置系统栏为透明
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                    ) { paddingValues ->
                        HomeScreen(Modifier.padding(paddingValues))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchBarPosition by rememberSaveable { mutableStateOf(SearchBarPosition.TOP) }
    val apps = remember { AppUtils.getInstalledApps(context) }
    val files = remember { FileUtils.getFilesInDirectory("/storage/emulated/0/") }
    val frequentApps = remember { apps.take(5) }
    val filteredApps = apps.filter { 
        it.appName.contains(searchText, true) || 
        it.packageName.contains(searchText, true) 
    }
    val filteredFiles = files.filter { it.name.contains(searchText, true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (searchBarPosition == SearchBarPosition.TOP) {
                SearchBarSection(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onSearchPositionChange = {
                        searchBarPosition = if (searchBarPosition == SearchBarPosition.TOP) 
                            SearchBarPosition.BOTTOM else SearchBarPosition.TOP
                    }
                )
            }

            // 常用应用图标栏
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(frequentApps) { app ->
                    FrequentAppItem(app)
                }
            }

            // 搜索结果
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = if (searchBarPosition == SearchBarPosition.BOTTOM) 80.dp else 8.dp
                )
            ) {
                if (searchText.isNotEmpty()) {
                    items(filteredApps) { app ->
                        SearchResultItem(app)
                    }
                } else {
                    items(filteredFiles) { file ->
                        FileItem(file)
                    }
                }
            }
        }

        // 底部搜索框
        if (searchBarPosition == SearchBarPosition.BOTTOM) {
            SearchBarSection(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onSearchPositionChange = {
                    searchBarPosition = if (searchBarPosition == SearchBarPosition.TOP) 
                        SearchBarPosition.BOTTOM else SearchBarPosition.TOP
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarSection(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearchPositionChange: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        TextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            placeholder = { Text("搜索应用或文件") },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search,
                    contentDescription = "搜索",
                    modifier = Modifier.padding(start = 4.dp)
                )
            },
            trailingIcon = {
                Row {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { onSearchTextChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                    IconButton(onClick = onSearchPositionChange) {
                        Icon(Icons.Default.Search, contentDescription = "切换搜索框位置")
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            singleLine = true
        )
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

object GoogleSuggestUtils {
    suspend fun fetchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "https://suggestqueries.google.com/complete/search?client=firefox&q=" + URLEncoder.encode(query, "UTF-8")
        return@withContext try {
            val result = URL(url).readText()
            val json = JSONArray(result)
            val arr = json.getJSONArray(1)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}