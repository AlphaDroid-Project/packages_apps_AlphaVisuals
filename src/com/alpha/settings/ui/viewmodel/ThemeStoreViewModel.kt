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

package com.alpha.settings.ui.viewmodel

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alpha.settings.ui.data.model.IconPack
import com.alpha.settings.ui.data.model.Theme
import com.alpha.settings.ui.data.model.ThemeCategory
import com.alpha.settings.ui.data.model.ThemeInstallState
import com.alpha.settings.ui.data.repository.ThemeRepository
import com.alpha.settings.ui.engine.ThemeEngineProxy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import com.alpha.settings.ui.ui.ThemeStoreMainBrowsePolicy
import com.alpha.settings.ui.ui.buildOrderedMainStoreCategoryRows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.res.Configuration
import com.alpha.settings.ui.ui.components.UiStyleIds
import com.alpha.settings.ui.ui.model.UiStyleEditorState
import com.alpha.settings.ui.ui.model.UiStyleMode
import com.alpha.settings.ui.ui.model.UiStyleTuning
import com.alpha.settings.ui.systemui.SystemUiLivePreview
import com.android.internal.alpha.style.UserStyleSettings

/** Enabled / disabled / partial selection for a theme’s overlay targets (detail screen). */
enum class ThemeComponentEnablement {
    ENABLED,
    DISABLED,
    PARTIAL,
}

/** Queued theme enable/disable when a SystemUI restart may be needed; apply runs after the user confirms. */
sealed class DeferredSystemUiThemeOp {
    data class Apply(val theme: Theme) : DeferredSystemUiThemeOp()
    data class Disable(val theme: Theme) : DeferredSystemUiThemeOp()
}

class ThemeStoreViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ThemeStoreViewModel"
        private const val PREFS_NAME = "theme_store_prefs"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val MAX_SEARCH_HISTORY = 10

        private const val CAT_LOCKSCREEN_CLOCK_FONT = "android.theme.customization.lockscreen_clock_font"
        private const val CAT_SYSTEM_FONT = "android.theme.customization.font"
        private const val CAT_BACK_GESTURE = "android.theme.customization.back_gesture"
        private const val ICON_PACK_OVERLAY_PREFIX = "android.theme.customization.icon_pack."

        private fun categoryNeedsSystemUiRestart(componentId: String): Boolean =
            componentId == CAT_LOCKSCREEN_CLOCK_FONT ||
                componentId == CAT_SYSTEM_FONT ||
                componentId == CAT_BACK_GESTURE ||
                componentId.startsWith(ICON_PACK_OVERLAY_PREFIX)

        private fun anyCategoryNeedsSystemUiRestart(ids: Collection<String>): Boolean =
            ids.any(::categoryNeedsSystemUiRestart)
    }
    
    private val repository = ThemeRepository(application)
    private val themeEngineProxy = ThemeEngineProxy(application)
    private val sharedPrefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ThemeStoreUiState())
    val uiState: StateFlow<ThemeStoreUiState> = _uiState.asStateFlow()

    /**
     * Main theme list sections (category → themes), derived only from [uiState]. Collect this instead
     * of recomputing in Compose so [themeStates] emissions do not rebuild grouping/sorting.
     */
    val mainStoreBrowseCategoryRows: StateFlow<List<Pair<String, List<Theme>>>> =
        _uiState
            .map { state ->
                buildOrderedMainStoreCategoryRows(
                    filteredThemesForState(state).filter { ThemeStoreMainBrowsePolicy.includeThemeOnMainBrowse(it) },
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _themeStates = MutableStateFlow<Map<String, ThemeInstallState>>(emptyMap())
    val themeStates: StateFlow<Map<String, ThemeInstallState>> = _themeStates.asStateFlow()

    private val _enabledComponents = MutableStateFlow<Set<String>>(emptySet())
    val enabledComponents: StateFlow<Set<String>> = _enabledComponents.asStateFlow()

    private val _categoryThemes = MutableStateFlow<Map<String, String>>(emptyMap())
    val categoryThemesState: StateFlow<Map<String, String>> = _categoryThemes.asStateFlow()

    private val _pendingComponentChanges = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val pendingComponentChanges: StateFlow<Map<String, Set<String>>> = _pendingComponentChanges.asStateFlow()

    /** Non-null [Theme.id] while [applyPendingChanges] is running for that theme (disables Apply). */
    private val _applyingThemeId = MutableStateFlow<String?>(null)
    val applyingThemeId: StateFlow<String?> = _applyingThemeId.asStateFlow()

    /** Fires after a successful immediate apply that may need SystemUI restart (legacy dialog: restart only). */
    private val _systemUiRestartPrompt = Channel<Unit>(Channel.BUFFERED)
    val systemUiRestartPrompt = _systemUiRestartPrompt.receiveAsFlow()

    private val _deferredSystemUiThemeOp = MutableStateFlow<DeferredSystemUiThemeOp?>(null)
    val deferredSystemUiThemeOp: StateFlow<DeferredSystemUiThemeOp?> =
        _deferredSystemUiThemeOp.asStateFlow()

    /** After [finalizeDeferredSystemUiThemeOp] with restart; Main shows toast + restarts SystemUI. */
    private val _restartSystemUiAfterDeferred = Channel<Unit>(Channel.BUFFERED)
    val restartSystemUiAfterDeferred = _restartSystemUiAfterDeferred.receiveAsFlow()

    private fun offerSystemUiRestartIfNeeded(categoryIds: Collection<String>) {
        if (anyCategoryNeedsSystemUiRestart(categoryIds)) {
            _systemUiRestartPrompt.trySend(Unit)
        }
    }

    private fun offerSystemUiRestartForAppIconPackChange() {
        _systemUiRestartPrompt.trySend(Unit)
    }

    private val _initialComponentStates = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val initialComponentStates: StateFlow<Map<String, Set<String>>> = _initialComponentStates.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    /** Current [Settings.System] `ui_style` (QS / brightness decoration). */
    private val _systemUiStyleId = MutableStateFlow("system_default")
    val systemUiStyleId: StateFlow<String> = _systemUiStyleId.asStateFlow()

    private var uiStyleContentObserver: ContentObserver? = null

    init {
        loadThemes()
        loadIconPacks()
        loadThemedIconStyle()
        refreshComponentStates()
        loadSearchHistory()
        registerUiStyleObserver()
    }

    private fun registerUiStyleObserver() {
        val app = getApplication<Application>()
        val cr = app.contentResolver
        val handler = Handler(Looper.getMainLooper())
        val uri: Uri = Settings.System.getUriFor("ui_style")
        val obs = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                _systemUiStyleId.value = readUiStyleFromSettings()
            }
        }
        uiStyleContentObserver = obs
        cr.registerContentObserver(uri, false, obs, UserHandle.USER_ALL)
        _systemUiStyleId.value = readUiStyleFromSettings()
    }

    private fun readUiStyleFromSettings(): String {
        val v = Settings.System.getStringForUser(
            getApplication<Application>().contentResolver,
            "ui_style",
            UserHandle.USER_CURRENT,
        )
        return if (v.isNullOrEmpty()) "system_default" else v
    }

    override fun onCleared() {
        uiStyleContentObserver?.let { obs ->
            getApplication<Application>().contentResolver.unregisterContentObserver(obs)
        }
        uiStyleContentObserver = null
        super.onCleared()
    }
    
    private fun refreshComponentStates() {
        _enabledComponents.value = themeEngineProxy.getIconThemeTargets().toSet()
        _categoryThemes.value = themeEngineProxy.getCategoryThemes()
    }

    fun loadThemes(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.fetchThemes(forceRefresh).fold(
                onSuccess = { response ->
                    val uiStyleAuthors = response.uiStyles.associate { it.id to it.author }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            themes = response.themes,
                            categories = response.categories,
                            uiStyleAuthors = uiStyleAuthors,
                            filteredUiStyleIds = filteredUiStyleIdsForQuery(state.searchQuery, uiStyleAuthors),
                            error = null,
                        )
                    }
                    updateInstallStates(response.themes)
                    refreshComponentStates()
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load themes", error)
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to load installed overlays"
                        ) 
                    }
                }
            )
        }
    }

    fun checkInstallStates() {
        refreshComponentStates()
        val themes = _uiState.value.themes
        if (themes.isNotEmpty()) {
            updateInstallStates(themes)
        }
    }

    /** Clears all theme overlays and engine customization; refreshes UI state. */
    fun resetAllStylesToDefaults() {
        viewModelScope.launch {
            val categoriesBefore = themeEngineProxy.getCategoryThemes().keys
            val hadIconPack = themeEngineProxy.getIconPack() != null
            val ok = themeEngineProxy.resetAllStylesToDefaults()
            if (!ok) {
                Log.e(TAG, "resetAllStylesToDefaults failed")
                return@launch
            }
            _pendingComponentChanges.value = emptyMap()
            _initialComponentStates.value = emptyMap()
            refreshComponentStates()
            updateInstallStates(_uiState.value.themes)
            loadIconPacks()
            loadThemedIconStyle()
            if (categoriesBefore.isNotEmpty()) {
                offerSystemUiRestartIfNeeded(categoriesBefore)
            }
            if (hadIconPack) {
                offerSystemUiRestartForAppIconPackChange()
            }
        }
    }
    
    private fun updateInstallStates(themes: List<Theme>) {
        val categoryThemes = themeEngineProxy.getCategoryThemes()

        val states = themes.associate { theme ->
            val state = when {
                theme.isUnified && theme.overlays.isNotEmpty() -> {
                    val packageName = theme.overlays.first().packageName
                    val isInstalled = repository.isThemeInstalled(packageName)
                    val targets = theme.overlays.first().targets
                    val isAnyComponentActive = targets.any { target ->
                        categoryThemes[target] == packageName
                    }
                    
                    when {
                        isAnyComponentActive -> ThemeInstallState.Installed(theme.versionCode)
                        isInstalled -> ThemeInstallState.InstalledInactive(
                            repository.getInstalledVersionCode(packageName) ?: theme.versionCode
                        )
                        else -> ThemeInstallState.NotInstalled
                    }
                }
                else -> {
                    val installedOverlays = theme.overlays.filter { overlay ->
                        repository.isThemeInstalled(overlay.packageName)
                    }.map { it.componentId }.toSet()

                    val allActive = theme.overlays.all { overlay ->
                        categoryThemes[overlay.componentId] == overlay.packageName
                    }

                    when {
                        installedOverlays.isEmpty() -> ThemeInstallState.NotInstalled
                        installedOverlays.size == theme.overlays.size -> {
                            val firstOverlay = theme.overlays.first()
                            val installedVersion = repository.getInstalledVersionCode(firstOverlay.packageName)
                                ?: theme.versionCode
                            
                            if (allActive) {
                                ThemeInstallState.Installed(installedVersion)
                            } else {
                                ThemeInstallState.InstalledInactive(installedVersion)
                            }
                        }
                        else -> ThemeInstallState.InstalledInactive(theme.versionCode)
                    }
                }
            }
            theme.id to state
        }
        _themeStates.value = states
        
        val installedCount = states.values.count { 
            it is ThemeInstallState.Installed || it is ThemeInstallState.InstalledInactive 
        }
        Log.d("ThemeStoreViewModel", "Updated install states: $installedCount installed themes out of ${themes.size} total")
    }
    
    fun filterByCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategory = categoryId) }
    }
    
    fun searchThemes(query: String) {
        val filteredStyles = filteredUiStyleIdsForQuery(query, _uiState.value.uiStyleAuthors)
        _uiState.update { it.copy(searchQuery = query, filteredUiStyleIds = filteredStyles) }

        if (query.trim().length >= 2) {
            saveSearchQuery(query.trim())
        }
    }
    
    fun getFilteredThemes(ignoreSearchQuery: Boolean = false): List<Theme> =
        filteredThemesForState(_uiState.value, ignoreSearchQuery)

    private fun filteredThemesForState(
        state: ThemeStoreUiState,
        ignoreSearchQuery: Boolean = false,
    ): List<Theme> {
        var themes = state.themes
        state.selectedCategory?.let { category ->
            themes = themes.filter { it.category == category }
        }
        if (!ignoreSearchQuery && state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            themes = themes.filter { theme ->
                theme.name.lowercase().contains(query) ||
                    theme.description.lowercase().contains(query) ||
                    theme.author.lowercase().contains(query)
            }
        }
        return themes
    }

    private fun filteredUiStyleIdsForQuery(
        query: String,
        uiStyleAuthors: Map<String, String>,
    ): List<String> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return UiStyleIds.ALL.filter { id ->
            id.lowercase().contains(q) ||
                id.replace("_", " ").lowercase().contains(q) ||
                uiStyleAuthors[id]?.lowercase()?.contains(q) == true
        }
    }

    fun downloadTheme(theme: Theme) {
        applyTheme(theme)
    }

    fun installTheme(theme: Theme) {
        applyTheme(theme)
    }

    private fun categoryIdsForApplyRestartCheck(theme: Theme): List<String> {
        if (theme.overlays.isEmpty()) return emptyList()
        return if (theme.isUnified && theme.overlays.isNotEmpty()) {
            val overlay = theme.overlays.first()
            (_pendingComponentChanges.value[theme.id] ?: overlay.targets.toSet()).toList()
        } else {
            theme.overlays.map { it.componentId }
        }
    }

    private suspend fun applyThemeWork(theme: Theme) {
        if (theme.overlays.isEmpty()) return
        if (theme.isUnified && theme.overlays.isNotEmpty()) {
            val overlay = theme.overlays.first()
            val targetsToApply = _pendingComponentChanges.value[theme.id] ?: overlay.targets.toSet()
            if (targetsToApply.isNotEmpty()) {
                themeEngineProxy.applyThemeComponents(overlay.packageName, targetsToApply.toList())
            }
        } else {
            val categoryToPackage =
                theme.overlays.associate { it.componentId to it.packageName }
            themeEngineProxy.setCategoryThemes(categoryToPackage)
        }
        themeEngineProxy.notifyThemeChanged()
        _pendingComponentChanges.update { it - theme.id }
        _initialComponentStates.update { it - theme.id }
        refreshComponentStates()
        updateInstallStates(_uiState.value.themes)
    }

    private suspend fun disableThemeWork(theme: Theme) {
        _pendingComponentChanges.update { it - theme.id }
        _initialComponentStates.update { it - theme.id }
        for (overlay in theme.overlays) {
            themeEngineProxy.clearCategoryTheme(overlay.componentId)
        }
        themeEngineProxy.notifyThemeChanged()
        refreshComponentStates()
        updateInstallStates(_uiState.value.themes)
    }

    /**
     * Theme detail Enable/Disable: applies immediately unless a SystemUI restart may be needed;
     * then defers engine work until [finalizeDeferredSystemUiThemeOp].
     */
    fun requestToggleTheme(theme: Theme, enable: Boolean) {
        if (theme.overlays.isEmpty()) return
        val categories = if (enable) {
            categoryIdsForApplyRestartCheck(theme)
        } else {
            theme.overlays.map { it.componentId }
        }
        if (enable && categories.isEmpty()) return

        val needsDeferred = anyCategoryNeedsSystemUiRestart(categories)
        if (!needsDeferred) {
            viewModelScope.launch {
                if (enable) {
                    applyThemeWork(theme)
                    Log.d(TAG, "Applied theme (immediate): ${theme.name}")
                } else {
                    disableThemeWork(theme)
                    Log.d(TAG, "Disabled theme (immediate): ${theme.name}")
                }
            }
            return
        }
        _deferredSystemUiThemeOp.value = if (enable) {
            DeferredSystemUiThemeOp.Apply(theme)
        } else {
            DeferredSystemUiThemeOp.Disable(theme)
        }
    }

    /**
     * Runs deferred enable/disable after the user dismisses the restart dialog.
     * @param restartAfter if true, signals [restartSystemUiAfterDeferred] after work completes.
     */
    fun finalizeDeferredSystemUiThemeOp(restartAfter: Boolean) {
        val op = _deferredSystemUiThemeOp.value ?: return
        _deferredSystemUiThemeOp.value = null
        viewModelScope.launch {
            when (op) {
                is DeferredSystemUiThemeOp.Apply -> applyThemeWork(op.theme)
                is DeferredSystemUiThemeOp.Disable -> disableThemeWork(op.theme)
            }
            Log.d(TAG, "Deferred theme op completed (restartAfter=$restartAfter)")
            if (restartAfter) {
                _restartSystemUiAfterDeferred.trySend(Unit)
            }
        }
    }

    fun disableTheme(theme: Theme) {
        viewModelScope.launch {
            val categoryIds = theme.overlays.map { it.componentId }
            disableThemeWork(theme)
            offerSystemUiRestartIfNeeded(categoryIds)
            Log.d(TAG, "Disabled theme: ${theme.name}")
        }
    }

    fun uninstallTheme(theme: Theme, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            disableTheme(theme)
            refreshComponentStates()
            updateInstallStates(_uiState.value.themes)
            delay(200)
            onComplete?.invoke()
        }
    }

    fun applyTheme(theme: Theme) {
        if (theme.overlays.isEmpty()) {
            _themeStates.update { it + (theme.id to ThemeInstallState.NotInstalled) }
            return
        }

        viewModelScope.launch {
            val categoryIds = categoryIdsForApplyRestartCheck(theme)
            applyThemeWork(theme)
            offerSystemUiRestartIfNeeded(categoryIds)
            Log.d(TAG, "Applied theme: ${theme.name}")
        }
    }

    /** Engine state: which overlay component IDs are enabled with this theme’s packages. */
    private fun naturalEnabledComponentIds(theme: Theme): Set<String> {
        val map = _categoryThemes.value
        return theme.overlays
            .filter { o -> map[o.componentId] == o.packageName }
            .map { it.componentId }
            .toSet()
    }

    /** Pending selection, or natural engine state if no edits. */
    fun desiredEnabledComponentIds(theme: Theme): Set<String> {
        val themeId = theme.id
        val pending = _pendingComponentChanges.value[themeId]
        return pending ?: naturalEnabledComponentIds(theme)
    }

    /**
     * Whether all overlay rows are on, all off, or mixed — based on [desiredEnabledComponentIds]
     * (engine state, or unsaved selection when [pendingComponentChanges] has edits for this theme).
     */
    fun themeComponentEnablementStatus(theme: Theme): ThemeComponentEnablement {
        val ids = theme.overlays.map { it.componentId }.toSet()
        if (ids.isEmpty()) return ThemeComponentEnablement.DISABLED
        val desired = desiredEnabledComponentIds(theme).intersect(ids)
        return when {
            desired.isEmpty() -> ThemeComponentEnablement.DISABLED
            desired.size == ids.size -> ThemeComponentEnablement.ENABLED
            else -> ThemeComponentEnablement.PARTIAL
        }
    }

    fun setOverlayRowEnabled(theme: Theme, componentId: String, enabled: Boolean) {
        val themeId = theme.id
        require(theme.overlays.any { it.componentId == componentId }) { "Unknown component $componentId" }

        val natural = naturalEnabledComponentIds(theme)
        val current = desiredEnabledComponentIds(theme).toMutableSet()
        if (enabled) current.add(componentId) else current.remove(componentId)

        if (current == natural) {
            _pendingComponentChanges.update { it - themeId }
            _initialComponentStates.update { it - themeId }
        } else {
            _pendingComponentChanges.update { it + (themeId to current) }
        }
        Log.d(TAG, "Overlay row $componentId -> $enabled for ${theme.name}, pending=${current != natural}")
    }

    /** @deprecated Use [setOverlayRowEnabled]. */
    fun toggleComponent(theme: Theme, componentId: String, enabled: Boolean) {
        setOverlayRowEnabled(theme, componentId, enabled)
    }

    fun hasPendingChanges(themeId: String): Boolean {
        return _pendingComponentChanges.value.containsKey(themeId)
    }

    fun getPendingComponents(themeId: String): Set<String> {
        return _pendingComponentChanges.value[themeId] ?: emptySet()
    }

    fun applyPendingChanges(theme: Theme) {
        val themeId = theme.id
        val pending = _pendingComponentChanges.value[themeId] ?: return

        viewModelScope.launch {
            val overlayIds = theme.overlays.map { it.componentId }.toSet()
            val beforeEnabled = naturalEnabledComponentIds(theme)
            _applyingThemeId.value = themeId
            try {
                var ok = true
                if (theme.isUnified && theme.overlays.size == 1) {
                    val overlay = theme.overlays.first()
                    ok = themeEngineProxy.applyThemeComponents(overlay.packageName, pending.toList())
                } else {
                    for (o in theme.overlays) {
                        if (o.componentId in pending) {
                            ok = themeEngineProxy.setCategoryTheme(o.componentId, o.packageName) && ok
                        } else {
                            ok = themeEngineProxy.clearCategoryTheme(o.componentId) && ok
                        }
                    }
                }
                if (ok) {
                    themeEngineProxy.notifyThemeChanged()
                    _pendingComponentChanges.update { it - themeId }
                    _initialComponentStates.update { it - themeId }
                    refreshComponentStates()
                    updateInstallStates(_uiState.value.themes)
                    val changed = overlayIds.filter { id ->
                        beforeEnabled.contains(id) != pending.contains(id)
                    }
                    offerSystemUiRestartIfNeeded(changed)
                    Log.d(TAG, "Applied pending for ${theme.name}: $pending")
                } else {
                    Log.e(TAG, "Failed to apply pending for ${theme.name}")
                }
            } finally {
                if (_applyingThemeId.value == themeId) {
                    _applyingThemeId.value = null
                }
            }
        }
    }

    fun isComponentEnabled(componentId: String): Boolean {
        return _enabledComponents.value.contains(componentId)
    }

    fun getEnabledComponents(): List<String> {
        return themeEngineProxy.getIconThemeTargets()
    }

    fun applyThemeComponents(theme: Theme, selectedCategories: List<String>) {
        if (!theme.isUnified || theme.overlays.isEmpty()) return

        viewModelScope.launch {
            val packageName = theme.overlays.first().packageName

            val success = themeEngineProxy.applyThemeComponents(packageName, selectedCategories)
            if (success) {
                Log.d(TAG, "Applied ${selectedCategories.size} categories from ${theme.name}")
                offerSystemUiRestartIfNeeded(selectedCategories)
                refreshComponentStates()
                updateInstallStates(_uiState.value.themes)
            } else {
                Log.e(TAG, "Failed to apply theme components from ${theme.name}")
            }
        }
    }

    fun getCategoryThemes(): Map<String, String> {
        return _categoryThemes.value
    }

    fun isCategoryFromTheme(category: String, packageName: String): Boolean {
        return themeEngineProxy.getCategoryTheme(category) == packageName
    }

    fun getCategoryThemePackage(category: String): String? {
        return themeEngineProxy.getCategoryTheme(category)
    }
    
    fun clearCategoryTheme(category: String) {
        viewModelScope.launch {
            themeEngineProxy.clearCategoryTheme(category)
            refreshComponentStates()
            updateInstallStates(_uiState.value.themes)
        }
    }

    fun clearError(themeId: String) {
        updateInstallStates(_uiState.value.themes)
    }

    fun loadIconPacks() {
        viewModelScope.launch {
            val packs = repository.getInstalledIconPacks()
            val currentPack = themeEngineProxy.getIconPack()
            
            _uiState.update { 
                it.copy(
                    iconPacks = packs,
                    currentIconPack = currentPack
                ) 
            }
        }
    }

    fun applyIconPack(packageName: String) {
        viewModelScope.launch {
            val success = if (packageName.isEmpty()) {
                themeEngineProxy.clearIconPack()
            } else {
                themeEngineProxy.setIconPack(packageName)
            }

            if (success) {
                _uiState.update { it.copy(currentIconPack = if (packageName.isEmpty()) null else packageName) }
                offerSystemUiRestartForAppIconPackChange()
            }
        }
    }
    
    fun loadThemedIconStyle() {
        viewModelScope.launch {
            val style = themeEngineProxy.getThemedIconStyle()
            val enabled = themeEngineProxy.isThemedIconsEnabled()
            _uiState.update { 
                it.copy(
                    themedIconStyle = style,
                    themedIconsEnabled = enabled
                ) 
            }
        }
    }

    fun setThemedIconStyle(style: String) {
        viewModelScope.launch {
            themeEngineProxy.setThemedIconStyle(style)
            _uiState.update { it.copy(themedIconStyle = style) }
        }
    }

    fun setThemedIconsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themeEngineProxy.setThemedIconsEnabled(enabled)
            _uiState.update { it.copy(themedIconsEnabled = enabled) }
            
            if (enabled) {
                themeEngineProxy.setIconPack("")
                _uiState.update { it.copy(currentIconPack = null) }
            }
        }
    }
    
    private fun loadSearchHistory() {
        val historyJson = sharedPrefs.getString(KEY_SEARCH_HISTORY, null)
        if (historyJson != null) {
            try {
                val jsonArray = org.json.JSONArray(historyJson)
                val history = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    history.add(jsonArray.getString(i))
                }
                _searchHistory.value = history
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load search history", e)
                _searchHistory.value = emptyList()
            }
        }
    }
    
    private fun saveSearchQuery(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()
        
        currentHistory.remove(query)
        
        currentHistory.add(0, query)
        
        if (currentHistory.size > MAX_SEARCH_HISTORY) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _searchHistory.value = currentHistory
        
        try {
            val jsonArray = org.json.JSONArray(currentHistory)
            sharedPrefs.edit()
                .putString(KEY_SEARCH_HISTORY, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save search history", e)
        }
    }
    
    fun removeSearchHistoryItem(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.remove(query)
        _searchHistory.value = currentHistory
        
        try {
            val jsonArray = org.json.JSONArray(currentHistory)
            sharedPrefs.edit()
                .putString(KEY_SEARCH_HISTORY, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update search history", e)
        }
    }
    
    fun getThemeEngineProxy(): ThemeEngineProxy = themeEngineProxy

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        sharedPrefs.edit()
            .remove(KEY_SEARCH_HISTORY)
            .apply()
    }

    private val _uiStyleEditorState = MutableStateFlow(
        UiStyleEditorState(styleId = UiStyleIds.SYSTEM_DEFAULT)
    )
    val uiStyleEditorState: StateFlow<UiStyleEditorState> = _uiStyleEditorState.asStateFlow()

    private fun currentSystemUiMode(): UiStyleMode {
        val uiModeMask = getApplication<Application>().resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        return if (uiModeMask == Configuration.UI_MODE_NIGHT_YES) {
            UiStyleMode.DARK
        } else {
            UiStyleMode.LIGHT
        }
    }

    private fun uiStyleParamsKey(styleId: String): String? {
        return when (UiStyleIds.normalize(styleId)) {
            UiStyleIds.OUTLINE -> "ui_style_outline_params"
            UiStyleIds.NEON -> "ui_style_neon_params"
            UiStyleIds.BEVEL -> "ui_style_bevel_params"
            UiStyleIds.GRADIENT -> "ui_style_gradient_params"
            UiStyleIds.REFLECTIVE -> "ui_style_reflective_params"
            UiStyleIds.SLASH -> "ui_style_slash_params"
            UiStyleIds.AEROGEL -> "ui_style_aerogel_params"
            UiStyleIds.METALLIC -> "ui_style_metallic_params"
            else -> null
        }
    }

    private fun parseUiStyleSettingsForMode(
        combined: String?,
        isDark: Boolean,
    ): UserStyleSettings {
        if (combined.isNullOrBlank()) {
            return UserStyleSettings.DEFAULT
        }

        val prefix = if (isDark) "dark:" else "light:"
        return combined
            .split("|")
            .firstOrNull { it.startsWith(prefix) }
            ?.substring(prefix.length)
            ?.let(UserStyleSettings::fromString)
            ?: UserStyleSettings.DEFAULT
    }

    private fun readUiStyleEditorState(styleId: String): UiStyleEditorState {
        val normalizedId = UiStyleIds.normalize(styleId)
        val paramsKey = uiStyleParamsKey(normalizedId)
        val resolver = getApplication<Application>().contentResolver
        val combined = paramsKey?.let {
            Settings.System.getStringForUser(resolver, it, UserHandle.USER_CURRENT)
        }

        return UiStyleEditorState(
            styleId = normalizedId,
            light = UiStyleTuning.fromUserStyleSettings(
                parseUiStyleSettingsForMode(combined, isDark = false)
            ),
            dark = UiStyleTuning.fromUserStyleSettings(
                parseUiStyleSettingsForMode(combined, isDark = true)
            ),
        )
    }

    fun loadUiStyleEditor(styleId: String) {
        _uiStyleEditorState.value = readUiStyleEditorState(styleId)
    }

    fun applyUiStyle(styleId: String) {
        val normalizedId = UiStyleIds.normalize(styleId)
        Settings.System.putStringForUser(
            getApplication<Application>().contentResolver,
            "ui_style",
            normalizedId,
            UserHandle.USER_CURRENT,
        )
        _systemUiStyleId.value = normalizedId
    }

    /** Edits tuning for the current system day/night mode ([currentSystemUiMode]). */
    fun updateUiStyleTuning(transform: (UiStyleTuning) -> UiStyleTuning) {
        val mode = currentSystemUiMode()
        val current = _uiStyleEditorState.value
        val updated = when (mode) {
            UiStyleMode.LIGHT -> current.copy(light = transform(current.light))
            UiStyleMode.DARK -> current.copy(dark = transform(current.dark))
        }
        _uiStyleEditorState.value = updated
        persistUiStyleEditorState(updated)
    }

    /** Resets tuning for the current system day/night mode only. */
    fun resetUiStyleTuningForCurrentMode() {
        updateUiStyleTuning { UiStyleTuning.Default }
    }

    fun resetUiStyle(styleId: String = _uiStyleEditorState.value.styleId) {
        val normalizedId = UiStyleIds.normalize(styleId)
        val paramsKey = uiStyleParamsKey(normalizedId)

        if (paramsKey != null) {
            Settings.System.putStringForUser(
                getApplication<Application>().contentResolver,
                paramsKey,
                null,
                UserHandle.USER_CURRENT,
            )
        }

        _uiStyleEditorState.value = _uiStyleEditorState.value.copy(
            styleId = normalizedId,
            light = UiStyleTuning.Default,
            dark = UiStyleTuning.Default,
        )
    }

    private fun persistUiStyleEditorState(state: UiStyleEditorState) {
        val paramsKey = uiStyleParamsKey(state.styleId) ?: return

        val light = state.light.toUserStyleSettings()
        val dark = state.dark.toUserStyleSettings()
        val combined = "light:${light}|dark:${dark}"

        Settings.System.putStringForUser(
            getApplication<Application>().contentResolver,
            paramsKey,
            combined,
            UserHandle.USER_CURRENT,
        )
        SystemUiLivePreview.pulseVolumeDialog(getApplication())
    }
}

data class ThemeStoreUiState(
    val isLoading: Boolean = false,
    val themes: List<Theme> = emptyList(),
    val categories: List<ThemeCategory> = emptyList(),
    /** From overlay catalog `ui_styles`; keyed by style id. */
    val uiStyleAuthors: Map<String, String> = emptyMap(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val iconPacks: List<IconPack> = emptyList(),
    val currentIconPack: String? = null,
    val themedIconStyle: String = ThemeEngineProxy.Companion.ThemedIconStyle.AXION,
    val themedIconsEnabled: Boolean = false,
    val filteredUiStyleIds: List<String> = emptyList()
)
