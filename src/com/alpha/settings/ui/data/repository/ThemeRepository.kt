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

package com.alpha.settings.ui.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import android.content.res.Resources
import com.alpha.settings.ui.data.catalog.OverlayCatalogRoot
import com.alpha.settings.ui.data.catalog.OverlayCatalogThemeJson
import com.alpha.settings.ui.data.model.IconPack
import com.alpha.settings.ui.data.model.Theme
import com.alpha.settings.ui.data.model.ThemeCategory
import com.alpha.settings.ui.data.model.ThemeComponent
import com.alpha.settings.ui.data.model.ThemeOverlay
import com.alpha.settings.ui.data.model.ThemesResponse
import com.alpha.settings.ui.data.model.UiStyleCatalogEntry
import com.alpha.settings.ui.ui.components.PreviewDimensions
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThemeRepository(private val context: Context) {

    companion object {
        private const val TAG = "ThemeRepository"

        private val IGNORED_THEMEPICKER_CATEGORIES = setOf(
            "android.theme.customization.adaptive_icon_shape",
            "android.theme.customization.system_palette",
            "android.theme.customization.accent_color",
            "android.theme.customization.color_source",
        )

        /** Shown only as dependencies of other features; not listed as standalone “themes”. */
        private val IGNORED_AUXILIARY_OVERLAY_CATEGORIES = setOf(
            "android.theme.customization.smartspace",
            "android.theme.customization.smartspace_offset",
            "android.theme.customization.hideclock",
        )

        /** `com.android.theme.icon_pack.<family>.<target>` — all targets merge into one store theme. */
        private val ICON_PACK_FAMILY_PATTERN = Regex(
            "^com\\.android\\.theme\\.icon_pack\\.([^.]+)\\.([^.]+)$",
        )

        private val MERGEABLE_ICON_PACK_TARGETS = setOf(
            "android",
            "systemui",
            "settings",
            "launcher",
            "themepicker",
        )

        private const val CATEGORY_ICON_PACKS = "icon_packs"

        private const val OVERLAY_CATALOG_REL = "etc/assets/overlay_catalog.json"
    }

    suspend fun fetchThemes(forceRefresh: Boolean = false): Result<ThemesResponse> =
        withContext(Dispatchers.IO) {
            try {
                val catalog = loadOverlayCatalog()
                val themes = getInstalledThemes(catalog)
                val categories = buildCategories(themes, catalog)
                val components = mapCatalogComponents(catalog)
                val uiStyles = mapCatalogUiStyles(catalog)
                Result.success(
                    ThemesResponse(
                        version = catalog?.version ?: 1,
                        lastUpdated = catalog?.lastUpdated.orEmpty(),
                        themes = themes,
                        categories = categories,
                        components = components,
                        uiStyles = uiStyles,
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load installed themes", e)
                Result.failure(e)
            }
        }

    fun getInstalledVersionCode(packageName: String): Int? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.longVersionCode.toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    
    fun isThemeInstalled(packageName: String): Boolean {
        return getInstalledVersionCode(packageName) != null
    }

    /** Themes from [overlay_catalog.json] on the product image + any overlay APKs not listed there. */
    private fun getInstalledThemes(catalog: OverlayCatalogRoot?): List<Theme> {
        val pm = context.packageManager
        val fromCatalog = mutableListOf<Theme>()
        val catalogPackages = mutableSetOf<String>()
        // Icon pack families that already have a catalog entry — all PM targets of the same family
        // must be suppressed so each family only appears once.
        val catalogIconPackFamilies = mutableSetOf<String>()
        if (catalog != null && catalog.themes.isNotEmpty()) {
            val authorDefault = catalog.meta?.authorDefault?.trim()?.takeUnless { it.isEmpty() } ?: ""
            for (entry in catalog.themes) {
                val theme = themeFromCatalogEntry(entry, pm, authorDefault) ?: continue
                fromCatalog.add(theme)
                theme.overlays.forEach { overlay ->
                    catalogPackages.add(overlay.packageName)
                    iconPackFamilyKey(overlay.packageName)?.let { catalogIconPackFamilies.add(it) }
                }
            }
            Log.d(TAG, "Overlay catalog: ${fromCatalog.size} installed theme(s) from ${catalog.themes.size} catalog entries")
        }
        val rawPm = scanPackageManagerOverlays(pm).filter { theme ->
            val pkg = theme.overlays.firstOrNull()?.packageName ?: return@filter true
            if (pkg in catalogPackages) return@filter false
            // Drop every remaining target of a family that's already covered by the catalog.
            iconPackFamilyKey(pkg)?.let { if (it in catalogIconPackFamilies) return@filter false }
            true
        }
        // Catalog themes are already multi-overlay where needed; only PM-only rows get icon-pack merge.
        val combined = fromCatalog + mergeIconPackFamilies(rawPm)
        return dedupeThemesByOverlayPackages(combined).sortedBy { it.name.lowercase() }
    }

    /**
     * One store row per distinct overlay set: the same APK can appear as both a catalog entry and a
     * PM-scanned `local_*` row (or duplicate catalog rows), which produced paired “one with icon /
     * one empty” tiles.
     */
    private fun dedupeThemesByOverlayPackages(themes: List<Theme>): List<Theme> {
        val noOverlays = themes.filter { it.overlays.isEmpty() }
        val withOverlays = themes.filter { it.overlays.isNotEmpty() }
        val deduped = withOverlays
            .groupBy { t ->
                t.overlays.map { it.packageName }.sorted().joinToString("\u0000")
            }
            .values
            .map { group -> group.reduce { a, b -> preferThemeForDuplicateOverlay(a, b) } }
        return deduped + noOverlays
    }

    private fun preferThemeForDuplicateOverlay(a: Theme, b: Theme): Theme {
        val aCatalog = a.id.startsWith("catalog_")
        val bCatalog = b.id.startsWith("catalog_")
        return when {
            aCatalog && !bCatalog -> a
            bCatalog && !aCatalog -> b
            a.id.startsWith("local_iconpack_") && !b.id.startsWith("local_iconpack_") -> b
            b.id.startsWith("local_iconpack_") && !a.id.startsWith("local_iconpack_") -> a
            else -> if (a.overlays.size >= b.overlays.size) a else b
        }
    }

    private fun loadOverlayCatalog(): OverlayCatalogRoot? {
        val dir = Environment.getProductDirectory()
        val file = File(dir, OVERLAY_CATALOG_REL)
        if (!file.isFile || !file.canRead()) {
            Log.w(TAG, "Overlay catalog missing or unreadable: ${file.absolutePath}")
            return null
        }
        return try {
            val text = file.readText(Charsets.UTF_8)
            val root = Gson().fromJson(text, OverlayCatalogRoot::class.java)
                ?: return null
            patchCatalogAuthorsFromRawJson(text, root)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Invalid overlay catalog JSON", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read overlay catalog", e)
            null
        }
    }

    /**
     * Ensures each theme's [OverlayCatalogThemeJson.author] matches the file (Gson+R8 can drop fields).
     */
    private fun patchCatalogAuthorsFromRawJson(rawJson: String, root: OverlayCatalogRoot): OverlayCatalogRoot {
        return try {
            val arr = JsonParser.parseString(rawJson).asJsonObject.getAsJsonArray("themes") ?: return root
            val patched = root.themes.mapIndexed { i, t ->
                if (i >= arr.size()) return@mapIndexed t
                val jo = arr[i].takeIf { it.isJsonObject }?.asJsonObject ?: return@mapIndexed t
                val el = jo.get("author") ?: return@mapIndexed t
                if (el.isJsonNull) return@mapIndexed t
                val auth = el.asString.trim()
                if (auth.isEmpty()) return@mapIndexed t
                t.copy(author = auth)
            }
            root.copy(themes = patched)
        } catch (e: Exception) {
            Log.w(TAG, "Could not patch catalog authors from raw JSON", e)
            root
        }
    }

    private fun themeFromCatalogEntry(
        t: OverlayCatalogThemeJson,
        pm: PackageManager,
        authorDefault: String,
    ): Theme? {
        if (t.overlays.isEmpty()) return null
        val overlays = mutableListOf<ThemeOverlay>()
        var maxVc = 0
        var firstOverlayPkg: String? = null
        for (o in t.overlays) {
            val pi = try {
                pm.getPackageInfo(o.packageName, PackageManager.GET_META_DATA)
            } catch (_: PackageManager.NameNotFoundException) {
                return null
            }
            if (pi.applicationInfo?.enabled == false) return null
            if (!pi.isOverlayPackage()) return null
            val overlayCategory = pi.overlayCategory ?: return null
            if (!overlayCategory.startsWith("android.theme.customization.")) return null
            if (overlayCategory in IGNORED_THEMEPICKER_CATEGORIES) return null
            if (overlayCategory in IGNORED_AUXILIARY_OVERLAY_CATEGORIES) return null
            val vc = pi.longVersionCode.toInt()
            if (vc > maxVc) maxVc = vc
            if (firstOverlayPkg == null) firstOverlayPkg = o.packageName
            overlays.add(
                ThemeOverlay(
                    componentId = overlayCategory,
                    packageName = o.packageName,
                    targetPackage = pi.overlayTarget ?: o.targetPackage,
                    targets = listOf(overlayCategory),
                    label = o.label?.trim().orEmpty(),
                    enabled = true,
                ),
            )
        }
        val author = t.author?.trim()?.takeUnless { it.isEmpty() }
            ?: firstOverlayPkg?.let { pkg -> resolveAuthorFromOverlayPackage(pm, pkg) }
            ?: authorDefault
        val unified = t.isUnified && t.overlays.size == 1
        return Theme(
            id = "catalog_${t.id}",
            name = t.name,
            description = t.description.trim(),
            author = author,
            version = t.version.ifBlank { "1.0" },
            versionCode = maxVc,
            minSdk = t.minSdk,
            previewImages = t.previewImages,
            category = t.category,
            tags = t.tags + "catalog",
            overlays = overlays,
            isUnified = unified,
            supportsRegionSampling = t.supportsRegionSampling == true,
        )
    }

    private fun scanPackageManagerOverlays(pm: PackageManager): List<Theme> {
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        return packages.mapNotNull { packageInfo ->
            if (packageInfo.applicationInfo?.enabled == false) return@mapNotNull null
            val overlayCategory = packageInfo.overlayCategory ?: return@mapNotNull null
            if (!packageInfo.isOverlayPackage()) return@mapNotNull null
            if (!overlayCategory.startsWith("android.theme.customization.")) return@mapNotNull null
            if (overlayCategory in IGNORED_THEMEPICKER_CATEGORIES) return@mapNotNull null
            if (overlayCategory in IGNORED_AUXILIARY_OVERLAY_CATEGORIES) return@mapNotNull null

            val packageName = packageInfo.packageName
            val appLabel = packageInfo.applicationInfo?.let(pm::getApplicationLabel)?.toString()
                ?: packageName
            val componentId = overlayCategory.removePrefix("android.theme.customization.")
            val author = resolveAuthorFromOverlayPackage(pm, packageName) ?: ""
            val storeCategory = PreviewDimensions.normalizeStoreCategoryForPreview(componentId)

            Theme(
                id = "local_$packageName",
                name = appLabel,
                description = "",
                author = author,
                version = packageInfo.versionName ?: "1.0",
                versionCode = packageInfo.longVersionCode.toInt(),
                minSdk = packageInfo.applicationInfo?.minSdkVersion ?: 31,
                previewImages = emptyList(),
                category = storeCategory,
                tags = listOf(componentId, "local"),
                overlays = listOf(
                    ThemeOverlay(
                        componentId = overlayCategory,
                        packageName = packageName,
                        targetPackage = packageInfo.overlayTarget ?: "",
                        targets = listOf(overlayCategory),
                        label = "",
                        enabled = true,
                    )
                ),
                isUnified = true,
            )
        }
    }

    private fun resolveAuthorFromOverlayPackage(pm: PackageManager, packageName: String): String? {
        val resources = try {
            pm.getResourcesForApplication(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val keys = listOf("overlay_author", "theme_author", "author")
        for (k in keys) {
            val id = resources.getIdentifier(k, "string", packageName)
            if (id != 0) {
                val v = runCatching { resources.getString(id) }.getOrNull()
                if (!v.isNullOrBlank()) return v.trim()
            }
        }
        return null
    }

    private fun mapCatalogComponents(catalog: OverlayCatalogRoot?): List<ThemeComponent> {
        val list = catalog?.components ?: return emptyList()
        return list.map { c ->
            ThemeComponent(
                id = c.id,
                name = c.name,
                description = c.description.orEmpty(),
                targetPackage = c.targetPackage,
                icon = c.icon,
            )
        }
    }

    private fun mapCatalogUiStyles(catalog: OverlayCatalogRoot?): List<UiStyleCatalogEntry> {
        val list = catalog?.uiStyles ?: return emptyList()
        return list.mapNotNull { row ->
            val id = row.id.trim().ifEmpty { return@mapNotNull null }
            val author = row.author.trim().ifEmpty { "AlphaDroid" }
            UiStyleCatalogEntry(id = id, author = author)
        }
    }

    /**
     * Unifies every `com.android.theme.icon_pack.<family>.*` overlay (android, systemui, settings,
     * launcher, themepicker) into one store theme per family so the label is e.g. “Aurora” once and
     * apply enables all installed targets together.
     */
    private fun mergeIconPackFamilies(themes: List<Theme>): List<Theme> {
        val passthrough = mutableListOf<Theme>()
        val iconPackRows = mutableListOf<Theme>()
        for (t in themes) {
            val pkg = t.overlays.firstOrNull()?.packageName
            if (pkg == null) {
                passthrough.add(t)
                continue
            }
            val family = iconPackFamilyKey(pkg)
            if (family != null) {
                iconPackRows.add(t)
            } else {
                passthrough.add(t)
            }
        }
        val byFamily = LinkedHashMap<String, MutableList<Theme>>()
        for (t in iconPackRows) {
            val key = iconPackFamilyKey(t.overlays.first().packageName)!!
            byFamily.getOrPut(key) { mutableListOf() }.add(t)
        }
        val merged = byFamily.map { (family, group) ->
            mergeIconPackFamilyGroup(group, family)
        }
        return passthrough + merged
    }

    private fun iconPackFamilyKey(packageName: String): String? {
        val m = ICON_PACK_FAMILY_PATTERN.matchEntire(packageName) ?: return null
        val target = m.groupValues[2].lowercase()
        if (target !in MERGEABLE_ICON_PACK_TARGETS) return null
        return m.groupValues[1].lowercase()
    }

    private fun mergeIconPackFamilyGroup(group: List<Theme>, family: String): Theme {
        val overlays = group
            .map { it.overlays.first() }
            .distinctBy { it.componentId }
            .sortedBy { it.componentId }
        val name = unifiedIconPackDisplayName(group)
        val base = group.first()
        val mergedAuthor = group.firstOrNull { it.author.isNotBlank() }?.author?.trim().orEmpty()
        return base.copy(
            id = "local_iconpack_$family",
            name = name,
            author = mergedAuthor,
            category = CATEGORY_ICON_PACKS,
            tags = listOf(CATEGORY_ICON_PACKS, "local", family),
            overlays = overlays,
            isUnified = false,
            versionCode = group.maxOf { it.versionCode },
        )
    }

    private fun unifiedIconPackDisplayName(group: List<Theme>): String {
        val distinct = group.map { it.name.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinct.size == 1) return distinct.first()
        return distinct.minByOrNull { it.length } ?: group.first().name
    }

    private fun buildCategories(themes: List<Theme>, catalog: OverlayCatalogRoot?): List<ThemeCategory> {
        val present = themes.map { it.category }.toSet()
        if (catalog != null && catalog.categories.isNotEmpty()) {
            return catalog.categories
                .filter { it.id in present }
                .map { c ->
                    ThemeCategory(id = c.id, name = c.name, icon = c.icon)
                }
        }
        return themes
            .map { it.category }
            .distinct()
            .sorted()
            .map { id ->
                ThemeCategory(
                    id = id,
                    name = id.replace('_', ' ')
                        .split(' ')
                        .joinToString(" ") { token ->
                            token.replaceFirstChar { it.uppercase() }
                        },
                    icon = "palette",
                )
            }
    }

    suspend fun getInstalledIconPacks(): List<IconPack> = withContext(Dispatchers.IO) {
        val iconPacks = mutableListOf<IconPack>()
        val pm = context.packageManager

        val intentActions = listOf(
            "org.adw.launcher.THEMES",
            "com.teslacoilsw.launcher.THEME"
        )

        val seenPackages = mutableSetOf<String>()

        for (action in intentActions) {
            val intent = Intent(action)
            val formatList = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)

            android.util.Log.d("ThemeRepository", "Icon pack query for action $action found ${formatList.size} packages")

            for (resolveInfo in formatList) {
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName !in seenPackages) {
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        val label = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(appInfo)
                        
                        iconPacks.add(IconPack(packageName, label, icon))
                        seenPackages.add(packageName)
                        android.util.Log.d("ThemeRepository", "Added icon pack: $label ($packageName)")
                    } catch (e: Exception) {
                        android.util.Log.e("ThemeRepository", "Failed to load icon pack $packageName", e)
                    }
                }
            }
        }

        android.util.Log.d("ThemeRepository", "Total icon packs found: ${iconPacks.size}")

        val sortedPacks = iconPacks.sortedBy { it.label }

        listOf(IconPack("", "System Default", null)) + sortedPacks
    }
}
