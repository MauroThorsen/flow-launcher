package com.example.flowlauncher

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
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
import android.content.Intent
import androidx.compose.foundation.clickable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 动态权限请求
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
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var searchText by rememberSaveable { mutableStateOf("") }
    val apps = remember { AppUtils.getInstalledApps(context) }
    val files = remember { FileUtils.getFilesInDirectory("/storage/emulated/0/") }
    val filteredApps = apps.filter { it.appName.contains(searchText, true) }
    val filteredFiles = files.filter { it.name.contains(searchText, true) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("搜索应用或文件") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("应用列表", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(filteredApps) { app ->
                AppItem(app)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("文件列表", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(filteredFiles) { file ->
                FileItem(file)
            }
        }
    }
}

@Composable
fun AppItem(app: AppInfo) {
    val context = LocalContext.current
    Row(modifier = Modifier.padding(vertical = 4.dp).clickable {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }) {
        val iconBitmap: ImageBitmap = remember(app.icon) {
            app.icon.toBitmap(48, 48).asImageBitmap()
        }
        Image(
            bitmap = iconBitmap,
            contentDescription = app.appName,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(app.appName, style = MaterialTheme.typography.bodyLarge)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
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