/*
 * Copyright (C) 2025 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.android.axion.axthemestore.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThemeIconLoader {
    private const val TAG = "ThemeIconLoader"
    
    suspend fun loadThemeIcons(context: Context, packageName: String): List<Drawable> = 
        withContext(Dispatchers.IO) {
            val icons = mutableListOf<Drawable>()
            
            try {
                val pm = context.packageManager
                val resources = pm.getResourcesForApplication(packageName)
                
                val commonIconNames = listOf(
                    "stat_sys_wifi_signal_4",
                    "ic_wifi_signal_4",
                    "stat_sys_signal_cellular_4_4_bar",
                    "ic_signal_cellular_4_4_bar",
                    "ic_qs_bluetooth",
                    "ic_qs_flashlight",
                    "ic_qs_airplane",
                    "ic_settings",
                    "ic_launcher"
                )
                
                for (iconName in commonIconNames) {
                    if (icons.size >= 6) break
                    
                    try {
                        val resId = resources.getIdentifier(iconName, "drawable", packageName)
                        if (resId != 0) {
                            val typedValue = android.util.TypedValue()
                            resources.getValue(resId, typedValue, true)
                            
                            val drawable = if (typedValue.string?.toString()?.endsWith(".xml") == true) {
                                Drawable.createFromXml(resources, resources.getXml(resId))
                            } else {
                                resources.openRawResource(resId).use { inputStream ->
                                    val bitmap = BitmapFactory.decodeStream(inputStream)
                                    if (bitmap != null) {
                                        BitmapDrawable(resources, bitmap)
                                    } else null
                                }
                            }
                            
                            if (drawable != null) {
                                icons.add(drawable)
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                
                if (icons.size < 6) {
                    val arrayNames = listOf(
                        "target_android",
                        "target_systemui", 
                        "target_wifi",
                        "target_signal"
                    )
                    
                    for (arrayName in arrayNames) {
                        if (icons.size >= 6) break
                        
                        try {
                            val arrayId = resources.getIdentifier(arrayName, "array", packageName)
                            if (arrayId != 0) {
                                val drawableNames = resources.getStringArray(arrayId)
                                val shuffledNames = drawableNames.toList().shuffled()
                                
                                for (drawableName in shuffledNames) {
                                    if (icons.size >= 6) break
                                    
                                    try {
                                        val resId = resources.getIdentifier(drawableName, "drawable", packageName)
                                        if (resId != 0) {
                                            val typedValue = android.util.TypedValue()
                                            resources.getValue(resId, typedValue, true)
                                            
                                            val drawable = if (typedValue.string?.toString()?.endsWith(".xml") == true) {
                                                Drawable.createFromXml(resources, resources.getXml(resId))
                                            } else {
                                                resources.openRawResource(resId).use { inputStream ->
                                                    val bitmap = BitmapFactory.decodeStream(inputStream)
                                                    if (bitmap != null) {
                                                        BitmapDrawable(resources, bitmap)
                                                    } else null
                                                }
                                            }
                                            
                                            if (drawable != null && !icons.contains(drawable)) {
                                                icons.add(drawable)
                                            }
                                        }
                                    } catch (e: Exception) {
                                    }
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                
                if (icons.isEmpty()) {
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        val appIcon = pm.getApplicationIcon(appInfo)
                        icons.add(appIcon)
                    } catch (e: Exception) {
                        Log.d(TAG, "Could not load app icon for $packageName")
                    }
                }
                
                Log.d(TAG, "Loaded ${icons.size} icons for $packageName")
                
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d(TAG, "Package not installed: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load icons from $packageName", e)
            }
            
            icons
        }
    
    suspend fun loadAppIconPackIcons(context: Context, packageName: String): List<Drawable> = 
        withContext(Dispatchers.IO) {
            val icons = mutableListOf<Drawable>()
            
            try {
                val pm = context.packageManager
                val resources = pm.getResourcesForApplication(packageName)
                
                val commonApps = listOf(
                    listOf("com_android_chrome", "chrome", "browser"),
                    listOf("com_google_android_gm", "gmail", "email"),
                    listOf("com_android_settings", "settings"),
                    listOf("com_android_dialer", "com_google_android_dialer", "phone", "dialer"),
                    listOf("com_android_mms", "com_google_android_apps_messaging", "messages", "sms"),
                    listOf("com_android_camera2", "com_google_android_GoogleCamera", "camera"),
                    listOf("com_android_vending", "playstore", "play"),
                    listOf("com_google_android_calendar", "com_android_calendar", "calendar"),
                    listOf("com_google_android_deskclock", "com_android_deskclock", "clock"),
                    listOf("com_google_android_apps_nbu_files", "com_android_documentsui", "files"),
                    listOf("com_google_android_apps_maps", "maps"),
                    listOf("com_google_android_youtube", "youtube")
                )
                
                for (appNames in commonApps) {
                    if (icons.size >= 6) break
                    
                    var foundIcon = false
                    for (iconName in appNames) {
                        try {
                            val resId = resources.getIdentifier(iconName, "drawable", packageName)
                            if (resId != 0) {
                                val drawable = resources.getDrawable(resId, null)
                                if (drawable != null) {
                                    icons.add(drawable)
                                    foundIcon = true
                                    break
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                    
                    if (foundIcon) continue
                }
                
                if (icons.size < 6) {
                    try {
                        val drawableClass = Class.forName("$packageName.R\$drawable")
                        val fields = drawableClass.fields
                        
                        val iconFields = fields.filter { field ->
                            val name = field.name.lowercase()
                            !name.startsWith("ic_launcher") && 
                            !name.startsWith("abc_") &&
                            !name.startsWith("notification_") &&
                            !name.contains("background") &&
                            !name.contains("foreground")
                        }.shuffled()
                        
                        for (field in iconFields) {
                            if (icons.size >= 6) break
                            
                            try {
                                val resId = field.getInt(null)
                                if (resId != 0) {
                                    val drawable = resources.getDrawable(resId, null)
                                    if (drawable != null && !icons.contains(drawable)) {
                                        icons.add(drawable)
                                    }
                                }
                            } catch (e: Exception) {
                            }
                        }
                    } catch (e: ClassNotFoundException) {
                        Log.d(TAG, "R.drawable class not found for $packageName")
                    }
                }
                
                if (icons.isEmpty()) {
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        val appIcon = pm.getApplicationIcon(appInfo)
                        icons.add(appIcon)
                    } catch (e: Exception) {
                        Log.d(TAG, "Could not load app icon for $packageName")
                    }
                }
                
                Log.d(TAG, "Loaded ${icons.size} app icons from icon pack $packageName")
                
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d(TAG, "Icon pack not installed: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load app icons from $packageName", e)
            }
            
            icons
        }
}

suspend fun loadSystemDefaultIcons(context: Context): List<Drawable> = 
    withContext(Dispatchers.IO) {
        val icons = mutableListOf<Drawable>()
        val pm = context.packageManager
        
        val commonPackages = listOf(
            "com.android.chrome",
            "com.google.android.gm",
            "com.android.settings",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.android.vending",
            "com.google.android.calendar",
            "com.android.calendar",
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.google.android.apps.nbu.files",
            "com.android.documentsui",
            "com.google.android.apps.maps",
            "com.google.android.youtube"
        )
        
        for (packageName in commonPackages) {
            if (icons.size >= 6) break
            
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val appIcon = pm.getApplicationIcon(appInfo)
                icons.add(appIcon)
            } catch (e: Exception) {
            }
        }
        
        Log.d("ThemeIconLoader", "Loaded ${icons.size} system default icons")
        icons
    }

@Composable
fun ThemePackagePreview(
    packageName: String,
    modifier: Modifier = Modifier,
    showSingleIcon: Boolean = false
) {
    val context = LocalContext.current
    var icons by remember(packageName) { mutableStateOf<List<Drawable>>(emptyList()) }
    
    LaunchedEffect(packageName) {
        icons = ThemeIconLoader.loadThemeIcons(context, packageName)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icons.isNotEmpty()) {
            if (showSingleIcon) {
                DrawableIcon(
                    drawable = icons.first(),
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icons.take(6).forEach { drawable ->
                        DrawableIcon(
                            drawable = drawable,
                            size = 48.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawableIcon(
    drawable: Drawable,
    size: Dp,
    tint: Color
) {
    val density = LocalDensity.current
    val bitmap = remember(drawable, size) {
        drawable.toBitmap(
            width = with(density) { size.toPx().toInt() },
            height = with(density) { size.toPx().toInt() }
        )
    }
    
    val tintedBitmap = remember(bitmap, tint) {
        val paint = Paint().apply {
            colorFilter = PorterDuffColorFilter(
                tint.toArgb(),
                PorterDuff.Mode.SRC_IN
            )
        }
        
        val result = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        result
    }
    
    Image(
        bitmap = tintedBitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.size(size)
    )
}

@Composable
fun AppIconPackPreview(
    packageName: String,
    modifier: Modifier = Modifier,
    showSingleIcon: Boolean = false
) {
    val context = LocalContext.current
    var icons by remember(packageName) { mutableStateOf<List<Drawable>>(emptyList()) }
    
    LaunchedEffect(packageName) {
        icons = if (packageName.isEmpty()) {
            loadSystemDefaultIcons(context)
        } else {
            ThemeIconLoader.loadAppIconPackIcons(context, packageName)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icons.isNotEmpty()) {
            if (showSingleIcon) {
                AppIcon(
                    drawable = icons.first(),
                    size = 36.dp
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icons.take(6).forEach { drawable ->
                        AppIcon(
                            drawable = drawable,
                            size = 48.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(
    drawable: Drawable,
    size: Dp
) {
    val density = LocalDensity.current
    val bitmap = remember(drawable, size) {
        drawable.toBitmap(
            width = with(density) { size.toPx().toInt() },
            height = with(density) { size.toPx().toInt() }
        )
    }
    
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
    )
}
