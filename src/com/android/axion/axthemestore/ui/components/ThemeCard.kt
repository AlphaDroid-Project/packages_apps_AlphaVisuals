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

package com.android.axion.axthemestore.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.axion.axthemestore.data.model.Theme
import com.android.axion.axthemestore.data.model.ThemeInstallState
import com.android.axion.axthemestore.data.model.formatFileSize
import com.android.axion.axthemestore.data.model.hasUpdate

@Composable
fun ThemeCard(
    theme: Theme,
    installState: ThemeInstallState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            ) {
                if (theme.previewImages.isNotEmpty()) {
                    AsyncNetworkImage(
                        url = theme.previewImages.first(),
                        contentDescription = theme.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        errorContent = {
                            val isInstalled = installState is ThemeInstallState.Installed || 
                                              installState is ThemeInstallState.InstalledInactive
                            val packageName = theme.overlays.firstOrNull()?.packageName
                            
                            if (isInstalled && packageName != null) {
                                com.android.axion.axthemestore.ui.components.ThemePackagePreview(
                                    packageName = packageName,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                ImagePlaceholder(modifier = Modifier.fillMaxSize())
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }
                
                InstallStateBadge(
                    state = installState,
                    theme = theme,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }
            
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = theme.author,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    
                    if (theme.totalFileSize > 0) {
                        Text(
                            text = theme.totalFileSize.formatFileSize(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallStateBadge(
    state: ThemeInstallState,
    theme: Theme,
    modifier: Modifier = Modifier
) {
    val (icon, backgroundColor, contentColor) = when (state) {
        is ThemeInstallState.NotInstalled -> Triple(
            Icons.Default.Download,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        is ThemeInstallState.Installed -> {
            if (theme.hasUpdate(state.installedVersionCode)) {
                Triple(
                    Icons.Default.Update,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            } else {
                Triple(
                    Icons.Default.Check,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        is ThemeInstallState.InstalledInactive -> {
            if (theme.hasUpdate(state.installedVersionCode)) {
                Triple(
                    Icons.Default.Update,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            } else {
                Triple(
                    Icons.Default.Download,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        is ThemeInstallState.PartiallyInstalled -> Triple(
            Icons.Default.Download,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        is ThemeInstallState.Downloading -> Triple(
            null,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        is ThemeInstallState.Installing -> Triple(
            null,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        is ThemeInstallState.Error -> Triple(
            Icons.Default.Error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        is ThemeInstallState.Downloaded -> Triple(
            Icons.Default.Download,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
    
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is ThemeInstallState.Downloading -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progress,
                    label = "download_progress"
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = contentColor
                )
            }
            is ThemeInstallState.Installing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = contentColor
                )
            }
            else -> {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
