/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.data.catalog

import com.google.gson.annotations.SerializedName

/** Root object for /product/etc/assets/overlay_catalog.json (v4). */
data class OverlayCatalogRoot(
    val version: Int = 0,
    val lastUpdated: String = "",
    val meta: OverlayCatalogMeta? = null,
    val categories: List<OverlayCatalogCategoryJson> = emptyList(),
    /** Built-in QS / brightness UI styles (no overlay APKs; not part of [themes]). */
    @SerializedName("ui_styles")
    val uiStyles: List<OverlayCatalogUiStyleJson> = emptyList(),
    val themes: List<OverlayCatalogThemeJson> = emptyList(),
    val components: List<OverlayCatalogComponentJson>? = null,
)

/** Metadata for [Settings.System] `ui_style` presets shipped in SystemUI. */
data class OverlayCatalogUiStyleJson(
    val id: String = "",
    val author: String = "",
)

data class OverlayCatalogMeta(
    val authorDefault: String? = null,
)

data class OverlayCatalogCategoryJson(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val icon: String = "palette",
)

data class OverlayCatalogComponentJson(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val engineCategory: String? = null,
    val targetPackage: String = "",
    val icon: String = "palette",
)

data class OverlayCatalogThemeJson(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val author: String? = null,
    val version: String = "",
    val versionCode: Int = 0,
    val minSdk: Int = 31,
    val previewImages: List<String> = emptyList(),
    val category: String = "",
    val tags: List<String> = emptyList(),
    val isUnified: Boolean = false,
    val supportsRegionSampling: Boolean? = null,
    val overlays: List<OverlayCatalogOverlayJson> = emptyList(),
)

data class OverlayCatalogOverlayJson(
    val componentId: String = "",
    val packageName: String = "",
    val targetPackage: String = "",
    val label: String? = null,
    val targets: List<String> = emptyList(),
)
