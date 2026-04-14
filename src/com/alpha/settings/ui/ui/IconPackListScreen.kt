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
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.alpha.settings.ui.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpha.settings.ui.R
import com.alpha.settings.ui.engine.ThemeEngineProxy
import com.alpha.settings.ui.viewmodel.ThemeStoreViewModel
import com.alpha.settings.ui.ui.components.AppIconPackPreview
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IconPackListScreen(
    viewModel: ThemeStoreViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadIconPacks()
        viewModel.loadThemedIconStyle()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.app_icon_packs),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ThemedIconStyleSection(
                    enabled = uiState.themedIconsEnabled,
                    currentStyle = uiState.themedIconStyle,
                    onEnabledChange = { viewModel.setThemedIconsEnabled(it) },
                    onStyleChange = { viewModel.setThemedIconStyle(it) }
                )
            }
            
            items(uiState.iconPacks) { pack ->
                val isSelected = pack.packageName == (uiState.currentIconPack ?: "")
                
                IconPackDetailCard(
                    packageName = pack.packageName,
                    label = pack.label,
                    isSelected = isSelected,
                    onApplyClick = { viewModel.applyIconPack(pack.packageName) }
                )
            }
        }
    }
}

@Composable
private fun ThemedIconStyleSection(
    enabled: Boolean,
    currentStyle: String,
    onEnabledChange: (Boolean) -> Unit,
    onStyleChange: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.themed_icons),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.themed_icons_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn(
                    animationSpec = MaterialTheme.motionScheme.slowEffectsSpec()
                ) + expandVertically(
                    animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
                ),
                exit = fadeOut(
                    animationSpec = MaterialTheme.motionScheme.slowEffectsSpec()
                ) + shrinkVertically(
                    animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
                )
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = stringResource(R.string.icon_style),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemedIconPreview(
                            isAxIcons = currentStyle == ThemeEngineProxy.Companion.ThemedIconStyle.AXION
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ButtonGroup(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isAxion = currentStyle == ThemeEngineProxy.Companion.ThemedIconStyle.AXION
                        val isAosp = currentStyle == ThemeEngineProxy.Companion.ThemedIconStyle.AOSP

                        ToggleButton(
                            checked = isAxion,
                            onCheckedChange = { if (it) onStyleChange(ThemeEngineProxy.Companion.ThemedIconStyle.AXION) },
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                        ) {
                            if (isAxion) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.axicons),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isAxion) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.neutral),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        ToggleButton(
                            checked = isAosp,
                            onCheckedChange = { if (it) onStyleChange(ThemeEngineProxy.Companion.ThemedIconStyle.AOSP) },
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                        ) {
                            if (isAosp) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.aosp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isAosp) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.accent),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemedIconPreview(
    isAxIcons: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PreviewIcon(isAxIcons, IconType.PHONE)
        PreviewIcon(isAxIcons, IconType.MESSAGES)
        PreviewIcon(isAxIcons, IconType.CAMERA)
        PreviewIcon(isAxIcons, IconType.SETTINGS)
    }
}

private enum class IconType {
    PHONE, MESSAGES, CAMERA, SETTINGS
}

@Composable
private fun PreviewIcon(isAxIcons: Boolean, type: IconType) {
    val bgColor = if (isAxIcons) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    
    val fgColor = if (isAxIcons) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    val iconSize = if (isAxIcons) 22.dp else 28.dp
    
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(MaterialTheme.shapes.large)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(iconSize)
        ) {
            val canvasSize = size.minDimension
            val strokeWidth = canvasSize * 0.12f
            
            when (type) {
                IconType.PHONE -> {
                    val path = Path().apply {
                        val w = canvasSize
                        val h = canvasSize
                        moveTo(w * 0.35f, h * 0.15f)
                        cubicTo(w * 0.25f, h * 0.15f, w * 0.2f, h * 0.2f, w * 0.2f, h * 0.3f)
                        lineTo(w * 0.2f, h * 0.7f)
                        cubicTo(w * 0.2f, h * 0.8f, w * 0.25f, h * 0.85f, w * 0.35f, h * 0.85f)
                        lineTo(w * 0.65f, h * 0.85f)
                        cubicTo(w * 0.75f, h * 0.85f, w * 0.8f, h * 0.8f, w * 0.8f, h * 0.7f)
                        lineTo(w * 0.8f, h * 0.3f)
                        cubicTo(w * 0.8f, h * 0.2f, w * 0.75f, h * 0.15f, w * 0.65f, h * 0.15f)
                        close()
                    }
                    drawPath(path, fgColor, style = Stroke(strokeWidth))
                    drawCircle(
                        fgColor,
                        radius = canvasSize * 0.05f,
                        center = Offset(canvasSize * 0.5f, canvasSize * 0.75f)
                    )
                }
                IconType.MESSAGES -> {
                    val path = Path().apply {
                        val w = canvasSize
                        val h = canvasSize
                        moveTo(w * 0.15f, h * 0.3f)
                        cubicTo(w * 0.15f, h * 0.2f, w * 0.2f, h * 0.15f, w * 0.3f, h * 0.15f)
                        lineTo(w * 0.7f, h * 0.15f)
                        cubicTo(w * 0.8f, h * 0.15f, w * 0.85f, h * 0.2f, w * 0.85f, h * 0.3f)
                        lineTo(w * 0.85f, h * 0.6f)
                        cubicTo(w * 0.85f, h * 0.7f, w * 0.8f, h * 0.75f, w * 0.7f, h * 0.75f)
                        lineTo(w * 0.55f, h * 0.75f)
                        lineTo(w * 0.45f, h * 0.85f)
                        lineTo(w * 0.45f, h * 0.75f)
                        lineTo(w * 0.3f, h * 0.75f)
                        cubicTo(w * 0.2f, h * 0.75f, w * 0.15f, h * 0.7f, w * 0.15f, h * 0.6f)
                        close()
                    }
                    drawPath(path, fgColor)
                }
                IconType.CAMERA -> {
                    drawRoundRect(
                        fgColor,
                        topLeft = Offset(canvasSize * 0.15f, canvasSize * 0.3f),
                        size = Size(canvasSize * 0.7f, canvasSize * 0.5f),
                        cornerRadius = CornerRadius(canvasSize * 0.08f),
                        style = Stroke(strokeWidth)
                    )
                    drawCircle(
                        fgColor,
                        radius = canvasSize * 0.15f,
                        center = Offset(canvasSize * 0.5f, canvasSize * 0.55f),
                        style = Stroke(strokeWidth)
                    )
                    drawRect(
                        fgColor,
                        topLeft = Offset(canvasSize * 0.35f, canvasSize * 0.2f),
                        size = Size(canvasSize * 0.3f, canvasSize * 0.1f)
                    )
                }
                IconType.SETTINGS -> {
                    val centerX = canvasSize * 0.5f
                    val centerY = canvasSize * 0.5f
                    val outerRadius = canvasSize * 0.35f
                    val innerRadius = canvasSize * 0.15f
                    
                    val path = Path()
                    for (i in 0 until 6) {
                        val angle = (i * 60f - 90f) * (Math.PI / 180f).toFloat()
                        val x = centerX + outerRadius * cos(angle)
                        val y = centerY + outerRadius * sin(angle)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        
                        val nextAngle = ((i + 1) * 60f - 90f) * (Math.PI / 180f).toFloat()
                        val midAngle = (angle + nextAngle) / 2f
                        val midX = centerX + innerRadius * cos(midAngle)
                        val midY = centerY + innerRadius * sin(midAngle)
                        path.lineTo(midX, midY)
                    }
                    path.close()
                    
                    drawPath(path, fgColor)
                    drawCircle(
                        bgColor,
                        radius = canvasSize * 0.12f,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }
    }
}

@Composable
private fun IconPackDetailCard(
    packageName: String,
    label: String,
    isSelected: Boolean,
    onApplyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AppIconPackPreview(
                    packageName = packageName,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (packageName.isNotEmpty()) {
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.default_system_icon_pack),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isSelected) {
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
                        Text(stringResource(R.string.active))
                    }
                } else {
                    Button(
                        onClick = onApplyClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }
            }
        }
    }
}
