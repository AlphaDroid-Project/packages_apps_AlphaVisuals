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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.axion.axthemestore.engine.ThemeEngineProxy
import com.android.axion.axthemestore.viewmodel.ThemeStoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledComponentsScreen(
    viewModel: ThemeStoreViewModel,
    onBackClick: () -> Unit
) {
    val categoryThemes by viewModel.categoryThemesState.collectAsState()
    val iconTheme = remember { mutableStateOf<String?>(null) }
    val iconShape = remember { mutableStateOf<String?>(null) }
    val uiStyle = remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        val proxy = ThemeEngineProxy(viewModel.getApplication())
        iconTheme.value = proxy.getIconTheme()
        iconShape.value = proxy.getIconShape()
        uiStyle.value = proxy.getUiStyle()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Installed Components",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Active Theme Components",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "View all components currently applied to your system",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            item {
                SectionHeader(title = "UI Style")
            }
            item {
                ComponentCard(
                    componentName = "UI Style",
                    packageOrId = uiStyle.value ?: "Default (Axion)",
                    icon = Icons.Default.Palette,
                    isBuiltIn = true
                )
            }
            
            if (!iconShape.value.isNullOrEmpty()) {
                item {
                    SectionHeader(title = "Icon Shape")
                }
                item {
                    ComponentCard(
                        componentName = "Icon Shape",
                        packageOrId = iconShape.value ?: "Default",
                        icon = Icons.Default.Interests,
                        isBuiltIn = true
                    )
                }
            }
            
            if (categoryThemes.isNotEmpty()) {
                item {
                    SectionHeader(title = "Icon Theme Components")
                }
                
                items(categoryThemes.entries.toList()) { (category, packageName) ->
                    ComponentCard(
                        componentName = getCategoryDisplayName(category),
                        packageOrId = packageName,
                        icon = getCategoryIcon(category),
                        isBuiltIn = false
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Icon Themes Installed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Browse the theme store to install icon themes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ComponentCard(
    componentName: String,
    packageOrId: String,
    icon: ImageVector,
    isBuiltIn: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = componentName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isBuiltIn) packageOrId else packageOrId.substringAfterLast('.'),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!isBuiltIn) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun getCategoryDisplayName(categoryId: String): String {
    return when (categoryId) {
        "android" -> "Android Framework"
        "systemui" -> "System UI"
        "wifi" -> "WiFi Icons"
        "signal" -> "Signal Icons"
        else -> categoryId.replaceFirstChar { it.uppercase() }
    }
}

private fun getCategoryIcon(categoryId: String): ImageVector {
    return when (categoryId) {
        "wifi" -> Icons.Default.Wifi
        "signal" -> Icons.Default.SignalCellularAlt
        "systemui" -> Icons.Default.SettingsApplications
        "android" -> Icons.Default.Android
        else -> Icons.Default.Category
    }
}
