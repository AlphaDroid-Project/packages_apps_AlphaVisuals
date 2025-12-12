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

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.android.axion.axthemestore.data.model.Theme
import com.android.axion.axthemestore.viewmodel.ThemeStoreViewModel

@Composable
fun MainScreen(viewModel: ThemeStoreViewModel) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkInstallStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    DisposableEffect(context) {
        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                viewModel.loadThemes(forceRefresh = true)
                viewModel.loadIconPacks()
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        
        context.registerReceiver(packageReceiver, filter)
        onDispose {
            context.unregisterReceiver(packageReceiver)
        }
    }
    
    val bottomNavItems = listOf(
        BottomNavItem.Themes,
        BottomNavItem.IconPacks,
        BottomNavItem.Shapes
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            if (currentDestination?.route != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Themes.route,
            modifier = Modifier.padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding()
            ),
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(300)) }
        ) {
            composable(BottomNavItem.Themes.route) {
                ThemeStoreScreen(
                    viewModel = viewModel,
                    onThemeClick = { theme ->
                        navController.navigate("detail/${theme.id}")
                    },
                    onNavigateToCategory = { categoryId ->
                        navController.navigate("category/$categoryId")
                    },
                    onNavigateToInstalledComponents = {
                        navController.navigate("installed_components")
                    }
                )
            }
            
            composable("installed_components") {
                InstalledComponentsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "category/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                categoryId?.let {
                    CategoryThemesScreen(
                        categoryId = it,
                        viewModel = viewModel,
                        onThemeClick = { theme ->
                            navController.navigate("detail/${theme.id}")
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            
            composable(BottomNavItem.IconPacks.route) {
                IconPackListScreen(viewModel = viewModel)
            }
            
            composable(BottomNavItem.Shapes.route) {
                IconShapePickerScreen(viewModel = viewModel)
            }
            
            composable(
                route = "detail/{themeId}",
                arguments = listOf(navArgument("themeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val themeId = backStackEntry.arguments?.getString("themeId")
                val uiState = viewModel.uiState.value
                val theme = uiState.themes.find { it.id == themeId }
                
                theme?.let {
                    ThemeDetailScreen(
                        theme = it,
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Themes : BottomNavItem("themes", "Themes", Icons.Default.Palette)
    object IconPacks : BottomNavItem("iconposts", "App Icons", Icons.Default.Category)
    object Shapes : BottomNavItem("shapes", "Shapes", Icons.Default.Interests)
}
