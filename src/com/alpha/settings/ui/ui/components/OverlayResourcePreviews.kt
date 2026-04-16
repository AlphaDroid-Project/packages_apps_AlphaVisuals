/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui.components

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.TextViewCompat
import com.alpha.settings.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** QS tile drawables for icon-pack **detail** triplet: Bluetooth. */
private val ICON_PACK_DETAIL_BLUETOOTH = listOf(
    "ic_qs_bluetooth",
    "ic_qs_bluetooth_on",
    "ic_qs_bluetooth_off",
    "ic_qs_bluetooth_connecting",
    "ic_bluetooth",
)

/** Location / GPS-style QS drawables. */
private val ICON_PACK_DETAIL_LOCATION = listOf(
    "ic_qs_location",
    "ic_qs_location_on",
    "ic_location",
    "ic_signal_location",
    "qs_location_icon",
    "stat_sys_gps_on",
)

/** NFC QS drawables. */
private val ICON_PACK_DETAIL_NFC = listOf(
    "ic_qs_nfc",
    "ic_qs_nfc_on",
    "ic_nfc",
    "qs_nfc_icon",
)

/** ThemePicker-style icon overlays: often only `com_android_*` / navbar drawables, no QS previews. */
private val ICON_PACK_APP_TARGET_DRAWABLES = listOf(
    "com_android_settings",
    "com_android_systemui",
    "com_android_launcher3",
    "com_android_chrome",
    "com_google_android_gm",
    "com_android_dialer",
    "ic_sysbar_home",
    "ic_sysbar_back",
    "ic_sysbar_recent",
    "ic_android",
    "settings",
    "chrome",
)

/** Fills any empty slot after preferred lists (distinct drawables only). */
private val ICON_PACK_DETAIL_FALLBACK_ANY = listOf(
    "preview_large",
    "preview_small",
    "ic_qs_wifi_3",
    "ic_qs_wifi_4",
    "ic_qs_dnd_on",
    "ic_qs_sync_on",
    "ic_qs_airplane",
)

/**
 * Main-list / hero single-tile preview: try names in order; use the first drawable that decodes to
 * a bitmap (resource id alone is not enough — vectors or bad ids can fail [Drawable.toBitmap]).
 */
private val ICON_PACK_SINGLE_PREVIEW_CANDIDATES_COMPACT =
    (listOf("preview_small", "preview_large") + ICON_PACK_APP_TARGET_DRAWABLES +
        ICON_PACK_DETAIL_FALLBACK_ANY +
        listOf(
            "ic_qs_bluetooth",
            "ic_qs_bluetooth_on",
            "ic_qs_bluetooth_off",
            "ic_qs_bluetooth_detail_empty",
        )).distinct()

private val ICON_PACK_SINGLE_PREVIEW_CANDIDATES_FULL =
    (listOf("preview_large", "preview_small") + ICON_PACK_APP_TARGET_DRAWABLES +
        ICON_PACK_DETAIL_FALLBACK_ANY +
        listOf(
            "ic_qs_bluetooth",
            "ic_qs_bluetooth_on",
            "ic_qs_bluetooth_off",
            "ic_qs_bluetooth_detail_empty",
        )).distinct()

private val ICON_PACK_SINGLE_PREVIEW_MIPMAPS = listOf("ic_launcher", "ic_launcher_round")

private fun drawableToBitmapForPackPreview(d: android.graphics.drawable.Drawable): android.graphics.Bitmap? {
    val size = when {
        d is AdaptiveIconDrawable -> 288
        d.intrinsicWidth > 0 && d.intrinsicHeight > 0 ->
            maxOf(d.intrinsicWidth, d.intrinsicHeight)
        else -> 288
    }.coerceIn(64, 512)
    return runCatching {
        if (d is AdaptiveIconDrawable) {
            d.setBounds(0, 0, size, size)
        }
        d.toBitmap(size, size)
    }.getOrNull() ?: runCatching { d.toBitmap(512, 512) }.getOrNull()
}

private fun loadApplicationIconPreviewBitmap(pm: PackageManager, packageName: String): android.graphics.Bitmap? =
    runCatching {
        val ai = pm.getApplicationInfo(packageName, 0)
        val d = pm.getApplicationIcon(ai)
        drawableToBitmapForPackPreview(d)
    }.getOrNull()

private fun loadFirstAvailableIconPackPreviewBitmap(
    pm: PackageManager,
    packageName: String,
    candidates: List<String>,
): android.graphics.Bitmap? {
    if (packageName.isBlank()) return null
    val res = runCatching { pm.getResourcesForApplication(packageName) }.getOrNull() ?: return null
    val usedIds = mutableSetOf<Int>()
    fun tryDrawableRes(id: Int): android.graphics.Bitmap? {
        if (id == 0 || id in usedIds) return null
        val d = runCatching { res.getDrawable(id, null) }.getOrNull() ?: return null
        val bmp = drawableToBitmapForPackPreview(d)
        if (bmp != null) usedIds.add(id)
        return bmp
    }
    for (n in candidates) {
        val id = res.getIdentifier(n, "drawable", packageName)
        tryDrawableRes(id)?.let { return it }
    }
    for (n in ICON_PACK_SINGLE_PREVIEW_MIPMAPS) {
        val id = res.getIdentifier(n, "mipmap", packageName)
        tryDrawableRes(id)?.let { return it }
    }
    runCatching { pm.getApplicationInfo(packageName, 0).icon }.getOrNull()?.let { iconRes ->
        tryDrawableRes(iconRes)?.let { return it }
    }
    return loadApplicationIconPreviewBitmap(pm, packageName)
}

private fun loadIconPackDetailTripletBitmaps(
    pm: PackageManager,
    packageName: String,
): List<android.graphics.Bitmap?> {
    if (packageName.isBlank()) return listOf(null, null, null)
    val res = runCatching { pm.getResourcesForApplication(packageName) }.getOrNull()
        ?: return listOf(null, null, null)
    val used = mutableSetOf<Int>()
    fun takeFirst(names: List<String>): android.graphics.Bitmap? {
        for (n in names) {
            val id = res.getIdentifier(n, "drawable", packageName)
            if (id == 0 || id in used) continue
            val d = runCatching { res.getDrawable(id, null) }.getOrNull() ?: continue
            val bmp = drawableToBitmapForPackPreview(d)
            if (bmp != null) {
                used.add(id)
                return bmp
            }
        }
        return null
    }
    val out = arrayOfNulls<android.graphics.Bitmap>(3)
    out[0] = takeFirst(ICON_PACK_DETAIL_BLUETOOTH)
    out[1] = takeFirst(ICON_PACK_DETAIL_LOCATION)
    out[2] = takeFirst(ICON_PACK_DETAIL_NFC)
    for (i in 0..2) {
        if (out[i] == null) {
            out[i] = takeFirst(ICON_PACK_DETAIL_FALLBACK_ANY)
        }
    }
    val any = out.firstOrNull { it != null }
    if (any != null) {
        for (i in 0..2) {
            if (out[i] == null) {
                out[i] = any
            }
        }
    }
    return out.toList()
}

/**
 * Theme **detail** hero: three QS glyphs — Bluetooth, Location, NFC — with per-slot drawable
 * fallbacks, then [ICON_PACK_DETAIL_FALLBACK_ANY] for any still-empty slot.
 */
@Composable
fun IconPackDetailTripletPreview(
    packageName: String,
    modifier: Modifier = Modifier,
    detailIconScale: Float = 1f,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var bitmaps by remember(packageName) {
        mutableStateOf<List<android.graphics.Bitmap?>>(listOf(null, null, null))
    }
    LaunchedEffect(packageName) {
        bitmaps = withContext(Dispatchers.IO) {
            loadIconPackDetailTripletBitmaps(pm, packageName)
        }
    }
    val gap = 14.dp * detailIconScale
    val iconDp = 30.dp * detailIconScale
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bitmaps.forEach { bmp ->
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(iconDp),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    )
                } else {
                    Box(modifier = Modifier.size(iconDp))
                }
            }
        }
    }
}

@Composable
fun NavbarPreview(
    packageName: String,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    iconSize: Dp = PreviewDimensions.MainListIconSize,
    /**
     * When [compact] is false: draw back / home / recents in a row without the inner gradient plate
     * (parent supplies background, e.g. theme detail hero).
     */
    embedDetailInParent: Boolean = false,
    /** Scales the three-button strip on detail ([PreviewDimensions.DetailCompactIconScale]). */
    detailIconScale: Float = 1f,
) {
    val context = LocalContext.current
    val pm = context.packageManager

    val names = listOf("ic_sysbar_back", "ic_sysbar_home", "ic_sysbar_recent")
    var bitmaps by remember(packageName) { mutableStateOf(listOf<android.graphics.Bitmap?>()) }

    LaunchedEffect(packageName) {
        bitmaps = withContext(Dispatchers.IO) {
            runCatching {
                val res = pm.getResourcesForApplication(packageName)
                names.map { n ->
                    val id = res.getIdentifier(n, "drawable", packageName)
                    if (id == 0) null else res.getDrawable(id, null)?.toBitmap()
                }
            }.getOrElse { emptyList() }
        }
    }

    if (compact) {
        val iconDp = iconSize
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            val single = (bitmaps.getOrNull(1) ?: bitmaps.firstOrNull { it != null })
            if (single != null) {
                Image(
                    bitmap = single.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(iconDp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
    } else {
        val gap = 14.dp * detailIconScale
        val iconDp = 30.dp * detailIconScale
        val platePadH = 14.dp * detailIconScale
        val platePadV = 10.dp * detailIconScale
        val embedPadH = 8.dp * detailIconScale
        val embedPadV = 4.dp * detailIconScale

        @Composable
        fun ThreeButtonRow(rowModifier: Modifier) {
            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bitmaps.take(3).forEach { bmp ->
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(iconDp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        )
                    } else {
                        Box(modifier = Modifier.size(iconDp))
                    }
                }
            }
        }

        if (embedDetailInParent) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                ThreeButtonRow(
                    Modifier.padding(horizontal = embedPadH, vertical = embedPadV),
                )
            }
        } else {
            ThreeButtonRow(
                modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ),
                    )
                    .padding(horizontal = platePadH, vertical = platePadV),
            )
        }
    }
}

/**
 * Main list / detail: static `res/drawable/preview` from the charging-animation overlay (vendor
 * themes ship this next to the animation drawables).
 */
@Composable
fun ChargingAnimationStaticPreview(
    packageName: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = PreviewDimensions.MainListIconSize,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var bmp by remember(packageName) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(packageName) {
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                if (packageName.isBlank()) return@runCatching null
                val res = pm.getResourcesForApplication(packageName)
                val id = res.getIdentifier("preview", "drawable", packageName)
                if (id == 0) return@runCatching null
                val d = res.getDrawable(id, null) ?: return@runCatching null
                val w = d.intrinsicWidth.takeIf { it > 0 } ?: 512
                val h = d.intrinsicHeight.takeIf { it > 0 } ?: 512
                d.toBitmap(w, h)
            }.getOrNull()
        }
    }

    if (bmp != null) {
        val iconDp = iconSize
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(
                bitmap = bmp!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(iconDp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            )
        }
    } else {
        ThemePackagePreview(
            packageName = packageName,
            modifier = modifier,
            showSingleIcon = true,
            useAccentBackground = false,
            useCompactSignalStyle = true,
            singleIconSize = iconSize,
        )
    }
}

/**
 * Which overlay `config_*FontFamily` string [LockClockFontPreview] loads.
 * [LockscreenClock] uses `config_clockFontFamily`; [SystemFont] uses `config_bodyFontFamily`.
 */
enum class FontOverlayPreviewKind {
    LockscreenClock,
    SystemFont,
}

@Composable
fun LockClockFontPreview(
    packageName: String,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    /** Multiplier for compact thumbnail text size (e.g. detail preview at 2×). */
    compactTextScale: Float = 1f,
    /**
     * When [compact] is false: skip the inner elevated plate so a parent (e.g. theme detail
     * hero frame) provides the background.
     */
    embedDetailInParent: Boolean = false,
    /** Multiplier for detail hero `sp` (e.g. [PreviewDimensions.DetailCompactIconScale]). */
    detailTitleScale: Float = 1f,
    /** Lock screen clock overlays vs system UI font (`android.theme.customization.font`) overlays. */
    previewKind: FontOverlayPreviewKind = FontOverlayPreviewKind.LockscreenClock,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val thumbnailText = stringResource(R.string.clock_font_preview_thumbnail)
    val detailTitleText = stringResource(R.string.clock_font_preview_detail_title)
    val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()

    val configResName = when (previewKind) {
        FontOverlayPreviewKind.LockscreenClock -> "config_clockFontFamily"
        FontOverlayPreviewKind.SystemFont -> "config_bodyFontFamily"
    }

    var typeface by remember(packageName, previewKind) { mutableStateOf<Typeface?>(null) }

    LaunchedEffect(packageName, previewKind) {
        typeface = withContext(Dispatchers.IO) {
            runCatching {
                val res = pm.getResourcesForApplication(packageName)
                val id = res.getIdentifier(configResName, "string", packageName)
                if (id == 0) return@runCatching null
                val family = res.getString(id)?.trim().orEmpty()
                if (family.isBlank()) null else Typeface.create(family, Typeface.NORMAL)
            }.getOrNull()
        }
    }

    val tf = typeface ?: Typeface.DEFAULT

    if (compact) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                TextView(ctx).apply {
                    text = thumbnailText
                    includeFontPadding = false
                    gravity = Gravity.CENTER
                }
            },
            update = { tv ->
                tv.text = thumbnailText
                tv.setTextColor(onSurface)
                tv.typeface = tf
                tv.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    PreviewDimensions.ClockFontPreviewCompactThumbnailSp * compactTextScale,
                )
            },
        )
    } else {
        val detailModifier = if (embedDetailInParent) {
            modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        } else {
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        }
        AndroidView(
            modifier = detailModifier,
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    addView(
                        TextView(ctx).apply {
                            includeFontPadding = false
                            gravity = Gravity.CENTER
                        },
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }
            },
            update = { root ->
                val title = (root as FrameLayout).getChildAt(0) as TextView
                title.text = detailTitleText
                title.setTextColor(onSurface)
                title.typeface = tf
                title.maxLines = 1
                val minIx = PreviewDimensions.ClockFontPreviewDetailTitleMinSp.toInt()
                val maxSizeSp = PreviewDimensions.ClockFontPreviewDetailTitleSp * detailTitleScale
                val maxIx = kotlin.math.max(minIx, kotlin.math.round(maxSizeSp.toDouble()).toInt())
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    title,
                    minIx,
                    maxIx,
                    1,
                    TypedValue.COMPLEX_UNIT_SP,
                )
            },
        )
    }
}

/** Wifi signal icon drawables for detail preview (weakest → strongest). */
internal val WIFI_SIGNAL_DRAWABLES = listOf(
    "ic_wifi_signal_1",
    "ic_wifi_signal_2",
    "ic_wifi_signal_3",
    "ic_wifi_signal_4",
)

/** Cellular signal bar drawables for detail preview (weakest → strongest). */
internal val SIGNAL_CELLULAR_DRAWABLES = listOf(
    "ic_signal_cellular_1_4_bar",
    "ic_signal_cellular_2_4_bar",
    "ic_signal_cellular_3_4_bar",
    "ic_signal_cellular_4_4_bar",
)

/**
 * Detail preview for wifi / signal categories: loads a fixed set of [drawableNames] from
 * [packageName] and renders them in a row tinted with [MaterialTheme.colorScheme.onSurface].
 */
@Composable
fun IconSetDetailPreview(
    packageName: String,
    drawableNames: List<String>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val onSurface = MaterialTheme.colorScheme.onSurface

    var bitmaps by remember(packageName) {
        mutableStateOf<List<Bitmap?>>(List(drawableNames.size) { null })
    }

    LaunchedEffect(packageName) {
        bitmaps = withContext(Dispatchers.IO) {
            runCatching {
                val res = pm.getResourcesForApplication(packageName)
                drawableNames.map { name ->
                    val id = res.getIdentifier(name, "drawable", packageName)
                    if (id == 0) return@map null
                    res.getDrawable(id, null)?.let { drawableToBitmapForPackPreview(it) }
                }
            }.getOrElse { List(drawableNames.size) { null } }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bitmaps.forEach { bmp ->
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(onSurface),
                    )
                } else {
                    Box(modifier = Modifier.size(iconSize))
                }
            }
        }
    }
}

/**
 * Icon-pack preview: loads `ic_wifi_signal_4` from the android overlay package and tints it with
 * [MaterialTheme.colorScheme.onSurface].
 */
@Composable
fun IconPackPreview(
    packageName: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = PreviewDimensions.MainListIconSize,
    compact: Boolean = true,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val onSurface = MaterialTheme.colorScheme.onSurface

    var bmp by remember(packageName) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(packageName) {
        val cacheKey = PreviewBitmapCache.key(packageName, "wifi_signal")
        PreviewBitmapCache[cacheKey]?.let { bmp = it; return@LaunchedEffect }
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                val res = pm.getResourcesForApplication(packageName)
                val id = res.getIdentifier("ic_wifi_signal_4", "drawable", packageName)
                if (id == 0) return@runCatching null
                val d = res.getDrawable(id, null) ?: return@runCatching null
                drawableToBitmapForPackPreview(d)
            }.getOrNull()?.also { PreviewBitmapCache[cacheKey] = it }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                modifier = if (compact) Modifier.size(iconSize) else Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(onSurface),
            )
        }
    }
}

/**
 * LRU in-memory cache for preview bitmaps so scrolling back up (or re-entering the screen) does
 * not redo PM + drawable decode work.  Keyed by `"$packageName|$variant"`.
 */
internal object PreviewBitmapCache {
    private const val MAX = 80
    private val map = object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean =
            size > MAX
    }

    fun key(packageName: String, compact: Boolean): String = "$packageName|${if (compact) "c" else "f"}"
    fun key(packageName: String, variant: String): String = "$packageName|$variant"

    operator fun get(key: String): Bitmap? = synchronized(map) { map[key] }
    operator fun set(key: String, bmp: Bitmap) { synchronized(map) { map[key] = bmp } }
}

