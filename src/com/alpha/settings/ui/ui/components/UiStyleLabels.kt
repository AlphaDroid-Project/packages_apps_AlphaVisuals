/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun uiStyleTitle(styleId: String): String {
    return when (UiStyleIds.normalize(styleId)) {
        "system_default" -> stringResource(com.android.internal.R.string.ui_style_system_default)
        "outline" -> stringResource(com.android.internal.R.string.ui_style_outline)
        "neon" -> stringResource(com.android.internal.R.string.ui_style_neon)
        "bevel" -> stringResource(com.android.internal.R.string.ui_style_bevel)
        "gradient" -> stringResource(com.android.internal.R.string.ui_style_gradient)
        "reflective" -> stringResource(com.android.internal.R.string.ui_style_reflective)
        "slash" -> stringResource(com.android.internal.R.string.ui_style_slash)
        "aerogel" -> stringResource(com.android.internal.R.string.ui_style_aerogel)
        "metallic" -> stringResource(com.android.internal.R.string.ui_style_metallic)
        else -> stringResource(com.android.internal.R.string.ui_style_system_default)
    }
}
