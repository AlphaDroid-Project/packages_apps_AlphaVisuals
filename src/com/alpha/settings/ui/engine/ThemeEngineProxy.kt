/*
 * Copyright (C) 2025-2026 AxionOS Project
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

package com.alpha.settings.ui.engine

import android.content.Context
import android.content.om.OverlayManager
import android.content.res.ThemeEngine
import android.os.Handler
import android.os.HandlerThread
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import org.json.JSONObject

class ThemeEngineProxy(private val context: Context) {

    companion object {
        private const val TAG = "ThemeEngineProxy"

        private val workerThread: HandlerThread by lazy {
            HandlerThread("ThemeEngineProxy-worker").apply { start() }
        }
        private val workerHandler: Handler by lazy { Handler(workerThread.looper) }

        /** Serialized enable after [applyOverlays] delay; cancelled when a new apply is scheduled. */
        @Volatile
        private var pendingOverlayEnable: Runnable? = null

        private val pendingOverlayEnableLock = Any()

        const val SETTINGS_THEME_ENGINE_DATA = "theme_engine_data"

        private val SYNCED_OVERLAY_CATEGORIES = setOf(
            "android.theme.customization.icon_pack.android",
            "android.theme.customization.icon_pack.systemui",
            "android.theme.customization.icon_pack.settings",
            "android.theme.customization.icon_pack.launcher",
            "android.theme.customization.icon_pack.themepicker",
            "android.theme.customization.back_gesture",
            "android.theme.customization.charging_animation",
            "android.theme.customization.battery_style",
            "android.theme.customization.wifi_icon",
            "android.theme.customization.signal_icon",
            "android.theme.customization.font",
            "android.theme.customization.lockscreen_clock_font",
        )

        private const val OVERLAY_CATEGORY_WIFI = "android.theme.customization.wifi_icon"
        private const val OVERLAY_CATEGORY_SIGNAL = "android.theme.customization.signal_icon"
        private const val OVERLAY_CATEGORY_BATTERY = "android.theme.customization.battery_style"
        private const val OVERLAY_CATEGORY_CHARGING = "android.theme.customization.charging_animation"

        object Category {
            const val STATUSBAR_WIFI = "statusbar_wifi"
            const val STATUSBAR_SIGNAL = "statusbar_signal"
            const val ANDROID = "android"
            const val SYSTEMUI = "systemui"
            const val UI_VOLUME = "ui_volume"
            const val ICON_PACK = "icon_pack"
        }

        object ThemedIconStyle {
            const val AXION = "axion"
            const val AOSP = "aosp"
        }

        const val THEMED_ICON_STYLE_SETTING = "themed_icon_style"
        const val THEMED_ICONS_ENABLED_SETTING = "themed_icons"
    }

    fun getThemeConfig(): ThemeEngineConfig {
        return try {
            val json = Settings.Secure.getString(context.contentResolver, SETTINGS_THEME_ENGINE_DATA)
            if (json.isNullOrBlank()) ThemeEngineConfig() else parseConfig(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse theme config", e)
            ThemeEngineConfig()
        }
    }

    private fun saveThemeConfig(config: ThemeEngineConfig): Boolean {
        return try {
            val json = serializeConfig(config)
            Settings.Secure.putString(context.contentResolver, SETTINGS_THEME_ENGINE_DATA, json)
            Log.d(TAG, "Saved theme config: $json")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save theme config", e)
            false
        }
    }

    private fun parseConfig(jsonStr: String): ThemeEngineConfig {
        return try {
            val json = JSONObject(jsonStr)
            val version = json.optInt("version", 1)
            val iconTheme = if (json.has("systemThemeIcons") && !json.isNull("systemThemeIcons"))
                json.getString("systemThemeIcons") else null
            val iconThemeTargets = mutableListOf<String>()
            val targetsArr = json.optJSONArray("systemThemeIconTargets")
            if (targetsArr != null) {
                for (i in 0 until targetsArr.length()) {
                    iconThemeTargets.add(targetsArr.getString(i))
                }
            }

            val categoryThemes = mutableMapOf<String, String>()
            val catThemesObj = json.optJSONObject("categoryThemes")
            catThemesObj?.keys()?.forEach { key ->
                val pkgName = catThemesObj.optString(key)
                if (pkgName.isNotBlank()) categoryThemes[key] = pkgName
            }

            val themesMap = mutableMapOf<String, ThemeCategoryConfig>()
            val themesObj = json.optJSONObject("themes")
            themesObj?.keys()?.forEach { key ->
                val catObj = themesObj.getJSONObject(key)
                val enabled = catObj.optBoolean("enabled", false)
                val pkgName = if (catObj.has("packageName") && !catObj.isNull("packageName"))
                    catObj.getString("packageName") else null
                val styleId = if (catObj.has("styleId") && !catObj.isNull("styleId"))
                    catObj.getString("styleId") else null
                themesMap[key] = ThemeCategoryConfig(enabled, pkgName, styleId)
            }

            ThemeEngineConfig(version, themesMap, iconTheme, iconThemeTargets, categoryThemes)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing config JSON", e)
            ThemeEngineConfig()
        }
    }

    private fun serializeConfig(config: ThemeEngineConfig): String {
        val json = JSONObject()
        json.put("version", config.version)
        json.put("systemThemeIcons", config.iconTheme)
        val targetsArr = org.json.JSONArray()
        config.iconThemeTargets.forEach { targetsArr.put(it) }
        json.put("systemThemeIconTargets", targetsArr)

        val catThemesObj = JSONObject()
        config.categoryThemes.forEach { (key, value) -> catThemesObj.put(key, value) }
        json.put("categoryThemes", catThemesObj)

        val themesObj = JSONObject()
        config.themes.forEach { (key, value) ->
            val catObj = JSONObject()
            catObj.put("enabled", value.enabled)
            catObj.put("packageName", value.packageName)
            catObj.put("styleId", value.styleId)
            themesObj.put(key, catObj)
        }
        json.put("themes", themesObj)

        return json.toString()
    }

    fun setIconPack(packageName: String): Boolean {
        val config = getThemeConfig()
        val updatedThemes = config.themes.toMutableMap().apply {
            put(Category.ICON_PACK, ThemeCategoryConfig(enabled = true, packageName = packageName))
        }
        return saveThemeConfig(config.copy(themes = updatedThemes))
    }

    fun getIconPack(): String? {
        val entry = getThemeConfig().themes[Category.ICON_PACK]
        return if (entry?.enabled == true) entry.packageName else null
    }

    fun clearIconPack(): Boolean {
        val config = getThemeConfig()
        val updatedThemes = config.themes.toMutableMap().apply {
            put(Category.ICON_PACK, ThemeCategoryConfig(enabled = false))
        }
        return saveThemeConfig(config.copy(themes = updatedThemes))
    }

    fun getEnabledThemes(): Map<String, String> {
        return getThemeConfig().themes
            .filter { it.value.enabled && it.value.packageName != null }
            .mapValues { it.value.packageName!! }
    }

    fun disableThemeOverlays(categories: List<String>): Boolean {
        val config = getThemeConfig()
        val updatedThemes = config.themes.toMutableMap().apply {
            categories.forEach { put(it, ThemeCategoryConfig(enabled = false)) }
        }
        return saveThemeConfig(config.copy(themes = updatedThemes))
    }

    fun setIconThemeWithTargets(packageName: String, targets: List<String>): Boolean {
        val config = getThemeConfig()
        val newCategoryThemes = config.categoryThemes.toMutableMap().apply {
            targets.forEach { put(it, packageName) }
        }
        val saved = saveThemeConfig(config.copy(
            iconTheme = packageName,
            iconThemeTargets = targets,
            categoryThemes = newCategoryThemes
        ))
        if (saved) syncOverlayPackagesSettings(newCategoryThemes)
        return saved
    }

    fun getIconTheme(): String? = getThemeConfig().iconTheme

    fun getIconThemeTargets(): List<String> = getThemeConfig().iconThemeTargets

    fun clearIconTheme(): Boolean {
        val config = getThemeConfig()
        return saveThemeConfig(config.copy(iconTheme = null, iconThemeTargets = emptyList()))
    }

    fun setCategoryTheme(category: String, packageName: String): Boolean {
        val config = getThemeConfig()
        val oldPackage = config.categoryThemes[category]
        val updated = buildUpdatedCategoryConfig(config, config.categoryThemes + (category to packageName))
        val saved = saveThemeConfig(updated)
        if (saved) {
            workerHandler.post {
                applyOverlays(packageName, setOfNotNull(oldPackage?.takeIf { it != packageName }))
                syncOverlayPackagesSettings(updated.categoryThemes)
                syncThemeEngineStatusCategories(updated.categoryThemes)
            }
        }
        return saved
    }

    fun clearCategoryTheme(category: String): Boolean {
        val config = getThemeConfig()
        val oldPackage = config.categoryThemes[category]
        val updated = buildUpdatedCategoryConfig(config, config.categoryThemes - category)
        val saved = saveThemeConfig(updated)
        if (saved) {
            workerHandler.post {
                applyOverlays(null, setOfNotNull(oldPackage))
                syncOverlayPackagesSettings(updated.categoryThemes)
                syncThemeEngineStatusCategories(updated.categoryThemes)
            }
        }
        return saved
    }

    fun applyThemeComponents(packageName: String, categories: List<String>): Boolean {
        val config = getThemeConfig()
        val newCategoryThemes = config.categoryThemes.toMutableMap().apply {
            entries.removeAll { it.value == packageName && it.key !in categories }
            categories.forEach { put(it, packageName) }
        }
        val oldPackages = categories
            .mapNotNull { config.categoryThemes[it] }
            .filter { it != packageName }
            .toSet()
        val updated = buildUpdatedCategoryConfig(config, newCategoryThemes)
        val saved = saveThemeConfig(updated)
        if (saved) {
            syncOverlayPackagesSettings(updated.categoryThemes)
            workerHandler.post {
                applyOverlays(packageName, oldPackages)
                syncThemeEngineStatusCategories(updated.categoryThemes)
            }
        }
        return saved
    }

    fun clearCategoryThemesForPackage(packageName: String): Boolean {
        val config = getThemeConfig()
        val updated = buildUpdatedCategoryConfig(config, config.categoryThemes.filterValues { it != packageName })
        val saved = saveThemeConfig(updated)
        if (saved) {
            workerHandler.post {
                applyOverlays(null, setOf(packageName))
                syncOverlayPackagesSettings(updated.categoryThemes)
                syncThemeEngineStatusCategories(updated.categoryThemes)
            }
        }
        return saved
    }

    fun getCategoryTheme(category: String): String? = getThemeConfig().categoryThemes[category]

    fun getCategoryThemes(): Map<String, String> = getThemeConfig().categoryThemes

    private fun buildUpdatedCategoryConfig(config: ThemeEngineConfig, newCategoryThemes: Map<String, String>): ThemeEngineConfig {
        val uniquePackages = newCategoryThemes.values.toSet()
        return config.copy(
            iconTheme = if (uniquePackages.size == 1) uniquePackages.first() else null,
            iconThemeTargets = newCategoryThemes.keys.toList(),
            categoryThemes = newCategoryThemes
        )
    }

    private fun applyOverlays(packageName: String?, oldPackages: Set<String>) {
        try {
            synchronized(pendingOverlayEnableLock) {
                pendingOverlayEnable?.let { workerHandler.removeCallbacks(it) }
            }
            val om = context.getSystemService(OverlayManager::class.java) ?: return
            for (oldPkg in oldPackages) {
                try {
                    om.setEnabled(oldPkg, false, UserHandle.SYSTEM)
                    Log.d(TAG, "disabled overlay: $oldPkg")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to disable overlay: $oldPkg", e)
                }
            }
            if (packageName == null) {
                synchronized(pendingOverlayEnableLock) {
                    pendingOverlayEnable = null
                }
                return
            }
            val enableTask = object : Runnable {
                override fun run() {
                    try {
                        om.setEnabledExclusiveInCategory(packageName, UserHandle.SYSTEM)
                        Log.d(TAG, "enabled overlay: $packageName")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to enable overlay: $packageName", e)
                    } finally {
                        synchronized(pendingOverlayEnableLock) {
                            if (pendingOverlayEnable === this) {
                                pendingOverlayEnable = null
                            }
                        }
                    }
                }
            }
            synchronized(pendingOverlayEnableLock) {
                pendingOverlayEnable = enableTask
            }
            workerHandler.postDelayed(enableTask, 500)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply overlays for $packageName", e)
        }
    }

    private fun syncOverlayPackagesSettings(categoryThemes: Map<String, String>) {
        try {
            val resolver = context.contentResolver
            val current = Settings.Secure.getStringForUser(
                resolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                UserHandle.myUserId()
            )
            val json = if (current.isNullOrBlank()) JSONObject() else JSONObject(current)

            SYNCED_OVERLAY_CATEGORIES.forEach { category -> json.remove(category) }
            categoryThemes.forEach { (category, packageName) ->
                if (category in SYNCED_OVERLAY_CATEGORIES) json.put(category, packageName)
            }

            Settings.Secure.putStringForUser(
                resolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                json.toString(),
                UserHandle.myUserId()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync overlay packages settings", e)
        }
    }

    private fun syncThemeEngineStatusCategories(categoryThemes: Map<String, String>) {
        try {
            val current = Settings.Secure.getString(
                context.contentResolver,
                SETTINGS_THEME_ENGINE_DATA
            )
            val root = if (current.isNullOrBlank()) JSONObject() else JSONObject(current)
            val themes = root.optJSONObject("themes") ?: JSONObject()
            val categoryObj = root.optJSONObject("categoryThemes") ?: JSONObject()

            syncStatusCategory(
                overlayCategory = OVERLAY_CATEGORY_WIFI,
                engineKey = "wifi",
                categoryThemes = categoryThemes,
                themes = themes,
                categoryObj = categoryObj
            )
            syncStatusCategory(
                overlayCategory = OVERLAY_CATEGORY_SIGNAL,
                engineKey = "signal",
                categoryThemes = categoryThemes,
                themes = themes,
                categoryObj = categoryObj
            )
            syncStatusCategory(
                overlayCategory = OVERLAY_CATEGORY_BATTERY,
                engineKey = ThemeEngine.CATEGORY_BATTERY_STYLE,
                categoryThemes = categoryThemes,
                themes = themes,
                categoryObj = categoryObj
            )
            syncChargingAnimationCategory(
                categoryThemes = categoryThemes,
                themes = themes,
                categoryObj = categoryObj
            )

            root.put("themes", themes)
            root.put("categoryThemes", categoryObj)
            if (!root.has("version")) root.put("version", 1)
            Settings.Secure.putString(context.contentResolver, SETTINGS_THEME_ENGINE_DATA, root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync theme_engine_data status categories", e)
        }
    }

    private fun syncStatusCategory(
        overlayCategory: String,
        engineKey: String,
        categoryThemes: Map<String, String>,
        themes: JSONObject,
        categoryObj: JSONObject
    ) {
        val pkg = categoryThemes[overlayCategory]
        if (pkg.isNullOrBlank()) {
            themes.remove(engineKey)
            categoryObj.remove(engineKey)
            categoryObj.remove("statusbar_$engineKey")
            return
        }

        val entry = JSONObject().apply {
            put("enabled", true)
            put("packageName", pkg)
        }
        themes.put(engineKey, entry)
        categoryObj.put(engineKey, pkg)
        categoryObj.put("statusbar_$engineKey", pkg)
    }

    /**
     * Charging animation uses [ThemeEngine.CATEGORY_CHARGING_ANIMATION] only (no `statusbar_` alias).
     */
    private fun syncChargingAnimationCategory(
        categoryThemes: Map<String, String>,
        themes: JSONObject,
        categoryObj: JSONObject
    ) {
        val engineKey = ThemeEngine.CATEGORY_CHARGING_ANIMATION
        val pkg = categoryThemes[OVERLAY_CATEGORY_CHARGING]
        if (pkg.isNullOrBlank()) {
            themes.remove(engineKey)
            categoryObj.remove(engineKey)
            return
        }
        val entry = JSONObject().apply {
            put("enabled", true)
            put("packageName", pkg)
        }
        themes.put(engineKey, entry)
        categoryObj.put(engineKey, pkg)
    }

    fun setThemedIconStyle(style: String) {
        Settings.Secure.putString(context.contentResolver, THEMED_ICON_STYLE_SETTING, style)
    }

    fun getThemedIconStyle(): String {
        return Settings.Secure.getString(context.contentResolver, THEMED_ICON_STYLE_SETTING)
            ?: ThemedIconStyle.AXION
    }

    fun setThemedIconsEnabled(enabled: Boolean) {
        Settings.Secure.putInt(context.contentResolver, THEMED_ICONS_ENABLED_SETTING, if (enabled) 1 else 0)
    }

    fun isThemedIconsEnabled(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, THEMED_ICONS_ENABLED_SETTING, 0) == 1
    }

    fun getAvailableOverlays(category: String): List<String> {
        return try {
            ThemeEngine.getInstance(context)?.getAvailableOverlays(category) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available overlays", e)
            emptyList()
        }
    }

    fun notifyThemeChanged() {
        try {
            ThemeEngine.getInstance(context)?.notifyThemeChanged(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify theme changed", e)
        }
    }

    /**
     * Clears theme engine settings and disables all overlay packages referenced by the previous config.
     */
    fun resetAllStylesToDefaults(): Boolean {
        val config = getThemeConfig()
        val packagesToDisable = LinkedHashSet<String>()
        packagesToDisable.addAll(config.categoryThemes.values)
        config.themes.values.forEach { cat ->
            if (cat.enabled && !cat.packageName.isNullOrBlank()) {
                packagesToDisable.add(cat.packageName!!)
            }
        }
        val saved = saveThemeConfig(ThemeEngineConfig())
        if (!saved) return false
        workerHandler.post {
            applyOverlays(null, packagesToDisable)
            syncOverlayPackagesSettings(emptyMap())
            syncThemeEngineStatusCategories(emptyMap())
            notifyThemeChanged()
        }
        return true
    }
}

data class ThemeEngineConfig(
    val version: Int = 1,
    val themes: Map<String, ThemeCategoryConfig> = emptyMap(),
    val iconTheme: String? = null,
    val iconThemeTargets: List<String> = emptyList(),
    val categoryThemes: Map<String, String> = emptyMap(),
)

data class ThemeCategoryConfig(
    val enabled: Boolean = false,
    val packageName: String? = null,
    val styleId: String? = null
)
