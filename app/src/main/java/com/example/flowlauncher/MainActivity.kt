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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.example.flowlauncher.data.model.SearchSettings
import com.example.flowlauncher.data.model.AppSettings
import com.example.flowlauncher.utils.PermissionUtils

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
    
    // 应用设置
    val appSettings = remember { AppSettings() }
    val searchSettings = remember { SearchSettings() }
    
    // 权限状态
    var showPermissionDialog by remember { mutableStateOf(false) }
    var hasUsagePermission by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    
    // 获取应用列表
    val apps = remember { AppUtils.getInstalledApps(context, appSettings.showSystemApps) }
    val files = remember { FileUtils.getFilesInDirectory("/storage/emulated/0/") }
    
    // 获取常用应用列表
    val frequentApps = remember(hasUsagePermission) { 
        if (hasUsagePermission) {
            AppUtils.getFrequentlyUsedApps(
                context, 
                appSettings.frequentAppCount,
                appSettings.showSystemApps
            )
        } else {
            // 如果没有权限，则显示按字母排序的前几个应用
            apps.take(appSettings.frequentAppCount)
        }
    }

    // 检查权限并显示对话框
    LaunchedEffect(Unit) {
        if (!hasUsagePermission) {
            showPermissionDialog = true
        }
    }

    // 权限提示对话框
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要使用情况访问权限") },
            text = { Text("为了显示最常用的应用，我们需要使用情况访问权限。请在设置中授予权限。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        PermissionUtils.openUsageAccessSettings(context)
                        showPermissionDialog = false
                    }
                ) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("稍后再说")
                }
            }
        )
    }

    // 监听权限变化
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val newPermissionState = PermissionUtils.hasUsageStatsPermission(context)
            if (newPermissionState != hasUsagePermission) {
                hasUsagePermission = newPermissionState
            }
        }
    }

    val filteredApps = apps.filter { 
        it.appName.contains(searchText, true) || 
        it.packageName.contains(searchText, true) 
    }
    val filteredFiles = files.filter { it.name.contains(searchText, true) }

    // 联想词状态
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isExpanded by remember { mutableStateOf(false) }
    
    // 联想词请求
    LaunchedEffect(searchText) {
        println("搜索文本变化: '$searchText'")
        if (searchText.isNotBlank()) {
            println("开始请求联想词...")
            suggestions = GoogleSuggestUtils.fetchSuggestions(searchText)
            println("获取到 ${suggestions.size} 个联想词")
        } else {
            println("搜索文本为空，清空联想词")
            suggestions = emptyList()
            isExpanded = false
        }
    }

    // 实际显示的联想词数量
    val displayedSuggestions = if (isExpanded) {
        suggestions.take(searchSettings.expandedSuggestionCount)
    } else {
        suggestions.take(searchSettings.defaultSuggestionCount)
    }

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
                
                // 添加间距
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 常用应用图标栏
            if (searchText.isEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(frequentApps) { app ->
                        FrequentAppItem(
                            app = app,
                            maxWidth = appSettings.appItemWidth
                        )
                    }
                }
            }

            // 联想词和本地搜索结果并行展示
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = if (searchBarPosition == SearchBarPosition.BOTTOM) 80.dp else 8.dp
                )
            ) {
                // 联想词（优先展示）
                if (searchText.isNotBlank()) {
                    items(displayedSuggestions) { suggestion ->
                        SuggestionItem(suggestion) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(suggestion)}"))
                            context.startActivity(intent)
                        }
                    }
                    
                    // 显示展开/收起按钮
                    if (suggestions.size > searchSettings.defaultSuggestionCount) {
                        item {
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        if (isExpanded) "收起" else "展开更多联想词",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        if (isExpanded) 
                                            Icons.Default.KeyboardArrowUp 
                                        else 
                                            Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "收起" else "展开",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier
                                    .clickable { isExpanded = !isExpanded }
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
                
                // 本地搜索结果（仅在联想词未展开时显示）
                if (searchText.isNotBlank() && !isExpanded) {
                    items(filteredApps) { app ->
                        SearchResultItem(app)
                    }
                    items(filteredFiles) { file ->
                        FileItem(file)
                    }
                }
            }
        }

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
fun FrequentAppItem(app: AppInfo, maxWidth: Int) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(maxWidth.dp)
            .clickable {
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
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

@Composable
fun SuggestionItem(suggestion: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(suggestion) },
        leadingContent = {
            Icon(Icons.Default.Search, contentDescription = "联想搜索")
        },
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
    )
}

object GoogleSuggestUtils {
    suspend fun fetchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        println("开始获取联想词，搜索词: $query")
        if (query.isBlank()) {
            println("搜索词为空，返回空列表")
            return@withContext emptyList()
        }
        
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://suggestqueries.google.com/complete/search?client=firefox&q=$encodedQuery"
        println("请求URL: $url")
        
        return@withContext try {
            println("开始发送网络请求...")
            val result = URL(url).readText()
            println("收到响应: $result")
            
            val json = JSONArray(result)
            println("解析JSON数组，长度: ${json.length()}")
            
            val arr = json.getJSONArray(1)
            println("获取建议数组，长度: ${arr.length()}")
            
            List(arr.length()) { arr.getString(it) }.also {
                println("成功获取联想词列表: $it")
            }
        } catch (e: Exception) {
            println("获取联想词失败: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}