/*
 * Copyright (C) 2025 AxionOS Project
 * Copyright (C) 2026 AlphaDroid
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpha.settings.ui.R
import com.alpha.settings.ui.data.model.Theme
import com.alpha.settings.ui.data.model.ThemeInstallState
import com.alpha.settings.ui.data.model.targetShortLabel
import com.alpha.settings.ui.ui.components.FontOverlayPreviewKind
import com.alpha.settings.ui.ui.components.IconSetDetailPreview
import com.alpha.settings.ui.ui.components.IconPackDetailTripletPreview
import com.alpha.settings.ui.ui.components.LockClockFontPreview
import com.alpha.settings.ui.ui.components.NavbarPreview
import com.alpha.settings.ui.ui.components.PreviewDimensions
import com.alpha.settings.ui.ui.components.SIGNAL_CELLULAR_DRAWABLES
import com.alpha.settings.ui.ui.components.ThemeStoreItemPreview
import com.alpha.settings.ui.ui.components.WIFI_SIGNAL_DRAWABLES
import com.alpha.settings.ui.viewmodel.ThemeComponentEnablement
import com.alpha.settings.ui.viewmodel.ThemeStoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailScreen(
    theme: Theme,
    viewModel: ThemeStoreViewModel,
    onBackClick: () -> Unit
) {
    val categoryThemes by viewModel.categoryThemesState.collectAsStateWithLifecycle()
    val themeStates by viewModel.themeStates.collectAsStateWithLifecycle()

    val enablement = remember(theme.id, categoryThemes) {
        viewModel.themeComponentEnablementStatus(theme)
    }

    val installState = themeStates[theme.id] ?: ThemeInstallState.NotInstalled
    val isThemeActive = installState is ThemeInstallState.Installed
    val previewCategory = PreviewDimensions.normalizeStoreCategoryForPreview(theme.category)
    val isChargingAnimation = previewCategory == "charging_animation"

    val toggleEnabled = installState is ThemeInstallState.Installed ||
        installState is ThemeInstallState.InstalledInactive
    val toggleLabel = when (installState) {
        is ThemeInstallState.Installed -> stringResource(R.string.disable)
        else -> stringResource(R.string.enable)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(theme.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            if (theme.overlays.isNotEmpty()) {
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Button(
                        onClick = {
                            when (installState) {
                                is ThemeInstallState.Installed ->
                                    viewModel.requestToggleTheme(theme, enable = false)
                                is ThemeInstallState.InstalledInactive ->
                                    viewModel.requestToggleTheme(theme, enable = true)
                                ThemeInstallState.NotInstalled -> Unit
                            }
                        },
                        enabled = toggleEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 56.dp),
                    ) {
                        Text(toggleLabel)
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (isChargingAnimation) {
                ThemeStoreItemPreview(
                    theme = theme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    compact = false,
                )
            } else {
                val scheme = MaterialTheme.colorScheme
                val inactiveInnerPreviewColor =
                    lerp(scheme.surfaceContainerLow, scheme.surfaceContainerHighest, 0.45f)
                val activeInnerPreviewGradient = remember(scheme) {
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PreviewDimensions.DetailStandardPreviewHeight)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .then(
                            if (isThemeActive) {
                                Modifier.background(activeInnerPreviewGradient)
                            } else {
                                Modifier.background(inactiveInnerPreviewColor)
                            },
                        )
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (previewCategory) {
                        "lockscreen_clock_font" -> LockClockFontPreview(
                            packageName = theme.overlays.firstOrNull()?.packageName.orEmpty(),
                            modifier = Modifier.fillMaxSize(),
                            compact = false,
                            embedDetailInParent = true,
                            detailTitleScale = PreviewDimensions.DetailCompactIconScale,
                            previewKind = FontOverlayPreviewKind.LockscreenClock,
                        )
                        "font" -> LockClockFontPreview(
                            packageName = theme.overlays.firstOrNull()?.packageName.orEmpty(),
                            modifier = Modifier.fillMaxSize(),
                            compact = false,
                            embedDetailInParent = true,
                            detailTitleScale = PreviewDimensions.DetailCompactIconScale,
                            previewKind = FontOverlayPreviewKind.SystemFont,
                        )
                        "navbar" -> NavbarPreview(
                            packageName = theme.overlays.firstOrNull()?.packageName.orEmpty(),
                            modifier = Modifier.fillMaxSize(),
                            compact = false,
                            embedDetailInParent = true,
                            detailIconScale = PreviewDimensions.DetailCompactIconScale,
                        )
                        "icon_packs" -> IconPackDetailTripletPreview(
                            packageName = theme.overlays.firstOrNull()?.packageName.orEmpty(),
                            modifier = Modifier.fillMaxSize(),
                            detailIconScale = PreviewDimensions.DetailCompactIconScale,
                        )
                        "wifi_icons" -> IconSetDetailPreview(
                            packageName = theme.overlays.firstOrNull()?.packageName.orEmpty(),
                            drawableNames = WIFI_SIGNAL_DRAWABLES,
                            modifier = Modifier.fillMaxSize(),
                            iconSize = 32.dp * PreviewDimensions.DetailCompactIconScale,
                        )
                        "signal_icons" -> IconSetDetailPreview(
                            packageName = theme.overlays.firstOrNull()?.packageName.orEmpty(),
                            drawableNames = SIGNAL_CELLULAR_DRAWABLES,
                            modifier = Modifier.fillMaxSize(),
                            iconSize = 32.dp * PreviewDimensions.DetailCompactIconScale,
                        )
                        else -> ThemeStoreItemPreview(
                            theme = theme,
                            modifier = Modifier.fillMaxSize(),
                            compact = true,
                            compactIconScale = PreviewDimensions.DetailCompactIconScale,
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                themeAuthorForDisplay(theme.author)?.let { authorLine ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.by_author, authorLine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (theme.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (theme.overlays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(R.string.components),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    theme.overlays.forEachIndexed { index, overlay ->
                        val isEnabled =
                            categoryThemes[overlay.componentId] == overlay.packageName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = overlay.packageName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 3
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.target_line,
                                        overlay.targetShortLabel()
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isEnabled) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(R.string.state_enabled),
                                    modifier = Modifier
                                        .size(22.dp)
                                        .padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = stringResource(R.string.state_disabled),
                                    modifier = Modifier
                                        .size(22.dp)
                                        .padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                        if (index < theme.overlays.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (enablement) {
                            ThemeComponentEnablement.ENABLED ->
                                stringResource(R.string.state_enabled)
                            ThemeComponentEnablement.DISABLED ->
                                stringResource(R.string.state_disabled)
                            ThemeComponentEnablement.PARTIAL ->
                                stringResource(R.string.state_partially_enabled)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = when (enablement) {
                            ThemeComponentEnablement.ENABLED ->
                                MaterialTheme.colorScheme.primary
                            ThemeComponentEnablement.PARTIAL ->
                                MaterialTheme.colorScheme.tertiary
                            ThemeComponentEnablement.DISABLED ->
                                MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/** Non-null only when [Theme.author] should be shown as the detail \"by …\" line. */
private fun themeAuthorForDisplay(author: String): String? {
    val t = author.trim()
    if (t.isEmpty() || t.equals("System", ignoreCase = true)) return null
    return t
}
