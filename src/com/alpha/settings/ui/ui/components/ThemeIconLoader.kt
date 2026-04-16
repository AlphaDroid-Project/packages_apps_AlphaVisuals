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

package com.alpha.settings.ui.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThemeIconLoader {
    private const val TAG = "ThemeIconLoader"

    /** Status / QS names + ThemePicker-style overlay targets (`com_android_*`, navbar, etc.). */
    private val THEME_ICON_CANDIDATE_NAMES =
        listOf(
            "stat_sys_wifi_signal_4",
            "ic_wifi_signal_4",
            "stat_sys_signal_cellular_4_4_bar",
            "ic_signal_cellular_4_4_bar",
            "ic_qs_bluetooth",
            "ic_qs_flashlight",
            "ic_qs_airplane",
            "ic_qs_wifi_3",
            "ic_qs_dnd",
            "ic_settings",
            "ic_launcher",
            // Icon overlay packs often ship only app-target glyphs (no QS art).
            "com_android_settings",
            "com_android_systemui",
            "com_android_launcher3",
            "com_google_android_apps_nbu_files",
            "com_android_chrome",
            "com_google_android_gm",
            "com_android_dialer",
            "com_android_mms",
            "ic_sysbar_home",
            "ic_sysbar_back",
            "ic_sysbar_recent",
            "ic_android",
            "ic_apps",
            "settings",
            "chrome",
            "browser",
        ).distinct()

    private val THEME_ICON_MIPMAP_CANDIDATES = listOf(
        "ic_launcher",
        "ic_launcher_round",
        "adaptiveproduct_settings",
    )

    suspend fun loadThemeIcons(context: Context, packageName: String): List<Drawable> = 
        withContext(Dispatchers.IO) {
            val icons = mutableListOf<Drawable>()
            val usedResIds = mutableSetOf<Int>()
            
            try {
                val pm = context.packageManager
                val resources = pm.getResourcesForApplication(packageName)

                fun tryAddResId(resId: Int): Boolean {
                    if (icons.size >= 6 || resId == 0 || resId in usedResIds) return false
                    val drawable = try {
                        resources.getDrawable(resId, null)
                    } catch (_: Exception) {
                        null
                    } ?: return false
                    usedResIds.add(resId)
                    icons.add(drawable)
                    return true
                }

                fun tryAddName(iconName: String, defType: String = "drawable") {
                    if (icons.size >= 6) return
                    val resId = resources.getIdentifier(iconName, defType, packageName)
                    tryAddResId(resId)
                }
                
                for (iconName in THEME_ICON_CANDIDATE_NAMES) {
                    if (icons.size >= 6) break
                    tryAddName(iconName)
                }

                for (mipmapName in THEME_ICON_MIPMAP_CANDIDATES) {
                    if (icons.size >= 6) break
                    tryAddName(mipmapName, "mipmap")
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
                                    tryAddName(drawableName)
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }

                if (icons.size < 6) {
                    try {
                        val drawableClass = Class.forName("$packageName.R\$drawable")
                        val iconFields = drawableClass.fields.filter { field ->
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
                                tryAddResId(resId)
                            } catch (_: Exception) {
                            }
                        }
                    } catch (_: ClassNotFoundException) {
                        Log.d(TAG, "R.drawable not found for $packageName")
                    }
                }
                
                if (icons.isEmpty()) {
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        if (appInfo.icon != 0) {
                            tryAddResId(appInfo.icon)
                        }
                        if (icons.isEmpty()) {
                            icons.add(pm.getApplicationIcon(appInfo))
                        }
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
            val usedResIds = mutableSetOf<Int>()
            
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
                            if (resId != 0 && resId !in usedResIds) {
                                val drawable = resources.getDrawable(resId, null)
                                if (drawable != null) {
                                    usedResIds.add(resId)
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
                                if (resId != 0 && resId !in usedResIds) {
                                    val drawable = resources.getDrawable(resId, null)
                                    if (drawable != null) {
                                        usedResIds.add(resId)
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
                        if (appInfo.icon != 0 && appInfo.icon !in usedResIds) {
                            try {
                                val drawable = resources.getDrawable(appInfo.icon, null)
                                if (drawable != null) {
                                    usedResIds.add(appInfo.icon)
                                    icons.add(drawable)
                                }
                            } catch (_: Exception) {
                            }
                        }
                        if (icons.isEmpty()) {
                            icons.add(pm.getApplicationIcon(appInfo))
                        }
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
    showSingleIcon: Boolean = false,
    useAccentBackground: Boolean = true,
    /** When true, compact single-icon preview uses the small accent plate (Wi‑Fi / signal style). */
    useCompactSignalStyle: Boolean = true,
    /** Main-list single-icon size ([PreviewDimensions.MainListIconSize]). */
    singleIconSize: Dp = PreviewDimensions.MainListIconSize,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val maxSinglePx = with(density) { singleIconSize.roundToPx() }
    val maxGridPx = with(density) { 48.dp.roundToPx() }
    var bitmaps by remember(packageName) { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(packageName, showSingleIcon) {
        bitmaps = withContext(Dispatchers.IO) {
            val icons = ThemeIconLoader.loadThemeIcons(context, packageName)
            val maxPx = if (showSingleIcon) maxSinglePx else maxGridPx
            icons.take(if (showSingleIcon) 1 else 6).mapNotNull { d ->
                runCatching { d.toBitmapForPreview(maxPx) }.getOrNull()
            }
        }
    }

    val singleIconSignalStyle =
        showSingleIcon && useAccentBackground && useCompactSignalStyle

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (singleIconSignalStyle) {
                    Modifier
                } else if (useAccentBackground) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmaps.isNotEmpty()) {
            if (showSingleIcon) {
                val iconDp = singleIconSize
                Image(
                    bitmap = bitmaps.first().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(iconDp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bitmaps.forEach { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        )
                    }
                }
            }
        } else {
            Icon(
                imageVector = Icons.Filled.Extension,
                contentDescription = null,
                modifier = Modifier.then(
                    if (showSingleIcon) Modifier.size(singleIconSize * 0.58f) else Modifier.size(40.dp),
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
            )
        }
    }
}

private fun Drawable.toBitmapForPreview(maxSidePx: Int): Bitmap {
    val iw = intrinsicWidth
    val ih = intrinsicHeight
    return if (iw > 0 && ih > 0) {
        val scale = maxSidePx.toFloat() / maxOf(iw, ih)
        val w = (iw * scale).toInt().coerceAtLeast(1)
        val h = (ih * scale).toInt().coerceAtLeast(1)
        toBitmap(w, h)
    } else {
        toBitmap(maxSidePx, maxSidePx)
    }
}

@Composable
fun AppIconPackPreview(
    packageName: String,
    modifier: Modifier = Modifier,
    showSingleIcon: Boolean = false,
    useAccentBackground: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val singlePx = with(density) { PreviewDimensions.MainListIconSize.roundToPx() }
    val gridPx = with(density) { 48.dp.roundToPx() }
    var bitmaps by remember(packageName) { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(packageName, showSingleIcon) {
        bitmaps = withContext(Dispatchers.IO) {
            val icons = if (packageName.isEmpty()) {
                loadSystemDefaultIcons(context)
            } else {
                ThemeIconLoader.loadAppIconPackIcons(context, packageName)
            }
            val maxPx = if (showSingleIcon) singlePx else gridPx
            icons.take(if (showSingleIcon) 1 else 6).mapNotNull { d ->
                runCatching { d.toBitmapForPreview(maxPx) }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (useAccentBackground) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmaps.isNotEmpty()) {
            if (showSingleIcon) {
                val iconDp = PreviewDimensions.MainListIconSize
                Image(
                    bitmap = bitmaps.first().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconDp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bitmaps.forEach { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        )
                    }
                }
            }
        }
    }
}
