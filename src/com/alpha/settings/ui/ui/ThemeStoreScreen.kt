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

package com.alpha.settings.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpha.settings.ui.R
import com.alpha.settings.ui.data.model.Theme
import com.alpha.settings.ui.data.model.ThemeInstallState
import com.alpha.settings.ui.ui.components.PreviewDimensions
import com.alpha.settings.ui.ui.components.ThemeStoreItemPreview
import com.alpha.settings.ui.viewmodel.ThemeStoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStoreScreen(
    viewModel: ThemeStoreViewModel,
    onThemeClick: (Theme) -> Unit,
    onNavigateToInstalledComponents: () -> Unit = {},
    onNavigateToUiStyles: () -> Unit = {},
    onNavigateToMonetSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeStates by viewModel.themeStates.collectAsStateWithLifecycle()
    val browseRows by viewModel.mainStoreBrowseCategoryRows.collectAsStateWithLifecycle()
    val firstSectionMatch = remember(uiState.searchQuery) {
        firstSectionSearchMatch(uiState.searchQuery)
    }

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            text = {
                Text(stringResource(R.string.theme_store_reset_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetAllStylesToDefaults()
                    }
                ) {
                    Text(stringResource(R.string.theme_store_reset_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.theme_store_reset_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alpha_visuals_title), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToInstalledComponents) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.installed_components))
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            Icons.Outlined.RestartAlt,
                            contentDescription = stringResource(R.string.theme_store_reset)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(paddingValues))
            uiState.error != null -> ErrorState(uiState.error!!, { viewModel.loadThemes(true) }, Modifier.padding(paddingValues))
            else -> {
                val scheme = MaterialTheme.colorScheme
                val mainBackdrop = remember(
                    scheme.background,
                    scheme.primaryContainer,
                    scheme.surface,
                    scheme.tertiaryContainer,
                ) {
                    Brush.verticalGradient(
                        colors = listOf(
                            scheme.background,
                            lerp(scheme.background, scheme.primaryContainer, 0.22f),
                            lerp(scheme.surface, scheme.tertiaryContainer, 0.12f),
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(mainBackdrop),
                    )
                    Column(Modifier.fillMaxSize()) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.searchThemes(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            placeholder = { Text(stringResource(R.string.search_themes)) },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                        )
                        Spacer(modifier = Modifier.height(PreviewDimensions.ThemeStoreSearchToFirstRowSpacing))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(
                                bottom = PreviewDimensions.ThemeStoreSearchToFirstRowSpacing,
                            ),
                            verticalArrangement = Arrangement.spacedBy(
                                PreviewDimensions.ThemeStoreSectionSpacing,
                            ),
                        ) {
                            if (firstSectionMatch.hasAnyMatch || uiState.searchQuery.isBlank()) {
                                item(key = "themes_section") {
                                    ThemesEntrySection(
                                        onMonetClick = onNavigateToMonetSettings,
                                        onUiStylesClick = onNavigateToUiStyles,
                                    )
                                }
                            }
                            if (browseRows.isEmpty()) {
                                item(key = "empty_state") {
                                    EmptyState(
                                        isSearching = uiState.searchQuery.isNotBlank(),
                                        firstSectionMatchLabel = firstSectionMatch.label,
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                            .fillMaxWidth(),
                                    )
                                }
                            } else {
                                items(
                                    browseRows,
                                    key = { (category, _) -> category },
                                    contentType = { "category_row" },
                                ) { (category, themes) ->
                                    val themeIdsKey = remember(themes) {
                                        themes.joinToString(separator = "\u0000") { it.id }
                                    }
                                    val anyActiveInSection = remember(themeIdsKey, themeStates) {
                                        themes.any {
                                            themeStates[it.id] is ThemeInstallState.Installed
                                        }
                                    }
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = themeSectionTitle(category),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (anyActiveInSection) {
                                                lerp(scheme.onSurface, scheme.primary, 0.28f)
                                            } else {
                                                scheme.onSurface
                                            },
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 4.dp,
                                            ),
                                        )
                                        Spacer(
                                            modifier = Modifier.height(
                                                PreviewDimensions.ThemeStoreSectionLabelToCardsSpacing,
                                            ),
                                        )
                                        MainStoreThemeStrip(
                                            themes = themes,
                                            themeStates = themeStates,
                                            onThemeClick = onThemeClick,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryThemesScreen(
    categoryId: String,
    viewModel: ThemeStoreViewModel,
    onThemeClick: (Theme) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeStates by viewModel.themeStates.collectAsStateWithLifecycle()

    val category = uiState.categories.find { it.id == categoryId }
    val categoryName = category?.name ?: "Themes"

    val categoryThemes = remember(categoryId, uiState.themes) {
        uiState.themes.filter { it.category == categoryId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadThemes(forceRefresh = true) }
                    )
                }
                categoryThemes.isEmpty() -> {
                    EmptyState(isSearching = false)
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categoryThemes, key = { it.id }) { theme ->
                            ThemeRow(
                                theme = theme,
                                isActive = themeStates[theme.id] is ThemeInstallState.Installed,
                                onClick = { onThemeClick(theme) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun themeSectionTitle(category: String): String {
    val c = PreviewDimensions.normalizeStoreCategoryForPreview(category)
    return when (c) {
        "ui_style" -> stringResource(R.string.ui_style_preview_section_title)
        "back_gesture" -> stringResource(R.string.section_back_gesture)
        "charging_animation" -> stringResource(R.string.section_charging_animation)
        "battery_style" -> stringResource(R.string.section_battery_style)
        "navbar" -> stringResource(R.string.section_navbar)
        "lockscreen_clock_font" -> stringResource(R.string.section_lockscreen_clock_font)
        "font" -> stringResource(R.string.section_system_font)
        "wifi_icons" -> stringResource(R.string.section_wifi_icons)
        "signal_icons" -> stringResource(R.string.section_signal_icons)
        "icon_packs" -> stringResource(R.string.section_icon_packs)
        else -> c.replace('_', ' ')
            .split(' ')
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }
}

@Composable
private fun MainStoreThemeStrip(
    themes: List<Theme>,
    themeStates: Map<String, ThemeInstallState>,
    onThemeClick: (Theme) -> Unit,
) {
    if (themes.size <= MAIN_STORE_THEME_STRIP_STATIC_ROW_MAX) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            themes.forEach { theme ->
                ThemeHorizontalCard(
                    theme = theme,
                    isActive = themeStates[theme.id] is ThemeInstallState.Installed,
                    onClick = { onThemeClick(theme) },
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                themes,
                key = { it.id },
                contentType = { "theme_horizontal_card" },
            ) { theme ->
                ThemeHorizontalCard(
                    theme = theme,
                    isActive = themeStates[theme.id] is ThemeInstallState.Installed,
                    onClick = { onThemeClick(theme) },
                )
            }
        }
    }
}

@Composable
private fun ThemeHorizontalCard(
    theme: Theme,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val outerCardColor = scheme.surfaceContainerLow
    val inactiveInnerPreviewColor =
        lerp(scheme.surfaceContainerLow, scheme.surfaceContainerHighest, 0.45f)
    val activeInnerPreviewGradient = remember(
        scheme.primaryContainer,
        scheme.surfaceContainerHigh,
        scheme.tertiaryContainer,
        scheme.primary,
        scheme.secondaryContainer,
        scheme.tertiary,
    ) {
        Brush.linearGradient(
            colors = listOf(
                lerp(scheme.primaryContainer, scheme.surfaceContainerHigh, 0.15f),
                lerp(scheme.tertiaryContainer, scheme.primary.copy(alpha = 0.35f), 0.5f),
                lerp(scheme.secondaryContainer, scheme.tertiary.copy(alpha = 0.28f), 0.35f),
            ),
            start = Offset(0f, 0f),
            end = Offset(200f, 180f),
        )
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = 132.dp, max = 168.dp)
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                scheme.primary.copy(alpha = 0.55f),
                                scheme.tertiary.copy(alpha = 0.45f),
                            ),
                        ),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            ),
        color = Color.Transparent,
        shape = shape,
        shadowElevation = if (isActive) 3.dp else 0.dp,
        tonalElevation = if (isActive) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier
                .background(outerCardColor, shape)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (isActive) {
                            Modifier.background(activeInnerPreviewGradient)
                        } else {
                            Modifier.background(inactiveInnerPreviewColor)
                        },
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                ThemeStoreItemPreview(
                    theme = theme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PreviewDimensions.MainListHorizontalPreviewHeight),
                    compact = true,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mainListDisplayName(theme),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ThemeRow(
    theme: Theme,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val outerRowColor = scheme.surfaceContainerLow
    val inactiveInnerPreviewColor =
        lerp(scheme.surfaceContainerLow, scheme.surfaceContainerHighest, 0.4f)
    val activeInnerPreviewGradient = remember(
        scheme.primaryContainer,
        scheme.surfaceContainerHigh,
        scheme.tertiaryContainer,
        scheme.primary,
    ) {
        Brush.linearGradient(
            colors = listOf(
                lerp(scheme.primaryContainer, scheme.surfaceContainerHigh, 0.12f),
                lerp(scheme.tertiaryContainer, scheme.primary.copy(alpha = 0.3f), 0.45f),
            ),
            start = Offset(0f, 40f),
            end = Offset(380f, 40f),
        )
    }
    val chevronColor =
        if (isActive) lerp(scheme.primary, scheme.tertiary, 0.25f) else scheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                scheme.primary.copy(alpha = 0.5f),
                                scheme.tertiary.copy(alpha = 0.4f),
                            ),
                        ),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            ),
        color = Color.Transparent,
        shape = shape,
        shadowElevation = if (isActive) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .background(outerRowColor, shape)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(PreviewDimensions.MainListCategoryPreviewSize)
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (isActive) {
                            Modifier.background(activeInnerPreviewGradient)
                        } else {
                            Modifier.background(inactiveInnerPreviewColor)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ThemeStoreItemPreview(
                    theme = theme,
                    modifier = Modifier.fillMaxSize(),
                    compact = true,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mainListDisplayName(theme),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = chevronColor,
            )
        }
    }
}

private fun mainListDisplayName(theme: Theme): String {
    if (theme.category == "back_gesture" && theme.name.contains("Dot trail", ignoreCase = true)) {
        return "Dot trail"
    }
    if (theme.category == "charging_animation") {
        val pkg = theme.overlays.firstOrNull()?.packageName.orEmpty()
        val style = pkg.substringAfterLast('.')
        if (style == "moto") return "Moto"
        if (style == "nothing") return "Nothing"
        if (style == "supervooc") return "SuperVOOC"
    }
    return theme.name
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = Modifier.fillMaxSize().then(modifier), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = Modifier.fillMaxSize().then(modifier), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun EmptyState(
    isSearching: Boolean,
    firstSectionMatchLabel: String? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = when {
                !isSearching -> stringResource(R.string.no_themes_available)
                firstSectionMatchLabel != null -> firstSectionMatchLabel
                else -> stringResource(R.string.no_results_found)
            },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private val MONET_SEARCH_TOKENS = listOf(
    "monet", "material you", "dynamic color", "theme style", "color source",
    "accent", "chroma", "luminance", "fidelity", "wallpaper color", "tonal",
    "vibrant", "expressive", "spritz", "rainbow", "fruit salad", "monochromatic",
)

private val UI_STYLE_SEARCH_TOKENS = listOf(
    "ui style", "ui styles", "uistyle", "uistyles",
    "outline", "neon", "bevel", "gradient", "reflective", "slash", "aerogel", "metallic", "style",
)

private data class FirstSectionSearchMatch(
    val monetToken: String? = null,
    val uiStyleToken: String? = null,
) {
    val hasAnyMatch: Boolean
        get() = monetToken != null || uiStyleToken != null

    val label: String?
        get() = when {
            monetToken != null -> "Monet - $monetToken"
            uiStyleToken != null -> "UI Styles - $uiStyleToken"
            else -> null
        }
}

private fun firstSectionSearchMatch(query: String): FirstSectionSearchMatch {
    if (query.isBlank()) return FirstSectionSearchMatch()
    val q = query.lowercase()
    val monet = MONET_SEARCH_TOKENS.firstOrNull { token -> token.contains(q) || q.contains(token) }
    val uiStyles =
        UI_STYLE_SEARCH_TOKENS.firstOrNull { token -> token.contains(q) || q.contains(token) }
    return FirstSectionSearchMatch(monetToken = monet, uiStyleToken = uiStyles)
}

@Composable
private fun ThemesEntrySection(
    onMonetClick: () -> Unit,
    onUiStylesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val cardShape = MaterialTheme.shapes.medium
    val previewShape = MaterialTheme.shapes.small
    val previewBg = lerp(scheme.surfaceContainerLow, scheme.surfaceContainerHighest, 0.45f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.section_themes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(PreviewDimensions.ThemeStoreSectionLabelToCardsSpacing))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                onClick = onMonetClick,
                modifier = Modifier.weight(1f),
                color = Color.Transparent,
                shape = cardShape,
            ) {
                Column(
                    modifier = Modifier
                        .background(scheme.surfaceContainerLow, cardShape)
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PreviewDimensions.MainListHorizontalPreviewHeight)
                            .clip(previewShape)
                            .background(previewBg)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MonetSwatchCompact()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.monet_card_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Surface(
                onClick = onUiStylesClick,
                modifier = Modifier.weight(1f),
                color = Color.Transparent,
                shape = cardShape,
            ) {
                Column(
                    modifier = Modifier
                        .background(scheme.surfaceContainerLow, cardShape)
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PreviewDimensions.MainListHorizontalPreviewHeight)
                            .clip(previewShape)
                            .background(previewBg)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        UiStyleCardPreview()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.ui_styles_card_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun UiStyleCardPreview(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(scheme.primary),
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(scheme.tertiary),
            )
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(scheme.outlineVariant),
        )
    }
}
