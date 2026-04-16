/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpha.settings.ui.R
import com.alpha.settings.ui.systemui.SystemUiLivePreview
import com.alpha.settings.ui.ui.components.UiStyleIds
import com.alpha.settings.ui.ui.components.uiStyleTitle
import com.alpha.settings.ui.ui.model.UiStyleMode
import com.alpha.settings.ui.ui.model.UiStyleTuning
import com.alpha.settings.ui.viewmodel.ThemeStoreViewModel
import kotlin.math.roundToInt

private const val UI_STYLE_AUTHOR_FALLBACK = "AlphaDroid"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiStylePreviewDetailScreen(
    viewModel: ThemeStoreViewModel,
    styleId: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editorState by viewModel.uiStyleEditorState.collectAsStateWithLifecycle()
    val systemStyleId by viewModel.systemUiStyleId.collectAsStateWithLifecycle()

    var selectedId by remember { mutableStateOf(UiStyleIds.normalize(styleId)) }
    val authorLine = uiState.uiStyleAuthors[selectedId] ?: UI_STYLE_AUTHOR_FALLBACK

    val configuration = LocalConfiguration.current
    val systemUiMode = if (
        (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    ) {
        UiStyleMode.DARK
    } else {
        UiStyleMode.LIGHT
    }
    val selectedTuning = editorState.tuningFor(systemUiMode)

    LaunchedEffect(selectedId) {
        viewModel.applyUiStyle(selectedId)
        viewModel.loadUiStyleEditor(selectedId)
    }

    LaunchedEffect(systemStyleId) {
        if (selectedId != systemStyleId) {
            selectedId = systemStyleId
        }
    }

    var showStyleDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ui_styles_card_label),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.monet_engine_style_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiStyleTitle(selectedId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    androidx.compose.foundation.layout.Box {
                        OutlinedCard(
                            onClick = { showStyleDropdown = true },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = uiStyleTitle(selectedId),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showStyleDropdown,
                            onDismissRequest = { showStyleDropdown = false },
                        ) {
                            UiStyleIds.ALL.forEach { id ->
                                DropdownMenuItem(
                                    text = { Text(uiStyleTitle(id)) },
                                    leadingIcon = if (id == selectedId) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        showStyleDropdown = false
                                        selectedId = id
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiStyleTitle(selectedId),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Button(
                        onClick = { SystemUiLivePreview.pulseVolumeDialog(context) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp,
                        ),
                    ) {
                        Text(text = "Preview", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text(
                    text = stringResource(R.string.by_author, authorLine),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (selectedId != UiStyleIds.SYSTEM_DEFAULT) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                ) {
                    Text(
                        text = stringResource(R.string.ui_style_live_preview_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (selectedId == UiStyleIds.SYSTEM_DEFAULT) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(
                        text = stringResource(R.string.ui_style_default_no_tuning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                return@Column
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.ui_style_mode_selector_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = systemUiMode == UiStyleMode.LIGHT,
                        onClick = { },
                        label = { Text(stringResource(R.string.ui_style_mode_light)) },
                    )
                    FilterChip(
                        selected = systemUiMode == UiStyleMode.DARK,
                        onClick = { },
                        label = { Text(stringResource(R.string.ui_style_mode_dark)) },
                    )
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.ui_style_tuning_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))

                UiStyleSliderRow(
                    label = stringResource(R.string.ui_style_saturation),
                    value = selectedTuning.saturation,
                    range = 0..200,
                    onReset = {
                        viewModel.updateUiStyleTuning {
                            it.copy(saturation = UiStyleTuning.Default.saturation)
                        }
                    },
                    onValueChange = { value ->
                        viewModel.updateUiStyleTuning {
                            it.copy(saturation = value)
                        }
                    },
                )

                UiStyleSliderRow(
                    label = stringResource(R.string.ui_style_lightness),
                    value = selectedTuning.lightness,
                    range = 0..200,
                    onReset = {
                        viewModel.updateUiStyleTuning {
                            it.copy(lightness = UiStyleTuning.Default.lightness)
                        }
                    },
                    onValueChange = { value ->
                        viewModel.updateUiStyleTuning {
                            it.copy(lightness = value)
                        }
                    },
                )

                UiStyleSliderRow(
                    label = stringResource(R.string.ui_style_opacity),
                    value = selectedTuning.opacity,
                    range = 0..100,
                    onReset = {
                        viewModel.updateUiStyleTuning {
                            it.copy(opacity = UiStyleTuning.Default.opacity)
                        }
                    },
                    onValueChange = { value ->
                        viewModel.updateUiStyleTuning {
                            it.copy(opacity = value)
                        }
                    },
                )

                UiStyleSliderRow(
                    label = stringResource(R.string.ui_style_strength),
                    value = selectedTuning.strength,
                    range = 0..200,
                    onReset = {
                        viewModel.updateUiStyleTuning {
                            it.copy(strength = UiStyleTuning.Default.strength)
                        }
                    },
                    onValueChange = { value ->
                        viewModel.updateUiStyleTuning {
                            it.copy(strength = value)
                        }
                    },
                )

                UiStyleSliderRow(
                    label = stringResource(R.string.ui_style_angle),
                    value = selectedTuning.angle,
                    range = 0..360,
                    onReset = {
                        viewModel.updateUiStyleTuning {
                            it.copy(angle = UiStyleTuning.Default.angle)
                        }
                    },
                    onValueChange = { value ->
                        viewModel.updateUiStyleTuning {
                            it.copy(angle = value)
                        }
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetUiStyleTuningForCurrentMode() },
                    modifier = Modifier.weight(1f),
                ) {
                    val modeLabel = if (systemUiMode == UiStyleMode.DARK) {
                        stringResource(R.string.ui_style_mode_dark)
                    } else {
                        stringResource(R.string.ui_style_mode_light)
                    }
                    Text(text = stringResource(R.string.ui_style_reset_mode, modeLabel))
                }

                OutlinedButton(
                    onClick = { viewModel.resetUiStyle(selectedId) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.ui_style_reset_style))
                }
            }
        }
    }
}

@Composable
private fun UiStyleSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    onReset: () -> Unit,
    onValueChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = "Reset",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                onValueChange(raw.roundToInt().coerceIn(range.first, range.last))
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}
