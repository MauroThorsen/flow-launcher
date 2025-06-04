package com.example.flowlauncher

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast

class CopyToClipboardActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val text = intent.getStringExtra("text")
        if (text != null) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
        
        finish()
    }
} 