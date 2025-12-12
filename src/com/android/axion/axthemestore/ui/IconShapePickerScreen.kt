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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.axion.axthemestore.ui.components.loadSystemDefaultIcons
import com.android.axion.axthemestore.viewmodel.ThemeStoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconShapePickerScreen(
    viewModel: ThemeStoreViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Icon Shapes",
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
            items(uiState.iconShapes, key = { it.id }) { shape ->
                val isSelected = shape.id == uiState.currentIconShape
                val shapePath = remember(shape.pathData) {
                    try {
                        PathParser.createPathFromPathData(shape.pathData).asComposePath()
                    } catch (e: Exception) {
                        null
                    }
                }
                
                IconShapeDetailCard(
                    label = shape.label,
                    shapePath = shapePath,
                    isSelected = isSelected,
                    onApplyClick = { viewModel.applyIconShape(shape.id) }
                )
            }
        }
    }
}

@Composable
private fun IconShapeDetailCard(
    label: String,
    shapePath: Path?,
    isSelected: Boolean,
    onApplyClick: () -> Unit
) {
    val context = LocalContext.current
    var appIcons by remember { mutableStateOf<List<Drawable>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        appIcons = loadSystemDefaultIcons(context)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
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
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (shapePath != null && appIcons.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            appIcons.take(3).forEach { icon ->
                                ShapeMaskedAppIcon(
                                    appIcon = icon,
                                    shapePath = shapePath,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                        
                        if (appIcons.size >= 6) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                appIcons.drop(3).take(3).forEach { icon ->
                                    ShapeMaskedAppIcon(
                                        appIcon = icon,
                                        shapePath = shapePath,
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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
                
                Text(
                    text = "Applies to all app icons on your device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
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
                        Text("Active")
                    }
                } else {
                    Button(
                        onClick = onApplyClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun ShapePreviewIcon(
    shapePath: Path,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 100f
        val scaleY = size.height / 100f
        
        scale(scaleX, scaleY, pivot = Offset.Zero) {
            drawPath(
                path = shapePath,
                color = color,
                style = Fill
            )
        }
    }
}

@Composable
private fun ShapeMaskedAppIcon(
    appIcon: Drawable,
    shapePath: Path,
    modifier: Modifier = Modifier
) {
    val iconBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = appIcon,
        key2 = System.identityHashCode(appIcon)
    ) {
        value = withContext(Dispatchers.Default) {
            val size = 400
            val bitmap = Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)

            if (appIcon is AdaptiveIconDrawable) {
                val layerSize = (size * 1.5f).toInt()
                val offset = (size - layerSize) / 2
                
                appIcon.background?.let { bg ->
                    bg.setBounds(offset, offset, offset + layerSize, offset + layerSize)
                    bg.draw(canvas)
                }
                
                appIcon.foreground?.let { fg ->
                    fg.setBounds(offset, offset, offset + layerSize, offset + layerSize)
                    fg.draw(canvas)
                }
            } else {
                appIcon.setBounds(0, 0, size, size)
                appIcon.draw(canvas)
            }
            
            bitmap.asImageBitmap()
        }
    }
    
    if (iconBitmap != null) {
        Canvas(modifier = modifier) {
            val pathBounds = shapePath.getBounds()
            
            if (pathBounds.width > 0 && pathBounds.height > 0) {
                val scaleX = size.width / pathBounds.width
                val scaleY = size.height / pathBounds.height
                
                val translateX = -pathBounds.left * scaleX
                val translateY = -pathBounds.top * scaleY
                
                translate(translateX, translateY) {
                    scale(scaleX, scaleY, pivot = Offset.Zero) {
                        clipPath(shapePath) {
                            drawImage(
                                image = iconBitmap!!,
                                dstOffset = IntOffset(pathBounds.left.toInt(), pathBounds.top.toInt()),
                                dstSize = IntSize(
                                    pathBounds.width.toInt().coerceAtLeast(1),
                                    pathBounds.height.toInt().coerceAtLeast(1)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
