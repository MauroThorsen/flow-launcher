# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 添加缺失类的规则
-dontwarn com.google.errorprone.annotations.MustBeClosed

# 保持插件相关的类
-keep class com.example.flowlauncher.plugin.** { *; }
-keep interface com.example.flowlauncher.plugin.** { *; }

# 保持插件加载器
-keep class com.example.flowlauncher.plugin.PluginLoader { *; }
-keep class com.example.flowlauncher.plugin.ISearchPlugin { *; }
-keep class com.example.flowlauncher.plugin.SearchResult { *; }