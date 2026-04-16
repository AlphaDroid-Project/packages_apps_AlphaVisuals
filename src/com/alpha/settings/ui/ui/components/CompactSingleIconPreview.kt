/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Square preview area: `aspectRatio(1f)` ensures the slot is square within the parent's constraints.
 * Inner drawable sizes use [PreviewDimensions.MainListIconSize].
 */
@Composable
fun MainListGlyphSlot(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.aspectRatio(1f, matchHeightConstraintsFirst = true),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Outer area for compact previews: centers content; no background (no gradient plate). */
@Composable
fun CompactPreviewIconArea(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
