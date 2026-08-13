/*
 * Copyright (C) 2025-2026 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.alpha.settings.ui.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.alpha.settings.ui.data.model.Theme

/**
 * Store / detail preview for a local overlay theme: static previews, gesture trail, battery shapes,
 * or package icons. Main list uses [MainListGlyphSlot] (square slot from parent constraints).
 */
@Composable
fun ThemeStoreItemPreview(
    theme: Theme,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    /** Applied to compact drawable sizes; detail uses [PreviewDimensions.DetailCompactIconScale]. */
    compactIconScale: Float = 1f,
) {
    val previewCategory = PreviewDimensions.normalizeStoreCategoryForPreview(theme.category)
    // For icon packs, prefer the android overlay — it ships ic_wifi_signal_4.
    val pkg = if (previewCategory == "icon_packs") {
        theme.overlays.firstOrNull { it.componentId.endsWith("icon_pack.android") }?.packageName
            ?: theme.overlays.firstOrNull()?.packageName
            ?: ""
    } else {
        theme.overlays.firstOrNull()?.packageName.orEmpty()
    }
    if (compact) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            MainListGlyphSlot(modifier = Modifier.fillMaxSize()) {
                ThemeStoreItemPreviewCompactInner(
                    category = previewCategory,
                    packageName = pkg,
                    modifier = Modifier.fillMaxSize(),
                    iconScale = compactIconScale,
                )
            }
        }
    } else {
        ThemeStoreItemPreviewDetail(
            previewCategory = previewCategory,
            packageName = pkg,
            modifier = modifier,
        )
    }
}

@Composable
private fun ThemeStoreItemPreviewCompactInner(
    category: String,
    packageName: String,
    modifier: Modifier,
    iconScale: Float = 1f,
) {
    val iconSize = PreviewDimensions.mainListIconSizeForCategory(category) * iconScale
    when (category) {
        "icon_packs" -> IconPackPreview(
            packageName = packageName,
            modifier = modifier,
            iconSize = iconSize,
            compact = true,
        )
        "wifi_icons" -> ThemePackagePreview(
            packageName = packageName,
            modifier = modifier,
            showSingleIcon = true,
            useAccentBackground = true,
            useCompactSignalStyle = true,
            singleIconSize = iconSize,
        )
        "signal_icons" -> ThemePackagePreview(
            packageName = packageName,
            modifier = modifier,
            showSingleIcon = true,
            useAccentBackground = true,
            useCompactSignalStyle = true,
            singleIconSize = iconSize,
        )
        "charging_animation" -> ChargingAnimationStaticPreview(
            packageName = packageName,
            modifier = modifier,
            iconSize = iconSize,
        )
        "battery_style" -> BatteryStylePreview(
            packageName = packageName,
            modifier = modifier,
            iconSize = iconSize,
        )
        "back_gesture" -> BackGesturePreview(modifier)
        "navbar" -> NavbarPreview(
            packageName = packageName,
            modifier = modifier,
            compact = true,
            iconSize = iconSize,
        )
        "lockscreen_clock_font" -> LockClockFontPreview(
            packageName,
            modifier,
            compact = true,
            compactTextScale = iconScale,
            previewKind = FontOverlayPreviewKind.LockscreenClock,
        )
        "font" -> LockClockFontPreview(
            packageName,
            modifier,
            compact = true,
            compactTextScale = iconScale,
            previewKind = FontOverlayPreviewKind.SystemFont,
        )
        else -> ThemePackagePreview(
            packageName = packageName,
            modifier = modifier,
            showSingleIcon = true,
            useAccentBackground = true,
            useCompactSignalStyle = true,
            singleIconSize = iconSize,
        )
    }
}

@Composable
private fun ThemeStoreItemPreviewDetail(
    previewCategory: String,
    packageName: String,
    modifier: Modifier,
) {
    when (previewCategory) {
        "icon_packs" -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            IconPackDetailTripletPreview(
                packageName = packageName,
                modifier = Modifier.fillMaxSize(),
                detailIconScale = PreviewDimensions.DetailCompactIconScale,
            )
        }
        "wifi_icons" -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            IconSetDetailPreview(
                packageName = packageName,
                drawableNames = WIFI_SIGNAL_DRAWABLES,
                modifier = Modifier.fillMaxSize(),
                iconSize = 32.dp * PreviewDimensions.DetailCompactIconScale,
            )
        }
        "signal_icons" -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            IconSetDetailPreview(
                packageName = packageName,
                drawableNames = SIGNAL_CELLULAR_DRAWABLES,
                modifier = Modifier.fillMaxSize(),
                iconSize = 32.dp * PreviewDimensions.DetailCompactIconScale,
            )
        }
        "charging_animation" -> {
            val style = packageName.substringAfterLast('.')
            Box(
                modifier = modifier.clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (style == "moto" || style == "nothing" || style == "supervooc") {
                    ChargingAnimationBannerPreview(
                        packageName = packageName,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ThemePackagePreview(
                        packageName = packageName,
                        modifier = Modifier.fillMaxSize(),
                        showSingleIcon = false,
                        useAccentBackground = true,
                        useCompactSignalStyle = false,
                    )
                }
            }
        }
        "battery_style" -> Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BatteryStylePreview(packageName, Modifier.fillMaxSize())
        }
        "back_gesture" -> Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BackGesturePreview(Modifier.fillMaxSize())
        }
        "navbar" -> Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            NavbarPreview(packageName, Modifier.fillMaxSize(), compact = false)
        }
        "lockscreen_clock_font" -> Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            LockClockFontPreview(
                packageName,
                Modifier.fillMaxSize(),
                compact = false,
                previewKind = FontOverlayPreviewKind.LockscreenClock,
            )
        }
        "font" -> Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            LockClockFontPreview(
                packageName,
                Modifier.fillMaxSize(),
                compact = false,
                previewKind = FontOverlayPreviewKind.SystemFont,
            )
        }
        else -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            ThemePackagePreview(
                packageName = packageName,
                modifier = Modifier.fillMaxSize(),
                showSingleIcon = false,
                useAccentBackground = true,
            )
        }
    }
}
