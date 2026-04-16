/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.alpha.settings.ui.ui.components.PreviewDimensions
import com.alpha.settings.ui.ui.components.QsTileUiStylePreview
import com.alpha.settings.ui.ui.components.UiStyleIds
import com.alpha.settings.ui.ui.components.uiStyleTitle
import com.alpha.settings.ui.viewmodel.ThemeStoreViewModel

@Composable
fun UiStylePreviewSection(
    viewModel: ThemeStoreViewModel,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    filterIds: List<String>? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val systemStyleId by viewModel.systemUiStyleId.collectAsStateWithLifecycle()
    val displayIds = remember(filterIds) { filterIds ?: UiStyleIds.ALL }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ui_style_preview_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = lerp(scheme.onSurface, scheme.primary, 0.28f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Spacer(modifier = Modifier.height(PreviewDimensions.ThemeStoreSectionLabelToCardsSpacing))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(displayIds, key = { it }) { id ->
                UiStyleHorizontalCard(
                    styleId = id,
                    label = uiStyleTitle(id),
                    isActive = id == systemStyleId,
                    onClick = {
                        viewModel.applyUiStyle(id)
                        onOpenDetail(id)
                    },
                )
            }
        }
    }
}

@Composable
private fun UiStyleHorizontalCard(
    styleId: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val previewShape = MaterialTheme.shapes.small

    val cardColor = if (isActive) {
        scheme.surfaceContainer
    } else {
        scheme.surfaceContainerLow
    }

    val previewChrome = remember(
        scheme.primaryContainer,
        scheme.surfaceContainerHigh,
        scheme.tertiaryContainer,
        scheme.surfaceContainerHighest,
        isActive,
    ) {
        if (isActive) {
            Brush.linearGradient(
                colors = listOf(
                    lerp(scheme.primaryContainer, scheme.surfaceContainerHigh, 0.24f),
                    lerp(scheme.tertiaryContainer, scheme.surfaceContainerHighest, 0.40f),
                ),
                start = Offset(0f, 0f),
                end = Offset(220f, 120f),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    scheme.surfaceContainerHigh,
                    scheme.surfaceContainerHighest,
                ),
                start = Offset(0f, 0f),
                end = Offset(220f, 120f),
            )
        }
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
                                scheme.tertiary.copy(alpha = 0.42f),
                            ),
                        ),
                        shape = shape,
                    )
                } else {
                    Modifier
                }
            ),
        color = Color.Transparent,
        shape = shape,
        shadowElevation = if (isActive) 3.dp else 0.dp,
        tonalElevation = if (isActive) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier
                .background(cardColor, shape)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(previewShape)
                    .background(previewChrome)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                QsTileUiStylePreview(
                    styleId = styleId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PreviewDimensions.UiStyleTileSectionHeight),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}