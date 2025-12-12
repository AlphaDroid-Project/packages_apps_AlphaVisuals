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

package com.android.axion.axthemestore.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.axion.axthemestore.data.model.Theme
import com.android.axion.axthemestore.data.model.ThemeInstallState
import com.android.axion.axthemestore.data.model.ThemeOverlay
import com.android.axion.axthemestore.data.model.formatFileSize
import com.android.axion.axthemestore.data.model.hasUpdate
import com.android.axion.axthemestore.ui.components.AsyncNetworkImage
import com.android.axion.axthemestore.ui.components.ImagePlaceholder
import com.android.axion.axthemestore.ui.components.ThemePackagePreview
import com.android.axion.axthemestore.viewmodel.ThemeStoreViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThemeDetailScreen(
    theme: Theme,
    viewModel: ThemeStoreViewModel,
    onBackClick: () -> Unit
) {
    val themeStates by viewModel.themeStates.collectAsState()
    val installState = themeStates[theme.id] ?: ThemeInstallState.NotInstalled
    val pendingChanges by viewModel.pendingComponentChanges.collectAsState()
    val hasPendingChanges = pendingChanges.containsKey(theme.id)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(theme.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    InstallSection(
                        theme = theme,
                        installState = installState,
                        hasPendingChanges = hasPendingChanges,
                        onDownloadClick = { viewModel.downloadTheme(theme) },
                        onInstallClick = { viewModel.installTheme(theme) },
                        onApplyClick = { viewModel.applyTheme(theme) },
                        onApplyPendingClick = { viewModel.applyPendingChanges(theme) },
                        onDisableClick = { viewModel.disableTheme(theme) },
                        onUninstallClick = { 
                            viewModel.uninstallTheme(theme, onComplete = onBackClick)
                        },
                        onClearError = { viewModel.clearError(theme.id) }
                    )
                }
            }
        }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            val packageName = theme.overlays.firstOrNull()?.packageName
            val hasRepoImages = theme.previewImages.isNotEmpty()
            val isInstalled = installState is ThemeInstallState.Installed || 
                              installState is ThemeInstallState.InstalledInactive
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                when {
                    hasRepoImages -> {
                        val totalPages = if (isInstalled && packageName != null) {
                            theme.previewImages.size + 1
                        } else {
                            theme.previewImages.size
                        }
                        
                        val pagerState = rememberPagerState(pageCount = { totalPages })
                        
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            if (page < theme.previewImages.size) {
                                AsyncNetworkImage(
                                    url = theme.previewImages[page],
                                    contentDescription = "Preview ${page + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    errorContent = {
                                        if (isInstalled && packageName != null) {
                                            ThemePackagePreview(
                                                packageName = packageName,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            ImagePlaceholder(
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                )
                            } else {
                                ThemePackagePreview(
                                    packageName = packageName!!,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        
                        if (totalPages > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(totalPages) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (pagerState.currentPage == index)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                    isInstalled && packageName != null -> {
                        ThemePackagePreview(
                            packageName = packageName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        ImagePlaceholder(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "by ${theme.author}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (theme.isUnified && theme.overlays.isNotEmpty()) {
                    val overlay = theme.overlays.first()
                    val packageName = overlay.packageName
                    val isThisThemeActive = installState is ThemeInstallState.Installed
                    val isInstalled = installState is ThemeInstallState.Installed || 
                                      installState is ThemeInstallState.InstalledInactive
                    
                    val enabledComponents by viewModel.enabledComponents.collectAsState()
                    val categoryThemes by viewModel.categoryThemesState.collectAsState()
                    val pendingChanges by viewModel.pendingComponentChanges.collectAsState()
                    val hasPending = pendingChanges.containsKey(theme.id)
                    
                    if (overlay.targets.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Components",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            if (hasPending) {
                                Text(
                                    text = "Pending changes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        overlay.targets.forEach { target ->
                            val isFromThisTheme = categoryThemes[target] == packageName
                            val currentThemePkg = categoryThemes[target]
                            
                            val pendingSet = pendingChanges[theme.id]
                            val isEnabled = when {
                                pendingSet != null -> pendingSet.contains(target)
                                isFromThisTheme -> true
                                else -> false
                            }
                            
                            ComponentSelectionItem(
                                componentId = target,
                                isEnabled = isEnabled,
                                isToggleable = isInstalled,
                                currentSource = if (!isFromThisTheme && currentThemePkg != null) {
                                    currentThemePkg.substringAfterLast('.')
                                } else null,
                                onToggle = { enabled ->
                                    if (isInstalled) {
                                        viewModel.toggleComponent(theme, target, enabled)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                } else if (theme.overlays.isNotEmpty() && !theme.isUiStyle) {
                    Text(
                        text = "Components",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    theme.overlays.forEach { overlay ->
                        ComponentOverlayItem(overlay = overlay)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                if (theme.tags.isNotEmpty()) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(theme.tags.size) { index ->
                            TagChip(tag = theme.tags[index])
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
            }
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TagChip(tag: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ComponentOverlayItem(overlay: ThemeOverlay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = overlay.componentId.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = overlay.targetPackage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = overlay.fileSize.formatFileSize(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UnifiedComponentItem(
    componentId: String,
    isEnabled: Boolean,
    isToggleable: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getComponentDisplayName(componentId),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = getComponentDescription(componentId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isToggleable) {
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getComponentDisplayName(componentId: String): String {
    return when (componentId) {
        "statusbar_wifi", "wifi" -> "WiFi Icons"
        "statusbar_signal", "signal" -> "Signal Icons"
        "android" -> "Android Framework"
        "systemui", "systemui_icons" -> "System UI"
        "ui_qs" -> "QuickSettings Style"
        "ui_volume" -> "Volume Panel Style"
        "ui_style" -> "UI Style"
        "icon_shape" -> "Icon Shape"
        else -> componentId.replaceFirstChar { it.uppercase() }
    }
}

private fun getComponentDescription(componentId: String): String {
    return when (componentId) {
        "statusbar_wifi", "wifi" -> "WiFi signal indicators in status bar"
        "statusbar_signal", "signal" -> "Mobile network indicators in status bar"
        "android" -> "Core Android framework icons"
        "systemui", "systemui_icons" -> "Status bar and quick settings icons"
        "ui_qs" -> "QuickSettings tiles and brightness slider style"
        "ui_volume" -> "Volume panel appearance"
        "ui_style" -> "Overall UI appearance"
        "icon_shape" -> "System-wide icon shape"
        else -> "Theme component"
    }
}

@Composable
private fun ComponentSelectionItem(
    componentId: String,
    isEnabled: Boolean,
    isToggleable: Boolean,
    currentSource: String? = null,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getComponentDisplayName(componentId),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (currentSource != null) {
                Text(
                    text = "Currently from: $currentSource",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                Text(
                    text = getComponentDescription(componentId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isToggleable) {
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        } else {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InstallSection(
    theme: Theme,
    installState: ThemeInstallState,
    hasPendingChanges: Boolean,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
    onApplyClick: () -> Unit,
    onApplyPendingClick: () -> Unit,
    onDisableClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (theme.isUiStyle) {
            when (installState) {
                is ThemeInstallState.Installed -> {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active")
                    }
                }
                is ThemeInstallState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Error: ${installState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onClearError()
                                onApplyClick()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onApplyClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply")
                    }
                }
            }
            return
        }
        
        when (installState) {
            is ThemeInstallState.NotInstalled -> {
                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download")
                }
            }

            is ThemeInstallState.Downloaded -> {
                Button(
                    onClick = onInstallClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install")
                }
            }
            
            is ThemeInstallState.InstalledInactive -> {
                val hasUpdate = theme.hasUpdate(installState.installedVersionCode)
                
                val hasSelection = if (theme.isUnified) {
                    hasPendingChanges
                } else {
                    true 
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (hasUpdate) {
                        Button(
                            onClick = onInstallClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update to v${theme.version}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onApplyClick,
                            modifier = Modifier.weight(1f),
                            enabled = hasSelection
                        ) {
                            Text("Apply")
                        }
                        
                        OutlinedButton(
                            onClick = onUninstallClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Uninstall")
                        }
                    }
                }
            }
            
            is ThemeInstallState.Installed -> {
                val hasUpdate = theme.hasUpdate(installState.installedVersionCode)
                
                if (hasUpdate) {
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update to v${theme.version}")
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasPendingChanges) {
                            Button(
                                onClick = onApplyPendingClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Apply Changes")
                            }
                        } else {
                            Button(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Active")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDisableClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Disable")
                            }
                            
                            OutlinedButton(
                                onClick = onUninstallClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Uninstall")
                            }
                        }
                    }
                }
            }
            
            is ThemeInstallState.Downloading -> {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloading ${installState.currentOverlay}...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${(installState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val animatedProgress by animateFloatAsState(
                        targetValue = installState.progress,
                        label = "download_progress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            is ThemeInstallState.Installing -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Installing...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is ThemeInstallState.PartiallyInstalled -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${installState.installedOverlays.size} of ${installState.totalOverlays} components installed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume Installation")
                    }
                }
            }
            
            is ThemeInstallState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: ${installState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onClearError()
                            onInstallClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
