/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.data.model

/** Short target label for detail UI (e.g. android, systemui, signal_icon). */
fun ThemeOverlay.targetShortLabel(): String {
    val cid = componentId.removePrefix("android.theme.customization.")
    if (cid.startsWith("icon_pack.")) {
        return cid.removePrefix("icon_pack.").substringBefore('.')
    }
    return cid.substringBefore('.').ifBlank {
        targetPackage.substringAfterLast('.')
    }
}
